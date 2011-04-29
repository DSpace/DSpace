/*
 * ChoiceAuthorityManager.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content.authority;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;

import org.apache.log4j.Logger;

import org.dspace.content.MetadataField;
import org.dspace.content.Collection;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.PluginManager;

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
public class ChoiceAuthorityManager
{
    private static Logger log = Logger.getLogger(ChoiceAuthorityManager.class);

    private static ChoiceAuthorityManager cached = null;

    // map of field key to authority plugin
    private Map<String,ChoiceAuthority> controller = new HashMap<String,ChoiceAuthority>();

    // map of field key to presentation type
    private Map<String,String> presentation = new HashMap<String,String>();

    // map of field key to closed value
    private Map<String,Boolean> closed = new HashMap<String,Boolean>();

    private ChoiceAuthorityManager()
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
                    // MetadataField.findByElement to maek sure it's a real field.
                     
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
                    closed.put(fkey, new Boolean(ConfigurationManager.getBooleanProperty(key)));
                }
                else
                    log.error("Illegal configuration property: "+key);
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
        int dot = field.indexOf(".");
        if (dot < 0)
            return null;
        String schema = field.substring(0, dot);
        String element = field.substring(dot+1);
        String qualifier = null;
        dot = element.indexOf(".");
        if (dot >= 0)
        {
            qualifier = element.substring(dot+1);
            element = element.substring(0, dot);
        }
        return makeFieldKey(schema, element, qualifier);
    }

    /**
     *  Wrapper that calls getMatches method of the plugin corresponding to
     *  the metadata field defined by schema,element,qualifier.
     *
     * @see ChoiceAuthority.getMatches
     * @param schema schema of metadata field
     * @param element element of metadata field
     * @param qualifier qualifier of metadata field
     * @param text user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param start choice at which to start, 0 is first.
     * @param limit maximum number of choices to return, 0 for no limit.
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getMatches(String schema, String element, String qualifier,
                                              String query, int collection, int start, int limit, String locale)
    {
        return getMatches(makeFieldKey(schema, element, qualifier), query, collection, start, limit, locale);
    }

    /**
     *  Wrapper calls getMatches method of the plugin corresponding to
     *  the metadata field defined by single field key.
     *
     * @see ChoiceAuthority.getMatches
     * @param fieldKey single string identifying metadata field
     * @param text user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param start choice at which to start, 0 is first.
     * @param limit maximum number of choices to return, 0 for no limit.
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getMatches(String fieldKey, String query, int collection, int start, int limit, String locale)
    {
        ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null)
            throw new IllegalArgumentException("No choices plugin was configured for  field \""+fieldKey+"\".");
        return ma.getMatches(query, collection, start, limit, locale);
    }

    /**
     *  Wrapper that calls getBestMatch method of the plugin corresponding to
     *  the metadata field defined by single field key.
     *
     * @see ChoiceAuthority.getBestMatch
     * @param fieldKey single string identifying metadata field
     * @param text user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null) with 1 or 0 values.
     */
    public Choices getBestMatch(String fieldKey, String query, int collection, String locale)
    {
        ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null)
            throw new IllegalArgumentException("No choices plugin was configured for  field \""+fieldKey+"\".");
        return ma.getBestMatch(query, collection, locale);
    }

    /**
     *  Wrapper that calls getLabel method of the plugin corresponding to
     *  the metadata field defined by schema,element,qualifier.
     */
    public String getLabel(String schema, String element, String qualifier,
                                                      String authKey, String locale)
    {
        return getLabel(makeFieldKey(schema, element, qualifier), authKey, locale);
    }

    /**
     *  Wrapper that calls getLabel method of the plugin corresponding to
     *  the metadata field defined by single field key.
     */
    public String getLabel(String fieldKey, String authKey, String locale)
    {
        ChoiceAuthority ma = controller.get(fieldKey);
        if (ma == null)
            throw new IllegalArgumentException("No choices plugin was configured for  field \""+fieldKey+"\".");
        return ma.getLabel(authKey, locale);
    }

    /**
     * Predicate, is there a Choices configuration of any kind for the
     * given metadata field?
     * @return true if choices are configured for this field.
    */
    public boolean isChoicesConfigured(String fieldKey)
    {
        return controller.containsKey(fieldKey);
    }

    /**
     * Get the presentation keyword (should be "lookup", "select" or "suggest", but this
     * is an informal convention so it can be easily extended) for this field.
     *
     * @return configured presentation type for this field, or null if none found
     */
    public String getPresentation(String fieldKey)
    {
        return presentation.get(fieldKey);
    }

    /**
     * Get the configured "closed" value for this field.
     *
     * @return true if choices are closed for this field.
    */
    public boolean isClosed(String fieldKey)
    {
        return closed.containsKey(fieldKey) ? closed.get(fieldKey).booleanValue() : false;
    }

    /**
     * Construct a single key from the tuple of schema/element/qualifier
     * that describes a metadata field.  Punt to the function we use for
     * submission UI input forms, for now.
     */
    public static String makeFieldKey(String schema, String element, String qualifier)
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
    public List<String> getVariants(String schema, String element, String qualifier,
            String authorityKey, String language)
    {
        ChoiceAuthority ma = controller.get(makeFieldKey(schema, element, qualifier));
        if (ma instanceof AuthorityVariantsSupport)
        {
            AuthorityVariantsSupport avs = (AuthorityVariantsSupport) ma;
            return avs.getVariants(authorityKey, language);
        }
        return null;
    }
}
