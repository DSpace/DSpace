/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.content.Collection;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.Utils;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Broker for ChoiceAuthority plugins, and for other information configured
 * about the choice aspect of authority control for a metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 *
 * {@code
 * # names the ChoiceAuthority plugin called for this field
 * choices.plugin.<FIELD> = name-of-plugin
 *
 * # mode of UI presentation desired in submission UI:
 * #  "select" is dropdown menu, "lookup" is popup with selector, "suggest" is autocomplete/suggest
 * choices.presentation.<FIELD> = "select" | "suggest"
 *
 * # is value "closed" to the set of these choices or are non-authority values permitted?
 * choices.closed.<FIELD> = true | false
 * }
 *
 * @author Larry Stone
 * @see ChoiceAuthority
 */
public final class ChoiceAuthorityServiceImpl implements ChoiceAuthorityService {
    private Logger log = org.apache.logging.log4j.LogManager.getLogger(ChoiceAuthorityServiceImpl.class);

    // map of field key to authority plugin
    protected Map<String, ChoiceAuthority> controller = new HashMap<String, ChoiceAuthority>();

    // map of field key, form definition to authority plugin
    protected Map<String, Map<String, ChoiceAuthority>> controllerFormDefinitions =
            new HashMap<String, Map<String, ChoiceAuthority>>();

    // map of field key to presentation type
    protected Map<String, String> presentation = new HashMap<String, String>();

    // map of field key to closed value
    protected Map<String, Boolean> closed = new HashMap<String, Boolean>();

    // flag to track the initialization status of the service
    private boolean initialized = false;

    // map of authority name to field keys (the same authority can be configured over multiple metadata)
    protected Map<String, List<String>> authorities = new HashMap<String, List<String>>();

    // map of authority name to form definition and field keys
    protected Map<String, Map<String, List<String>>> authoritiesFormDefinitions =
            new HashMap<String, Map<String, List<String>>>();

    // the item submission reader
    private SubmissionConfigReader itemSubmissionConfigReader;

    @Autowired(required = true)
    protected ConfigurationService configurationService;
    @Autowired(required = true)
    protected PluginService pluginService;

    private final String CHOICES_PLUGIN_PREFIX = "choices.plugin.";
    private final String CHOICES_PRESENTATION_PREFIX = "choices.presentation.";
    private final String CHOICES_CLOSED_PREFIX = "choices.closed.";

    protected ChoiceAuthorityServiceImpl() {
    }

    // translate tail of configuration key (supposed to be schema.element.qual)
    // into field key
    protected String config2fkey(String field) {
        // field is expected to be "schema.element.qualifier"
        int dot = field.indexOf('.');
        if (dot < 0) {
            return null;
        }
        String schema = field.substring(0, dot);
        String element = field.substring(dot + 1);
        String qualifier = null;
        dot = element.indexOf('.');
        if (dot >= 0) {
            qualifier = element.substring(dot + 1);
            element = element.substring(0, dot);
        }
        return makeFieldKey(schema, element, qualifier);
    }

    @Override
    public Set<String> getChoiceAuthoritiesNames() {
        init();
        Set<String> authoritiesNames = new HashSet<String>();
        authoritiesNames.addAll(authorities.keySet());
        authoritiesNames.addAll(authoritiesFormDefinitions.keySet());
        return authoritiesNames;
    }

    private synchronized void init() {
        if (!initialized) {
            try {
                itemSubmissionConfigReader = new SubmissionConfigReader();
            } catch (SubmissionConfigReaderException e) {
                // the system is in an illegal state as the submission definition is not valid
                throw new IllegalStateException(e);
            }
            loadChoiceAuthorityConfigurations();
            initialized = true;
        }
    }

    @Override
    public Choices getMatches(String schema, String element, String qualifier,
                              String query, Collection collection, int start, int limit, String locale) {
        return getMatches(makeFieldKey(schema, element, qualifier), query,
                          collection, start, limit, locale);
    }

    @Override
    public Choices getMatches(String fieldKey, String query, Collection collection,
                              int start, int limit, String locale) {
        ChoiceAuthority ma = getAuthorityByFieldKeyCollection(fieldKey, collection);
        if (ma == null) {
            throw new IllegalArgumentException(
                "No choices plugin was configured for  field \"" + fieldKey
                    + "\", collection=" + collection.getID().toString() + ".");
        }
        return ma.getMatches(fieldKey, query, collection, start, limit, locale);
    }


    @Override
    public Choices getMatches(String fieldKey, String query, Collection collection, int start, int limit, String locale,
                              boolean externalInput) {
        ChoiceAuthority ma = getAuthorityByFieldKeyCollection(fieldKey, collection);
        if (ma == null) {
            throw new IllegalArgumentException(
                "No choices plugin was configured for  field \"" + fieldKey
                    + "\", collection=" + collection.getID().toString() + ".");
        }
        if (externalInput && ma instanceof SolrAuthority) {
            ((SolrAuthority) ma).addExternalResultsInNextMatches();
        }
        return ma.getMatches(fieldKey, query, collection, start, limit, locale);
    }

    @Override
    public Choices getBestMatch(String fieldKey, String query, Collection collection,
                                String locale) {
        ChoiceAuthority ma = getAuthorityByFieldKeyCollection(fieldKey, collection);
        if (ma == null) {
            throw new IllegalArgumentException(
                "No choices plugin was configured for  field \"" + fieldKey
                    + "\", collection=" + collection.getID().toString() + ".");
        }
        return ma.getBestMatch(fieldKey, query, collection, locale);
    }

    @Override
    public String getLabel(MetadataValue metadataValue, Collection collection, String locale) {
        return getLabel(metadataValue.getMetadataField().toString(), collection, metadataValue.getAuthority(), locale);
    }

    @Override
    public String getLabel(String fieldKey, Collection collection, String authKey, String locale) {
        ChoiceAuthority ma = getAuthorityByFieldKeyCollection(fieldKey, collection);
        if (ma == null) {
            throw new IllegalArgumentException(
                "No choices plugin was configured for  field \"" + fieldKey
                    + "\", collection=" + collection.getID().toString() + ".");
        }
        return ma.getLabel(authKey, locale);
    }

    @Override
    public boolean isChoicesConfigured(String fieldKey, Collection collection) {
        return getAuthorityByFieldKeyCollection(fieldKey, collection) != null;
    }

    @Override
    public String getPresentation(String fieldKey) {
        return getPresentationMap().get(fieldKey);
    }

    @Override
    public boolean isClosed(String fieldKey) {
        return getClosedMap().containsKey(fieldKey) && getClosedMap().get(fieldKey);
    }

    @Override
    public List<String> getVariants(MetadataValue metadataValue, Collection collection) {
        String fieldKey = metadataValue.getMetadataField().toString();
        ChoiceAuthority ma = getAuthorityByFieldKeyCollection(fieldKey, collection);
        if (ma == null) {
            throw new IllegalArgumentException(
                "No choices plugin was configured for  field \"" + fieldKey
                    + "\", collection=" + collection.getID().toString() + ".");
        }
        if (ma instanceof AuthorityVariantsSupport) {
            AuthorityVariantsSupport avs = (AuthorityVariantsSupport) ma;
            return avs.getVariants(metadataValue.getAuthority(), metadataValue.getLanguage());
        }
        return null;
    }


    @Override
    public String getChoiceAuthorityName(String schema, String element, String qualifier, Collection collection) {
        init();
        String fieldKey = makeFieldKey(schema, element, qualifier);
        // check if there is an authority configured for the metadata valid for all the collections
        if (controller.containsKey(fieldKey)) {
            for (Entry<String, List<String>> authority2md : authorities.entrySet()) {
                if (authority2md.getValue().contains(fieldKey)) {
                    return authority2md.getKey();
                }
            }
        } else if (collection != null && controllerFormDefinitions.containsKey(fieldKey)) {
            // there is an authority configured for the metadata valid for some collections,
            // check if it is the requested collection
            Map<String, ChoiceAuthority> controllerFormDef = controllerFormDefinitions.get(fieldKey);
            SubmissionConfig submissionConfig = itemSubmissionConfigReader
                    .getSubmissionConfigByCollection(collection.getHandle());
            String submissionName = submissionConfig.getSubmissionName();
            // check if the requested collection has a submission definition that use an authority for the metadata
            if (controllerFormDef.containsKey(submissionName)) {
                for (Entry<String, Map<String, List<String>>> authority2defs2md :
                        authoritiesFormDefinitions.entrySet()) {
                    List<String> mdByDefinition = authority2defs2md.getValue().get(submissionName);
                    if (mdByDefinition != null && mdByDefinition.contains(fieldKey)) {
                        return authority2defs2md.getKey();
                    }
                }
            }
        }
        return null;
    }

    protected String makeFieldKey(String schema, String element, String qualifier) {
        return Utils.standardize(schema, element, qualifier, "_");
    }

    @Override
    public void clearCache() {
        controller.clear();
        authorities.clear();
        controllerFormDefinitions.clear();
        authoritiesFormDefinitions.clear();
        itemSubmissionConfigReader = null;
        initialized = false;
    }

    private void loadChoiceAuthorityConfigurations() {
        // Get all configuration keys starting with a given prefix
        List<String> propKeys = configurationService.getPropertyKeys(CHOICES_PLUGIN_PREFIX);
        Iterator<String> keyIterator = propKeys.iterator();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            String fkey = config2fkey(key.substring(CHOICES_PLUGIN_PREFIX.length()));
            if (fkey == null) {
                log.warn(
                    "Skipping invalid ChoiceAuthority configuration property: " + key + ": does not have schema" +
                        ".element.qualifier");
                continue;
            }

            // XXX FIXME maybe add sanity check, call
            // MetadataField.findByElement to make sure it's a real field.
            String authorityName = configurationService.getProperty(key);
            ChoiceAuthority ma = (ChoiceAuthority)
                pluginService.getNamedPlugin(ChoiceAuthority.class, authorityName);
            if (ma == null) {
                log.warn(
                    "Skipping invalid configuration for " + key + " because named plugin not found: " + authorityName);
                continue;
            }

            controller.put(fkey, ma);
            List<String> fkeys;
            if (authorities.containsKey(authorityName)) {
                fkeys = authorities.get(authorityName);
            } else {
                fkeys = new ArrayList<String>();
            }
            fkeys.add(fkey);
            authorities.put(authorityName, fkeys);
            log.debug("Choice Control: For field=" + fkey + ", Plugin=" + ma);
        }
        autoRegisterChoiceAuthorityFromInputReader();
    }

    private void autoRegisterChoiceAuthorityFromInputReader() {
        try {
            List<SubmissionConfig> submissionConfigs = itemSubmissionConfigReader
                    .getAllSubmissionConfigs(Integer.MAX_VALUE, 0);
            DCInputsReader dcInputsReader = new DCInputsReader();

            for (SubmissionConfig subCfg : submissionConfigs) {
                String submissionName = subCfg.getSubmissionName();
                List<DCInputSet> inputsBySubmissionName = dcInputsReader.getInputsBySubmissionName(submissionName);
                for (DCInputSet dcinputSet : inputsBySubmissionName) {
                    DCInput[][] dcinputs = dcinputSet.getFields();
                    for (DCInput[] dcrows : dcinputs) {
                        for (DCInput dcinput : dcrows) {
                            if (StringUtils.isNotBlank(dcinput.getPairsType())
                                || StringUtils.isNotBlank(dcinput.getVocabulary())) {
                                String authorityName = dcinput.getPairsType();
                                if (StringUtils.isBlank(authorityName)) {
                                    authorityName = dcinput.getVocabulary();
                                }
                                if (!StringUtils.equals(dcinput.getInputType(), "qualdrop_value")) {
                                    String fieldKey = makeFieldKey(dcinput.getSchema(), dcinput.getElement(),
                                                                   dcinput.getQualifier());
                                    ChoiceAuthority ca = controller.get(authorityName);
                                    if (ca == null) {
                                        ca = (ChoiceAuthority) pluginService
                                            .getNamedPlugin(ChoiceAuthority.class, authorityName);
                                        if (ca == null) {
                                            throw new IllegalStateException("Invalid configuration for " + fieldKey
                                                    + " in submission definition " + submissionName
                                                    + ", form definition " + dcinputSet.getFormName()
                                                    + " no named plugin found: " + authorityName);
                                        }
                                    }

                                    Map<String, ChoiceAuthority> definition2authority;
                                    if (controllerFormDefinitions.containsKey(fieldKey)) {
                                        definition2authority = controllerFormDefinitions.get(fieldKey);
                                    } else {
                                        definition2authority = new HashMap<String, ChoiceAuthority>();
                                    }
                                    definition2authority.put(submissionName, ca);
                                    controllerFormDefinitions.put(fieldKey, definition2authority);

                                    Map<String, List<String>> authorityName2definitions;
                                    if (authoritiesFormDefinitions.containsKey(authorityName)) {
                                        authorityName2definitions = authoritiesFormDefinitions.get(authorityName);
                                    } else {
                                        authorityName2definitions = new HashMap<String, List<String>>();
                                    }

                                    List<String> fields;
                                    if (authorityName2definitions.containsKey(submissionName)) {
                                        fields = authorityName2definitions.get(submissionName);
                                    } else {
                                        fields = new ArrayList<String>();
                                    }
                                    fields.add(fieldKey);
                                    authorityName2definitions.put(submissionName, fields);
                                    authoritiesFormDefinitions.put(authorityName, authorityName2definitions);
                                }
                            }
                        }
                    }
                }
            }
        } catch (DCInputsReaderException e) {
            // the system is in an illegal state as the submission definition is not valid
            throw new IllegalStateException(e);
        }
    }

    /**
     * Return map of key to presentation
     *
     * @return
     */
    private Map<String, String> getPresentationMap() {
        // If empty, load from configuration
        if (presentation.isEmpty()) {
            // Get all configuration keys starting with a given prefix
            List<String> propKeys = configurationService.getPropertyKeys(CHOICES_PRESENTATION_PREFIX);
            Iterator<String> keyIterator = propKeys.iterator();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();

                String fkey = config2fkey(key.substring(CHOICES_PRESENTATION_PREFIX.length()));
                if (fkey == null) {
                    log.warn(
                        "Skipping invalid ChoiceAuthority configuration property: " + key + ": does not have schema" +
                            ".element.qualifier");
                    continue;
                }
                presentation.put(fkey, configurationService.getProperty(key));
            }
        }

        return presentation;
    }

    /**
     * Return map of key to closed setting
     *
     * @return
     */
    private Map<String, Boolean> getClosedMap() {
        // If empty, load from configuration
        if (closed.isEmpty()) {
            // Get all configuration keys starting with a given prefix
            List<String> propKeys = configurationService.getPropertyKeys(CHOICES_CLOSED_PREFIX);
            Iterator<String> keyIterator = propKeys.iterator();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();

                String fkey = config2fkey(key.substring(CHOICES_CLOSED_PREFIX.length()));
                if (fkey == null) {
                    log.warn(
                        "Skipping invalid ChoiceAuthority configuration property: " + key + ": does not have schema" +
                            ".element.qualifier");
                    continue;
                }
                closed.put(fkey, configurationService.getBooleanProperty(key));
            }
        }

        return closed;
    }

    @Override
    public ChoiceAuthority getChoiceAuthorityByAuthorityName(String authorityName) {
        ChoiceAuthority ma = (ChoiceAuthority)
            pluginService.getNamedPlugin(ChoiceAuthority.class, authorityName);
        if (ma == null) {
            throw new IllegalArgumentException(
                "No choices plugin was configured for authorityName \"" + authorityName
                    + "\".");
        }
        return ma;
    }

    private ChoiceAuthority getAuthorityByFieldKeyCollection(String fieldKey, Collection collection) {
        init();
        ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null) {
            SubmissionConfigReader configReader;
            try {
                configReader = new SubmissionConfigReader();
                SubmissionConfig submissionName = configReader.getSubmissionConfigByCollection(collection.getHandle());
                ma = controllerFormDefinitions.get(submissionName.getSubmissionName()).get(fieldKey);
            } catch (SubmissionConfigReaderException e) {
                // the system is in an illegal state as the submission definition is not valid
                throw new IllegalStateException(e);
            }
        }
        return ma;
    }
}
