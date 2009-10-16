/*
 * MetadataAuthorityManager.java
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

import java.util.ArrayList;
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
 * Broker for metadata authority settings configured for each metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 *
 *  # is field authority controlled (i.e. store authority, confidence values)?
 *  authority.controlled.<FIELD> = true
 *
 *  # is field required to have an authority value, or may it be empty?
 *  # default is false.
 *  authority.required.<FIELD> = true | false
 *
 *  # default value of minimum confidence level for ALL fields - must be
 *  # symbolic confidence level, see org.dspace.content.authority.Choices
 *  authority.minconfidence = uncertain
 *
 *  # minimum confidence level for this field
 *  authority.minconfidence.SCHEMA.ELEMENT.QUALIFIER = SYMBOL
 *    e.g.
 *  authority.minconfidence.dc.contributor.author = accepted
 *
 * NOTE: There is *expected* to be a "choices" (see ChoiceAuthorityManager)
 * configuration for each authority-controlled field.
 *
 * @see ChoiceAuthorityManager
 * @see Choices
 * @author Larry Stone
 */
public class MetadataAuthorityManager
{
    private static Logger log = Logger.getLogger(MetadataAuthorityManager.class);

    private static MetadataAuthorityManager cached = null;

    // map of field key to authority plugin
    private Map<String,Boolean> controlled = new HashMap<String,Boolean>();

    // map of field key to answer of whether field is required to be controlled
    private Map<String,Boolean> isAuthorityRequired = new HashMap<String,Boolean>();

    /**
     * map of field key to answer of which is the min acceptable confidence
     * value for a field with authority
     */
    private Map<String, Integer> minConfidence = new HashMap<String, Integer>();

    /** fallback default value unless authority.minconfidence = X is configured. */
    private int defaultMinConfidence = Choices.CF_ACCEPTED;

    private MetadataAuthorityManager()
    {

        Enumeration pn = ConfigurationManager.propertyNames();
        final String authPrefix = "authority.controlled.";
      property:
        while (pn.hasMoreElements())
        {
            String key = (String)pn.nextElement();
            if (key.startsWith(authPrefix))
            {
                // field is expected to be "schema.element.qualifier"
                String field = key.substring(authPrefix.length());
                int dot = field.indexOf(".");
                if (dot < 0)
                {
                    log.warn("Skipping invalid MetadataAuthority configuration property: "+key+": does not have schema.element.qualifier");
                    continue property;
                }
                String schema = field.substring(0, dot);
                String element = field.substring(dot+1);
                String qualifier = null;
                dot = element.indexOf(".");
                if (dot >= 0)
                {
                    qualifier = element.substring(dot+1);
                    element = element.substring(0, dot);
                }

                String fkey = makeFieldKey(schema, element, qualifier);
                boolean ctl = ConfigurationManager.getBooleanProperty(key, true);
                boolean req = ConfigurationManager.getBooleanProperty("authority.required."+field, false);
                controlled.put(fkey, Boolean.valueOf(ctl));
                isAuthorityRequired.put(fkey, Boolean.valueOf(req));

                // get minConfidence level for this field if any
                int mci = readConfidence("authority.minconfidence."+field);
                if (mci >= Choices.CF_UNSET)
                    minConfidence.put(fkey, new Integer(mci));
                log.debug("Authority Control: For schema="+schema+", elt="+element+", qual="+qualifier+", controlled="+ctl+", required="+req);
            }
        }

        // get default min confidence if any:
        int dmc = readConfidence("authority.minconfidence");
        if (dmc >= Choices.CF_UNSET)
            defaultMinConfidence = dmc;
    }

    private int readConfidence(String key)
    {
        String mc = ConfigurationManager.getProperty(key);
        if (mc != null)
        {
            int mci = Choices.getConfidenceValue(mc.trim(), Choices.CF_UNSET-1);
            if (mci == Choices.CF_UNSET-1)
                {
                log.warn("IGNORING bad value in DSpace Configuration, key="+key+", value="+mc+", must be a valid Authority Confidence keyword.");
                }
            else
            {
                return mci;
            }
        }
        return Choices.CF_UNSET-1;
    }

    // factory method
    public static MetadataAuthorityManager getManager()
    {
        if (cached == null)
        {
            cached = new MetadataAuthorityManager();
        }
        return cached;
    }


    /** Predicate - is field authority-controlled? */
    public boolean isAuthorityControlled(String schema, String element, String qualifier)
    {
        return isAuthorityControlled(makeFieldKey(schema, element, qualifier));
    }

    /** Predicate - is field authority-controlled? */
    public boolean isAuthorityControlled(String fieldKey)
    {
        return controlled.containsKey(fieldKey) && controlled.get(fieldKey).booleanValue();
    }


    /** Predicate - is authority value required for field? */
    public boolean isAuthorityRequired(String schema, String element, String qualifier)
    {
        return isAuthorityRequired(makeFieldKey(schema, element, qualifier));
    }

    /** Predicate - is authority value required for field? */
    public boolean isAuthorityRequired(String fieldKey)
    {
        Boolean result = isAuthorityRequired.get(fieldKey);
        return (result == null) ? false : result.booleanValue();
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
     * Give the minimal level of confidence required to consider valid an authority value
     * for the given metadata.
     * @return the minimal valid level of confidence for the given metadata
     */
    public int getMinConfidence(String schema, String element, String qualifier)
    {
        Integer result = minConfidence.get(makeFieldKey(schema, element, qualifier));
        return result == null ? defaultMinConfidence : result.intValue();
    }

    /**
     * Return the list of metadata field with authority control. The strings
     * are in the form <code>schema.element[.qualifier]</code>
     *
     * @return the list of metadata field with authority control
     */
    public List<String> getAuthorityMetadata() {
        List<String> copy = new ArrayList<String>();
        for (String s : controlled.keySet())
        {
            copy.add(s.replaceAll("_","."));
        }
        return copy;
    }
}
