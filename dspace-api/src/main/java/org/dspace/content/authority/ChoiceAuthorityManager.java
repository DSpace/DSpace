/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.content.Item;
import org.dspace.core.PluginManager;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Broker for ChoiceAuthority plugins, and for other information configured
 * about the choice aspect of authority control for a metadata field.
 * 
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 * 
 * # names the ChoiceAuthority plugin called for this field
 * choices.plugin.<FIELD> = name-of-plugin
 * 
 * # mode of UI presentation desired in submission UI: # "select" is dropdown
 * menu, "lookup" is popup with selector, "suggest" is autocomplete/suggest
 * choices.presentation.<FIELD> = "select" | "suggest"
 * 
 * # is value "closed" to the set of these choices or are non-authority values
 * permitted? choices.closed.<FIELD> = true | false
 * 
 * @author Larry Stone
 * @see ChoiceAuthority
 */
public final class ChoiceAuthorityManager
{
    private static Logger log = Logger.getLogger(ChoiceAuthorityManager.class);

    private static ChoiceAuthorityManager cached = null;

    // map of field key to authority plugin
    private Map<String, ChoiceAuthority> controller = new HashMap<String, ChoiceAuthority>();

    // map of field key to presentation type
    private Map<String, String> presentation = new HashMap<String, String>();

    // map of field key to authority plugin name
    private Map<String,String> md2authorityname = new HashMap<String,String>();
    
    // map of field key to closed value
    private Map<String, Boolean> closed = new HashMap<String, Boolean>();

    private ChoiceAuthorityManager()
    {

        Enumeration pn = ConfigurationManager.propertyNames();
        final String choicesPrefix = "choices.";
        final String choicesPlugin = "choices.plugin.";
        final String choicesPresentation = "choices.presentation.";
        final String choicesClosed = "choices.closed.";
        Context context = null;
        try 
        {
	        context = new Context();
	        String defaultKey =  makeFieldKey(MetadataSchema.DC_SCHEMA, "authority", "default");
	        String defaultfKey = ConfigurationManager.getProperty(choicesPlugin+MetadataSchema.DC_SCHEMA+".authority.default");
	        ChoiceAuthority maDefault = (ChoiceAuthority) PluginManager
                    .getNamedPlugin(ChoiceAuthority.class,
                            ConfigurationManager
                                    .getProperty(defaultKey));
	        MetadataField[] tmp = MetadataField.findAllByElement(
	                context, MetadataSchema.DC_SCHEMA_ID, "authority", Item.ANY);
	        for(MetadataField mf : tmp) {
	            String tmpKey = makeFieldKey(MetadataSchema.DC_SCHEMA, mf.getElement(), mf.getQualifier());
	            String tmpfKey = ConfigurationManager.getProperty(choicesPlugin+MetadataSchema.DC_SCHEMA+"."+mf.getElement()+"."+mf.getQualifier());
	            ChoiceAuthority ma = (ChoiceAuthority) PluginManager
                        .getNamedPlugin(ChoiceAuthority.class,
                                ConfigurationManager
                                        .getProperty(tmpKey));
	            if (ma == null)
	            {
	                ma = maDefault;
	                tmpfKey = defaultfKey;
	            }

	            md2authorityname.put(tmpKey,
	                        ConfigurationManager.getProperty(tmpfKey));
                controller.put(tmpKey, ma);
	        }
        }
        catch (Exception e) {
        	log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        property: while (pn.hasMoreElements())
        {
            String key = (String) pn.nextElement();
            if (key.startsWith(choicesPrefix))
            {
                if (key.startsWith(choicesPlugin))
                {
                    String fkey = config2fkey(key.substring(choicesPlugin
                            .length()));
                    if (fkey == null)
                    {
                        log.warn("Skipping invalid ChoiceAuthority configuration property: "
                                + key
                                + ": does not have schema.element.qualifier");
                        continue property;
                    }
                    

                    try {
                        ChoiceAuthority ma = (ChoiceAuthority) PluginManager
                                    .getNamedPlugin(ChoiceAuthority.class,
                                            ConfigurationManager
                                                    .getProperty(key));
                        if (ma == null)
                        {
                            log.warn("Skipping invalid configuration for "
                                    + key + " because named plugin not found: "
                                    + ConfigurationManager.getProperty(key));
                            continue property;
                        }
                        controller.put(fkey, ma);

                        md2authorityname.put(fkey,
                                ConfigurationManager.getProperty(key));

                        log.debug("Choice Control: For field=" + fkey
                                + ", Plugin=" + ma);
                    }
                    catch (Exception e)
                    {
                        log.warn("Skipping invalid ChoiceAuthority configuration property: "
                                + key
                                + ": sanity check failure, it's not a real field - note that could be an item enhancer tips");
                    }
                }
                else if (key.startsWith(choicesPresentation))
                {
                    String fkey = config2fkey(key.substring(choicesPresentation
                            .length()));
                    if (fkey == null)
                    {
                        log.warn("Skipping invalid ChoiceAuthority configuration property: "
                                + key
                                + ": does not have schema.element.qualifier");
                        continue property;
                    }
                    presentation.put(fkey,
                            ConfigurationManager.getProperty(key));
                }
                else if (key.startsWith(choicesClosed))
                {
                    String fkey = config2fkey(key.substring(choicesClosed
                            .length()));
                    if (fkey == null)
                    {
                        log.warn("Skipping invalid ChoiceAuthority configuration property: "
                                + key
                                + ": does not have schema.element.qualifier");
                        continue property;
                    }
                    closed.put(fkey, Boolean.valueOf(ConfigurationManager
                            .getBooleanProperty(key)));
                }
                else
                {
                    log.error("Illegal configuration property: " + key);
                }
            }
        }
    }

    /** Factory method */
    public static ChoiceAuthorityManager getManager()
    {
        if (cached == null)
        {
            cached = new ChoiceAuthorityManager();
        }
        return cached;
    }

    // translate tail of configuration key (supposed to be schema.element.qual)
    // into field key
    private String config2fkey(String field)
    {
        // field is expected to be "schema.element.qualifier"
        int dot = field.indexOf('.');
        if (dot < 0)
        {
            return null;
        }
        String schema = field.substring(0, dot);
        String element = field.substring(dot + 1);
        String qualifier = null;
        dot = element.indexOf('.');
        if (dot >= 0)
        {
            qualifier = element.substring(dot + 1);
            element = element.substring(0, dot);
        }
        return makeFieldKey(schema, element, qualifier);
    }

    /**
     * Wrapper that calls getMatches method of the plugin corresponding to the
     * metadata field defined by schema,element,qualifier.
     * 
     * @see ChoiceAuthority#getMatches(String, String, int, int, int, String)
     * @param schema
     *            schema of metadata field
     * @param element
     *            element of metadata field
     * @param qualifier
     *            qualifier of metadata field
     * @param query
     *            user's value to match
     * @param collection
     *            database ID of Collection for context (owner of Item)
     * @param start
     *            choice at which to start, 0 is first.
     * @param limit
     *            maximum number of choices to return, 0 for no limit.
     * @param locale
     *            explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getMatches(String schema, String element, String qualifier,
            String query, int collection, int start, int limit, String locale)
    {
        return getMatches(makeFieldKey(schema, element, qualifier), query,
                collection, start, limit, locale);
    }

    /**
     * Wrapper calls getMatches method of the plugin corresponding to the
     * metadata field defined by single field key.
     * 
     * @see ChoiceAuthority#getMatches(String, String, int, int, int, String)
     * @param fieldKey
     *            single string identifying metadata field
     * @param query
     *            user's value to match
     * @param collection
     *            database ID of Collection for context (owner of Item)
     * @param start
     *            choice at which to start, 0 is first.
     * @param limit
     *            maximum number of choices to return, 0 for no limit.
     * @param locale
     *            explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getMatches(String fieldKey, String query, int collection,
            int start, int limit, String locale)
    {
        ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null)
        {
            ma = reloadCache(fieldKey);
            if(ma == null) {
                throw new IllegalArgumentException(
                    "No choices plugin was configured for  field \"" + fieldKey
                            + "\".");
            }
        }
        return ma.getMatches(fieldKey, query, collection, start, limit, locale);
    }

    public Choices getMatches(String fieldKey, String query, int collection, int start, int limit, String locale, boolean externalInput) {
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

    /**
     * Wrapper that calls getBestMatch method of the plugin corresponding to the
     * metadata field defined by single field key.
     * 
     * @see ChoiceAuthority#getBestMatch(String, String, int, String)
     * @param fieldKey
     *            single string identifying metadata field
     * @param query
     *            user's value to match
     * @param collection
     *            database ID of Collection for context (owner of Item)
     * @param locale
     *            explicit localization key if available, or null
     * @return a Choices object (never null) with 1 or 0 values.
     */
    public Choices getBestMatch(String fieldKey, String query, int collection,
            String locale)
    {
        ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null)
        {
            ma = reloadCache(fieldKey);
            if(ma == null) {
                throw new IllegalArgumentException(
                        "No choices plugin was configured for  field \""
                                + fieldKey + "\".");
            }
        }
        return ma.getBestMatch(fieldKey, query, collection, locale);
    }

    /**
     * Wrapper that calls getLabel method of the plugin corresponding to the
     * metadata field defined by schema,element,qualifier.
     */
    public String getLabel(String schema, String element, String qualifier,
            String authKey, String locale)
    {
        return getLabel(makeFieldKey(schema, element, qualifier), authKey,
                locale);
    }

    /**
     * Wrapper that calls getLabel method of the plugin corresponding to the
     * metadata field defined by single field key.
     */
    public String getLabel(String fieldKey, String authKey, String locale)
    {
        ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null)
        {
            ma = reloadCache(fieldKey);
            if(ma == null) {
                throw new IllegalArgumentException(
                    "No choices plugin was configured for  field \"" + fieldKey
                            + "\".");
            }
        }
        return ma.getLabel(fieldKey, authKey, locale);
    }

    /**
     * Predicate, is there a Choices configuration of any kind for the given
     * metadata field?
     * 
     * @return true if choices are configured for this field.
     */
    public boolean isChoicesConfigured(String fieldKey)
    {
        boolean result = controller.containsKey(fieldKey);
        if(fieldKey.contains("_authority_") && !result) {
            reloadCache();
            return true;
        }
        return result;
    }

    /**
     * Get the presentation keyword (should be "lookup", "select" or "suggest",
     * but this is an informal convention so it can be easily extended) for this
     * field.
     * 
     * @return configured presentation type for this field, or null if none
     *         found
     */
    public String getPresentation(String fieldKey)
    {
        String result = presentation.get(fieldKey);
        if(result==null && fieldKey.contains("_authority_")) {
            result = presentation.get("dc_authority_default");
        }
        return result;
    }

    /**
     * Get the configured "closed" value for this field.
     * 
     * @return true if choices are closed for this field.
     */
    public boolean isClosed(String fieldKey)
    {
        boolean result = closed.containsKey(fieldKey) ? closed.get(fieldKey)
                .booleanValue() : false;
        if(result==false && fieldKey.contains("_authority_")) {
            result = closed.containsKey(fieldKey) ?closed.get("dc_authority_default").booleanValue() : false;
        }
        return result;
    }

    /**
     * Construct a single key from the tuple of schema/element/qualifier that
     * describes a metadata field. Punt to the function we use for submission UI
     * input forms, for now.
     */
    public static String makeFieldKey(String schema, String element,
            String qualifier)
    {
        return MetadataField.formKey(schema, element, qualifier);
    }

    /**
     * Construct a single key from the "dot" notation e.g. "dc.rights"
     */
    public static String makeFieldKey(String dotty)
    {
        return dotty.replace(".", "_");
    }

    /**
     * Wrapper to call plugin's getVariants().
     */
    public List<String> getVariants(String schema, String element,
            String qualifier, String authorityKey, String language)
    {
        ChoiceAuthority ma = controller.get(makeFieldKey(schema, element,
                qualifier));
        if (ma instanceof AuthorityVariantsSupport)
        {
            AuthorityVariantsSupport avs = (AuthorityVariantsSupport) ma;
            return avs.getVariants(authorityKey, language);
        }
        return null;
    }

    /**
     * Wrapper that calls reject method of the plugin corresponding
     * 
     */
    public void notifyReject(int itemID, String schema, String element,
            String qualifier, String authorityKey)
    {
        String makeFieldKey = makeFieldKey(schema, element,
                qualifier);
        ChoiceAuthority ma = controller.get(makeFieldKey);
        if(ma == null) {
            reloadCache(makeFieldKey);
        }
        if (ma instanceof NotificableAuthority)
        {
            NotificableAuthority avs = (NotificableAuthority) ma;
            avs.reject(itemID, authorityKey);
        }
    }

    /**
     * Wrapper that calls accept potential method of the plugin corresponding
     * 
     */
    public void notifyAccept(int itemID, String schema, String element,
			String qualifier, String authorityKey, int confidence)
    {
        String makeFieldKey = makeFieldKey(schema, element,
                qualifier);
        ChoiceAuthority ma = controller.get(makeFieldKey);
        if(ma == null) {
            reloadCache(makeFieldKey);
        }
        if (ma instanceof NotificableAuthority)
        {
            NotificableAuthority avs = (NotificableAuthority) ma;
			avs.accept(itemID, authorityKey, confidence);
        }
    }
        
    /**
     * Wrapper that calls reject method of the plugin corresponding
     * 
     */
    public void notifyReject(int[] itemIDs, String schema, String element,
            String qualifier, String authorityKey)
    {
        String makeFieldKey = makeFieldKey(schema, element,
                qualifier);
        ChoiceAuthority ma = controller.get(makeFieldKey);
        if(ma == null) {
            reloadCache(makeFieldKey);
        }
        if (ma instanceof NotificableAuthority)
        {
            NotificableAuthority avs = (NotificableAuthority) ma;
            avs.reject(itemIDs, authorityKey);
        }
    }

    public Object getDetailsInfo(String field, String key, String locale)
    {
        ChoiceAuthority ma = controller.get(field);
        if(ma == null) {
            reloadCache(field);
        }
        if (ma instanceof ChoiceAuthorityDetails)
        {
        	ChoiceAuthorityDetails avs = (ChoiceAuthorityDetails) ma;
            return avs.getDetailsInfo(field, key, locale);
        }
        return null;
    }
    
    
    public Set<String> getAuthorities()
    {
        Set<String> set = new HashSet<String>();
        for (String alias : md2authorityname.values())
        {
            set.add(alias);
        }
        return set;
    }

    public List<String> getAuthorityMetadataForAuthority(String authorityName)
    {
        List<String> result = new LinkedList<String>();
        for (String md : md2authorityname.keySet())
        {
            if (md2authorityname.get(md).equalsIgnoreCase(authorityName))
            {
                result.add(md.replaceAll("_", "\\."));
            }
        }
        return result;
    }
    private ChoiceAuthority reloadCache(String fieldKey)
    {
        ChoiceAuthority ma = null;
        if(fieldKey.contains("_authority_")) {
            reloadCache();
            ma = controller.get(fieldKey);
        }
        return ma;
    }
    
    public static void reloadCache() {
        cached = null;
        getManager();
    }

	public String getAuthorityName(String fieldKey) {
		return md2authorityname.get(fieldKey);
	}
	
	public ChoiceAuthority getChoiceAuthority(String metadata) {
		String fieldKey = makeFieldKey(metadata);
		return getChoose(fieldKey);
	}

	public ChoiceAuthority getChoiceAuthority(String schema, String element,
			String qualifier) {
		String fieldKey = makeFieldKey(schema, element, qualifier);
		return getChoose(fieldKey);
	}
	
	private ChoiceAuthority getChoose(String fieldKey) {
		ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null)
        {
            ma = reloadCache(fieldKey);
            if(ma == null) {
                throw new IllegalArgumentException(
                        "No choices plugin was configured for  field \""
                                + fieldKey + "\".");
            }
        }
        return ma;
	}    

}
