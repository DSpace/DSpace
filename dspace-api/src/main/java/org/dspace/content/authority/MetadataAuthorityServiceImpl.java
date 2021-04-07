/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.MetadataField;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
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

    @Autowired(required = true)
    protected MetadataFieldService metadataFieldService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    // map of field key to authority plugin
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

    public void init() {

        if (isAuthorityRequired == null) {
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
    public boolean isAuthorityControlled(MetadataField metadataField) {
        init();
        return isAuthorityControlled(makeFieldKey(metadataField));
    }

    @Override
    public boolean isAuthorityControlled(String fieldKey) {
        init();
        return controlled.containsKey(fieldKey) && controlled.get(fieldKey);
    }

    @Override
    public boolean isAuthorityRequired(MetadataField metadataField) {
        init();
        return isAuthorityRequired(makeFieldKey(metadataField));
    }

    @Override
    public boolean isAuthorityRequired(String fieldKey) {
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
    public List<String> getAuthorityMetadata() {
        init();
        List<String> copy = new ArrayList<>();
        for (String s : controlled.keySet()) {
            copy.add(s.replaceAll("_", "."));
        }
        return copy;
    }

    @Override
    public void clearCache() {
        controlled.clear();
        minConfidence.clear();

        isAuthorityRequired = null;
    }
}
