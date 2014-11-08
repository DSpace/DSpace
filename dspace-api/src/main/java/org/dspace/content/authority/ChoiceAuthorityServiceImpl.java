/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;

import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.PluginManager;
import org.springframework.beans.factory.InitializingBean;

/**
 * Broker for ChoiceAuthority plugins, and for other information configured
 * about the choice aspect of authority control for a metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 *
 *  # names the ChoiceAuthority plugin called for this field
 *  choices.plugin.<FIELD> = name-of-plugin
 *
 *  # mode of UI presentation desired in submission UI:
 *  #  "select" is dropdown menu, "lookup" is popup with selector, "suggest" is autocomplete/suggest
 *  choices.presentation.<FIELD> = "select" | "suggest"
 *
 *  # is value "closed" to the set of these choices or are non-authority values permitted?
 *  choices.closed.<FIELD> = true | false
 *
 * @author Larry Stone
 * @see ChoiceAuthority
 */
public final class ChoiceAuthorityServiceImpl implements ChoiceAuthorityService, InitializingBean
{
    private Logger log = Logger.getLogger(ChoiceAuthorityServiceImpl.class);

    // map of field key to authority plugin
    protected Map<String,ChoiceAuthority> controller = new HashMap<String,ChoiceAuthority>();

    // map of field key to presentation type
    protected Map<String,String> presentation = new HashMap<String,String>();

    // map of field key to closed value
    protected Map<String,Boolean> closed = new HashMap<String,Boolean>();

    private ChoiceAuthorityServiceImpl() {
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {

        Enumeration pn = ConfigurationManager.propertyNames();
        final String choicesPrefix = "choices.";
        final String choicesPlugin = "choices.plugin.";
        final String choicesPresentation = "choices.presentation.";
        final String choicesClosed = "choices.closed.";
      property:
        while (pn.hasMoreElements())
        {
            String key = (String)pn.nextElement();
            if (key.startsWith(choicesPrefix))
            {
                if (key.startsWith(choicesPlugin))
                {
                    String fkey = config2fkey(key.substring(choicesPlugin.length()));
                    if (fkey == null)
                    {
                        log.warn("Skipping invalid ChoiceAuthority configuration property: "+key+": does not have schema.element.qualifier");
                        continue property;
                    }

                    // XXX FIXME maybe add sanity check, call
                    // MetadataField.findByElement to make sure it's a real field.
                     
                    ChoiceAuthority ma = (ChoiceAuthority)
                        PluginManager.getNamedPlugin(ChoiceAuthority.class, ConfigurationManager.getProperty(key));
                    if (ma == null)
                    {
                        log.warn("Skipping invalid configuration for "+key+" because named plugin not found: "+ConfigurationManager.getProperty(key));
                        continue property;
                    }
                    controller.put(fkey, ma);
                     
                    log.debug("Choice Control: For field="+fkey+", Plugin="+ma);
                }
                else if (key.startsWith(choicesPresentation))
                {
                    String fkey = config2fkey(key.substring(choicesPresentation.length()));
                    if (fkey == null)
                    {
                        log.warn("Skipping invalid ChoiceAuthority configuration property: "+key+": does not have schema.element.qualifier");
                        continue property;
                    }
                    presentation.put(fkey, ConfigurationManager.getProperty(key));
                }
                else if (key.startsWith(choicesClosed))
                {
                    String fkey = config2fkey(key.substring(choicesClosed.length()));
                    if (fkey == null)
                    {
                        log.warn("Skipping invalid ChoiceAuthority configuration property: "+key+": does not have schema.element.qualifier");
                        continue property;
                    }
                    closed.put(fkey, Boolean.valueOf(ConfigurationManager.getBooleanProperty(key)));
                }
                else
                {
                    log.error("Illegal configuration property: " + key);
                }
            }
        }
    }

    // translate tail of configuration key (supposed to be schema.element.qual)
    // into field key
    protected String config2fkey(String field)
    {
        // field is expected to be "schema.element.qualifier"
        int dot = field.indexOf('.');
        if (dot < 0)
        {
            return null;
        }
        String schema = field.substring(0, dot);
        String element = field.substring(dot+1);
        String qualifier = null;
        dot = element.indexOf('.');
        if (dot >= 0)
        {
            qualifier = element.substring(dot+1);
            element = element.substring(0, dot);
        }
        return makeFieldKey(schema, element, qualifier);
    }

    @Override
    public Choices getMatches(String schema, String element, String qualifier,
                  String query, Collection collection, int start, int limit, String locale)
    {
        return getMatches(makeFieldKey(schema, element, qualifier), query,
                collection, start, limit, locale);
    }

    @Override
    public Choices getMatches(String fieldKey, String query, Collection collection,
            int start, int limit, String locale)
    {
        ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null)
        {
            throw new IllegalArgumentException(
                    "No choices plugin was configured for  field \"" + fieldKey
                            + "\".");
        }
        return ma.getMatches(fieldKey, query, collection, start, limit, locale);
    }

    @Override
    public Choices getMatches(String fieldKey, String query, Collection collection, int start, int limit, String locale, boolean externalInput) {
        ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null) {
            throw new IllegalArgumentException(
                    "No choices plugin was configured for  field \"" + fieldKey
                            + "\".");
        }
        if (externalInput && ma instanceof SolrAuthority) {
            ((SolrAuthority)ma).addExternalResultsInNextMatches();
        }
        return ma.getMatches(fieldKey, query, collection, start, limit, locale);
    }

    @Override
    public Choices getBestMatch(String fieldKey, String query, Collection collection,
            String locale)
    {
        ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null)
        {
            throw new IllegalArgumentException(
                    "No choices plugin was configured for  field \"" + fieldKey
                            + "\".");
        }
        return ma.getBestMatch(fieldKey, query, collection, locale);
    }

    @Override
    public String getLabel(MetadataValue metadataValue, String locale)
    {
        return getLabel(metadataValue.getMetadataField().toString(), metadataValue.getAuthority(), locale);
    }

    @Override
    public String getLabel(String fieldKey, String authKey, String locale)
    {
        ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null)
        {
            throw new IllegalArgumentException("No choices plugin was configured for  field \"" + fieldKey + "\".");
        }
        return ma.getLabel(fieldKey, authKey, locale);
    }

    @Override
    public boolean isChoicesConfigured(String fieldKey)
    {
        return controller.containsKey(fieldKey);
    }

    @Override
    public String getPresentation(String fieldKey)
    {
        return presentation.get(fieldKey);
    }

    @Override
    public boolean isClosed(String fieldKey)
    {
        return closed.containsKey(fieldKey) && closed.get(fieldKey);
    }

    @Override
    public List<String> getVariants(MetadataValue metadataValue)
    {
        ChoiceAuthority ma = controller.get(metadataValue.getMetadataField().toString());
        if (ma instanceof AuthorityVariantsSupport)
        {
            AuthorityVariantsSupport avs = (AuthorityVariantsSupport) ma;
            return avs.getVariants(metadataValue.getAuthority(), metadataValue.getLanguage());
        }
        return null;
    }

    protected String makeFieldKey(String schema, String element, String qualifier)
    {
        if (qualifier == null)
        {
            return schema + "_" + element;
        }
        else
        {
            return schema + "_" + element + "_" + qualifier;
        }
    }

}
