/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.content.Collection;
import org.dspace.content.MetadataField;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Broker for metadata authority settings configured for each metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributor.author")
 *
 * {@code
 * # is field authority controlled (i.e. store authority, confidence values)?
 * authority.controlled.<FIELD> = true
 *
 * # is field required to have an authority value, or may it be empty?
 * # default is false.
 * authority.required.<FIELD> = true | false
 *
 * # default value of minimum confidence level for ALL fields - must be
 * # symbolic confidence level, see org.dspace.content.authority.Choices
 * authority.minconfidence = uncertain
 *
 * # minimum confidence level for this field
 * authority.minconfidence.SCHEMA.ELEMENT.QUALIFIER = SYMBOL
 * e.g.
 * authority.minconfidence.dc.contributor.author = accepted
 * }
 * NOTE: There is *expected* to be a "choices" (see ChoiceAuthorityManager)
 * configuration for each authority-controlled field.
 *
 * @author Larry Stone
 * @see ChoiceAuthorityServiceImpl
 * @see Choices
 */
public class MetadataAuthorityServiceImpl implements MetadataAuthorityService {
    private static final Logger log = LogManager.getLogger(MetadataAuthorityServiceImpl.class);

    private static final String AUTH_PREFIX = "authority.controlled";

    // the item submission reader
    private SubmissionConfigReader itemSubmissionConfigReader;

    @Autowired(required = true)
    protected UploadConfigurationService uploadConfigurationService;

    @Autowired(required = true)
    protected PluginService pluginService;

    @Autowired(required = true)
    protected MetadataFieldService metadataFieldService;

    @Autowired(required = true)
    protected AuthorityServiceUtils authorityServiceUtils;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    // map of field key that allow use of authorities
    // this comes from the dspace.cfg via the authority.controlled.* properties for
    // general controlled fields or from the submission forms for fields that are
    // controlled for specific collection (value-pairs or vocabularies)
    // for collection specific fields the map use as key the submission-name
    // for item's controlled metadata or the form-name for bitstream's controlled
    // metadata (i.e. tradition.dc.type is the key for the item dc.type of a collection
    // using the traditional submission and bitstream-metadata.dc.type is the key for
    // the metadata dc.type of bitstream included in the tradition submission that has
    // an upload step that refers to the bitstream-metadata form
    protected Map<String, Boolean> controlled = new HashMap<>();

    // map of field key to answer of whether field is required to be controlled
    protected Map<String, Boolean> isAuthorityRequired = null;

    /**
     * map of field key to answer of which is the min acceptable confidence
     * value for a field with authority
     */
    protected Map<String, Integer> minConfidence = new HashMap<>();

    /**
     * fallback default value unless authority.minconfidence = X is configured.
     */
    protected int defaultMinConfidence = Choices.CF_ACCEPTED;

    protected MetadataAuthorityServiceImpl() {

    }

    private synchronized void init() {

        if (isAuthorityRequired == null) {
            try {
                itemSubmissionConfigReader = new SubmissionConfigReader();
            } catch (SubmissionConfigReaderException e) {
                // the system is in an illegal state as the submission definition is not valid
                throw new IllegalStateException("Error reading the item submission configuration: " + e.getMessage(),
                        e);
            }
            isAuthorityRequired = new HashMap<>();
            List<String> keys = configurationService.getPropertyKeys(AUTH_PREFIX);
            Context context = new Context();
            try {
                for (String key : keys) {
                    // field is expected to be "schema.element.qualifier"
                    String field = key.substring(AUTH_PREFIX.length() + 1);
                    int dot = field.indexOf('.');
                    if (dot < 0) {
                        log.warn(
                            "Skipping invalid MetadataAuthority configuration property: {}:"
                                + " does not have schema.element.qualifier", key);
                        continue;
                    }
                    String schema = field.substring(0, dot);
                    String element = field.substring(dot + 1);
                    String qualifier = null;
                    dot = element.indexOf('.');
                    if (dot >= 0) {
                        qualifier = element.substring(dot + 1);
                        element = element.substring(0, dot);
                    }

                    MetadataField metadataField = metadataFieldService
                        .findByElement(context, schema, element, qualifier);
                    if (metadataField == null) {
                        throw new IllegalStateException(
                            "Error while configuring authority control, metadata field: " + field + " could not " +
                                "be found");
                    }
                    boolean ctl = configurationService.getBooleanProperty(key, true);
                    boolean req = configurationService.getBooleanProperty("authority.required." + field, false);
                    controlled.put(metadataField.toString(), ctl);
                    isAuthorityRequired.put(metadataField.toString(), req);

                    // get minConfidence level for this field if any
                    int mci = readConfidence("authority.minconfidence." + field);
                    if (mci >= Choices.CF_UNSET) {
                        minConfidence.put(metadataField.toString(), mci);
                    }
                    log.debug(
                        "Authority Control: For schema=" + schema + ", elt=" + element + ", qual=" + qualifier +
                            ", controlled=" + ctl + ", required=" + req);
                }
                autoRegisterControlledAuthorityFromInputReader();
            } catch (SQLException e) {
                log.error("Error reading authority config", e);
            }

            // get default min confidence if any:
            int dmc = readConfidence("authority.minconfidence");
            if (dmc >= Choices.CF_UNSET) {
                defaultMinConfidence = dmc;
            }
        }
    }

    /**
     * Add to the list of controlled metadata all the fields that are linked to a
     * vocabulary that actually store an authority
     */
    private void autoRegisterControlledAuthorityFromInputReader() {
        try {
            List<SubmissionConfig> submissionConfigs = itemSubmissionConfigReader
                    .getAllSubmissionConfigs(Integer.MAX_VALUE, 0);
            DCInputsReader dcInputsReader = new DCInputsReader();

            // loop over all the defined item submission configuration
            for (SubmissionConfig subCfg : submissionConfigs) {
                String submissionName = subCfg.getSubmissionName();
                List<DCInputSet> inputsBySubmissionName = dcInputsReader.getInputsBySubmissionName(submissionName);
                autoRegisterControlledAuthorityFromSubmissionForms(submissionName, inputsBySubmissionName);
            }
            // loop over all the defined bitstream metadata submission configuration
            for (UploadConfiguration uploadCfg : uploadConfigurationService.getMap().values()) {
                String formName = uploadCfg.getMetadata();
                DCInputSet inputByFormName = dcInputsReader.getInputsByFormName(formName);
                autoRegisterControlledAuthorityFromSubmissionForms(formName, List.of(inputByFormName));
            }
        } catch (DCInputsReaderException e) {
            // the system is in an illegal state as the submission definition is not valid
            throw new IllegalStateException("Error reading the item submission configuration: " + e.getMessage(),
                    e);
        }

    }

    private void autoRegisterControlledAuthorityFromSubmissionForms(String submissionName,
            List<DCInputSet> inputsBySubmissionName) {
        // loop over the submission forms configuration eventually associated with the
        // submission panel
        for (DCInputSet dcinputSet : inputsBySubmissionName) {
            DCInput[][] dcinputs = dcinputSet.getFields();
            for (DCInput[] dcrows : dcinputs) {
                for (DCInput dcinput : dcrows) {
                    // for each input in the form check if it is associated with a real value pairs
                    // or an xml vocabulary
                    String authorityName = null;
                    if (StringUtils.isNotBlank(dcinput.getPairsType())
                            && !StringUtils.equals(dcinput.getInputType(), "qualdrop_value")) {
                        authorityName = dcinput.getPairsType();
                    } else if (StringUtils.isNotBlank(dcinput.getVocabulary())) {
                        authorityName = dcinput.getVocabulary();
                    }

                    // do we have an authority?
                    if (StringUtils.isNotBlank(authorityName)) {
                        String fieldKey = makeFieldKey(dcinput.getSchema(), dcinput.getElement(),
                                dcinput.getQualifier());
                        ChoiceAuthority ca = (ChoiceAuthority) pluginService.getNamedPlugin(
                                ChoiceAuthority.class, authorityName);
                        if (ca == null) {
                            throw new IllegalStateException("Invalid configuration for " + fieldKey
                                    + " in submission definition " + submissionName + ", form definition "
                                    + dcinputSet.getFormName() + " no named plugin found: " + authorityName);
                        }
                        if (ca.storeAuthorityInMetadata()) {
                            controlled.put(submissionName + "." + fieldKey, true);
                        }
                    }
                }
            }
        }
    }

    private int readConfidence(String key) {
        String mc = configurationService.getProperty(key);
        if (mc != null) {
            int mci = Choices.getConfidenceValue(mc.trim(), Choices.CF_UNSET - 1);
            if (mci == Choices.CF_UNSET - 1) {
                log.warn(
                    "IGNORING bad value in DSpace Configuration, key=" + key + ", value=" + mc + ", must be a valid " +
                        "Authority Confidence keyword.");
            } else {
                return mci;
            }
        }
        return Choices.CF_UNSET - 1;
    }

    @Override
    public boolean isAuthorityAllowed(MetadataField metadataField, int dsoType, Collection collection) {
        init();
        return isAuthorityAllowed(makeFieldKey(metadataField), dsoType, collection);
    }

    @Override
    public boolean isAuthorityAllowed(String fieldKey, int dsoType, Collection collection) {
        init();
        if (controlled.containsKey(fieldKey) && controlled.get(fieldKey)) {
            return true;
        } else if (collection != null) {
            String subName = authorityServiceUtils.getSubmissionOrFormName(itemSubmissionConfigReader, dsoType,
                    collection);
            return controlled.containsKey(subName + "." + fieldKey) && controlled.get(subName + "." + fieldKey);
        }
        return false;
    }

    @Override
    public boolean isAuthorityRequired(MetadataField metadataField, int dsoType, Collection collection) {
        init();
        return isAuthorityRequired(makeFieldKey(metadataField), dsoType, collection);
    }

    @Override
    public boolean isAuthorityRequired(String fieldKey, int dsoType, Collection collection) {
        init();
        Boolean result = isAuthorityRequired.get(fieldKey);
        return (result != null) && result;
    }

    @Override
    public String makeFieldKey(MetadataField metadataField) {
        init();
        return metadataField.toString();
    }

    @Override
    public String makeFieldKey(String schema, String element, String qualifier) {
        init();
        if (qualifier == null) {
            return schema + "_" + element;
        } else {
            return schema + "_" + element + "_" + qualifier;
        }
    }

    /**
     * Give the minimal level of confidence required to consider valid an authority value
     * for the given metadata.
     *
     * @param metadataField metadata field
     * @return the minimal valid level of confidence for the given metadata
     */
    @Override
    public int getMinConfidence(MetadataField metadataField) {
        init();
        Integer result = minConfidence.get(makeFieldKey(metadataField));
        return result == null ? defaultMinConfidence : result;
    }

    @Override
    public void clearCache() {
        controlled.clear();
        minConfidence.clear();
        isAuthorityRequired = null;
    }
}
