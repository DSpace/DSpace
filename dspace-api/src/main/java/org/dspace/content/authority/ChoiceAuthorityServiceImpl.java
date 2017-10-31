/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
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
 *  # names the ChoiceAuthority plugin called for this field
 *  choices.plugin.<FIELD> = name-of-plugin
 *
 *  # mode of UI presentation desired in submission UI:
 *  #  "select" is dropdown menu, "lookup" is popup with selector, "suggest" is autocomplete/suggest
 *  choices.presentation.<FIELD> = "select" | "suggest"
 *
 *  # is value "closed" to the set of these choices or are non-authority values permitted?
 *  choices.closed.<FIELD> = true | false
 * }
 * @author Larry Stone
 * @see ChoiceAuthority
 */
public final class ChoiceAuthorityServiceImpl implements ChoiceAuthorityService
{
    private Logger log = Logger.getLogger(ChoiceAuthorityServiceImpl.class);

    // map of field key to authority plugin
    protected Map<String,ChoiceAuthority> controller = new HashMap<String,ChoiceAuthority>();

    // map of field key to presentation type
    protected Map<String,String> presentation = new HashMap<String,String>();

    // map of field key to closed value
    protected Map<String,Boolean> closed = new HashMap<String,Boolean>();

    // map of authority name to field key
    protected Map<String,String> authorities = new HashMap<String,String>();
    
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
    public Set<String> getChoiceAuthoritiesNames() 
    {
    	if (authorities.keySet().isEmpty()) {
    		loadChoiceAuthorityConfigurations();
    	}
    	return authorities.keySet();
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
        ChoiceAuthority ma = getChoiceAuthorityMap().get(fieldKey);
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
        ChoiceAuthority ma = getChoiceAuthorityMap().get(fieldKey);
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
        ChoiceAuthority ma = getChoiceAuthorityMap().get(fieldKey);
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
        ChoiceAuthority ma = getChoiceAuthorityMap().get(fieldKey);
        if (ma == null)
        {
            throw new IllegalArgumentException("No choices plugin was configured for  field \"" + fieldKey + "\".");
        }
        return ma.getLabel(fieldKey, authKey, locale);
    }

    @Override
    public boolean isChoicesConfigured(String fieldKey)
    {
        return getChoiceAuthorityMap().containsKey(fieldKey);
    }

    @Override
    public String getPresentation(String fieldKey)
    {
        return getPresentationMap().get(fieldKey);
    }

    @Override
    public boolean isClosed(String fieldKey)
    {
        return getClosedMap().containsKey(fieldKey) && getClosedMap().get(fieldKey);
    }

    @Override
    public List<String> getVariants(MetadataValue metadataValue)
    {
        ChoiceAuthority ma = getChoiceAuthorityMap().get(metadataValue.getMetadataField().toString());
        if (ma instanceof AuthorityVariantsSupport)
        {
            AuthorityVariantsSupport avs = (AuthorityVariantsSupport) ma;
            return avs.getVariants(metadataValue.getAuthority(), metadataValue.getLanguage());
        }
        return null;
    }
    
    
    @Override
    public String getChoiceAuthorityName(String schema, String element, String qualifier) {
    	String makeFieldKey = makeFieldKey(schema, element, qualifier);
		if(getChoiceAuthorityMap().containsKey(makeFieldKey)) {			
    		for(String key : this.authorities.keySet()) {
    			if(this.authorities.get(key).equals(makeFieldKey)) {
    				return key;
    			}
    		}
    	}
		return configurationService.getProperty(
				CHOICES_PLUGIN_PREFIX + schema + "." + element + (qualifier != null ? "." + qualifier : ""));
    }

    protected String makeFieldKey(String schema, String element, String qualifier)
    {
        return Utils.standardize(schema, element, qualifier, "_");
    }

    /**
     * Return map of key to ChoiceAuthority plugin
     * @return
     */
    private Map<String,ChoiceAuthority> getChoiceAuthorityMap()
    {
        // If empty, load from configuration
        if(controller.isEmpty())
        {
            loadChoiceAuthorityConfigurations();
        }

        return controller;
    }

	private void loadChoiceAuthorityConfigurations() {
		// Get all configuration keys starting with a given prefix
		List<String> propKeys = configurationService.getPropertyKeys(CHOICES_PLUGIN_PREFIX);
		Iterator<String> keyIterator = propKeys.iterator();
		while(keyIterator.hasNext())
		{
		    String key = keyIterator.next();
		    String fkey = config2fkey(key.substring(CHOICES_PLUGIN_PREFIX.length()));
		    if (fkey == null)
		    {
		        log.warn("Skipping invalid ChoiceAuthority configuration property: "+key+": does not have schema.element.qualifier");
		        continue;
		    }

		    // XXX FIXME maybe add sanity check, call
		    // MetadataField.findByElement to make sure it's a real field.
		    String authorityName = configurationService.getProperty(key);
			ChoiceAuthority ma = (ChoiceAuthority)
		        pluginService.getNamedPlugin(ChoiceAuthority.class, authorityName);
		    if (ma == null)
		    {
		        log.warn("Skipping invalid configuration for "+key+" because named plugin not found: "+authorityName);
		        continue;
		    }
		    if(!authorities.containsKey(authorityName)) {
		    	controller.put(fkey, ma);
		    	authorities.put(authorityName, fkey);
		    }
		    else {
		    	log.warn("Skipping invalid configuration for "+key+" because plugin is alredy in use: "+authorityName +" used by " + authorities.get(authorityName));
		        continue;		    	
		    }
		    
		    log.debug("Choice Control: For field="+fkey+", Plugin="+ma);
		}
		autoRegisterChoiceAuthorityFromInputReader();
	}

	private void autoRegisterChoiceAuthorityFromInputReader() {
		try {
			DCInputsReader dcInputsReader = new DCInputsReader();
			for (DCInputSet dcinputSet : dcInputsReader.getAllInputs(Integer.MAX_VALUE, 0)) {
				DCInput[] dcinputs = dcinputSet.getFields();
				for (DCInput dcinput : dcinputs) {
					if (StringUtils.isNotBlank(dcinput.getPairsType())
							|| StringUtils.isNotBlank(dcinput.getVocabulary())) {
						String authorityName = dcinput.getPairsType();
						if(StringUtils.isBlank(authorityName)) {
							authorityName = dcinput.getVocabulary();
						}
						if (!StringUtils.equals(dcinput.getInputType(), "qualdrop_value")) {
							String fieldKey = makeFieldKey(dcinput.getSchema(), dcinput.getElement(),
									dcinput.getQualifier());
							ChoiceAuthority ca = controller.get(authorityName);
							if (ca == null) {
								InputFormSelfRegisterWrapperAuthority ifa = new InputFormSelfRegisterWrapperAuthority();
								if(controller.containsKey(fieldKey)) {
									ifa = (InputFormSelfRegisterWrapperAuthority)controller.get(fieldKey);
								}
								
								ChoiceAuthority ma = (ChoiceAuthority)pluginService.getNamedPlugin(ChoiceAuthority.class, authorityName);
								if (ma == null) {
									log.warn("Skipping invalid configuration for " + fieldKey
											+ " because named plugin not found: " + authorityName);
									continue;
								}
								ifa.getDelegates().put(dcinputSet.getFormName(), ma);
								controller.put(fieldKey, ifa);
							} 
							
							if (!authorities.containsKey(authorityName)) {
								authorities.put(authorityName, fieldKey);
							}

						}
					}
				}
			}
		} catch (DCInputsReaderException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

    /**
     * Return map of key to presentation
     * @return
     */
    private Map<String,String> getPresentationMap()
    {
        // If empty, load from configuration
        if(presentation.isEmpty())
        {
            // Get all configuration keys starting with a given prefix
            List<String> propKeys = configurationService.getPropertyKeys(CHOICES_PRESENTATION_PREFIX);
            Iterator<String> keyIterator = propKeys.iterator();
            while(keyIterator.hasNext())
            {
                String key = keyIterator.next();

                String fkey = config2fkey(key.substring(CHOICES_PRESENTATION_PREFIX.length()));
                if (fkey == null)
                {
                    log.warn("Skipping invalid ChoiceAuthority configuration property: "+key+": does not have schema.element.qualifier");
                    continue;
                }
                presentation.put(fkey, configurationService.getProperty(key));
            }
        }

        return presentation;
    }

    /**
     * Return map of key to closed setting
     * @return
     */
    private Map<String,Boolean> getClosedMap()
    {
        // If empty, load from configuration
        if(closed.isEmpty())
        {
            // Get all configuration keys starting with a given prefix
            List<String> propKeys = configurationService.getPropertyKeys(CHOICES_CLOSED_PREFIX);
            Iterator<String> keyIterator = propKeys.iterator();
            while(keyIterator.hasNext())
            {
                String key = keyIterator.next();

                String fkey = config2fkey(key.substring(CHOICES_CLOSED_PREFIX.length()));
                if (fkey == null)
                {
                    log.warn("Skipping invalid ChoiceAuthority configuration property: "+key+": does not have schema.element.qualifier");
                    continue;
                }
                closed.put(fkey, configurationService.getBooleanProperty(key));
            }
        }

        return closed;
    }

	@Override
	public String getChoiceMetadatabyAuthorityName(String name) {
		if(authorities.isEmpty()) {
			loadChoiceAuthorityConfigurations();
		}
		if(authorities.containsKey(name)) {
			return authorities.get(name);
		}
		return null;
	}

	@Override
	public Choice getChoice(String fieldKey, String authKey, String locale) {
		ChoiceAuthority ma = getChoiceAuthorityMap().get(fieldKey);
		if (ma == null) {
			throw new IllegalArgumentException("No choices plugin was configured for  field \"" + fieldKey + "\".");
		}
		return ma.getChoice(fieldKey, authKey, locale);
	}

	@Override
	public ChoiceAuthority getChoiceAuthorityByAuthorityName(String authorityName) {
		ChoiceAuthority ma = (ChoiceAuthority)
		        pluginService.getNamedPlugin(ChoiceAuthority.class, authorityName);
        if (ma == null)
        {
            throw new IllegalArgumentException(
                    "No choices plugin was configured for authorityName \"" + authorityName
                            + "\".");
        }
        return ma;
	}
}
