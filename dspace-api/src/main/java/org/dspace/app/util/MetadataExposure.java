/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Enumeration;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Static utility class to manage configuration for exposure (hiding) of
 * certain Item metadata fields.
 *
 * This class answers the question, "is the user allowed to see this
 * metadata field?"  Any external interface (UI, OAI-PMH, etc) that
 * disseminates metadata should consult it before disseminating the value
 * of a metadata field.
 *
 * Since the MetadataExposure.isHidden() method gets called in a lot of inner
 * loops, it is important to implement it efficiently, in both time and
 * memory utilization.  It computes an answer without consuming ANY memory
 * (e.g. it does not build any temporary Strings) and in close to constant
 * time by use of hash tables.  Although most sites will only hide a few
 * fields, we can't predict what the usage will be so it's better to make it
 * scalable.
 *
 * Algorithm is as follows:
 *  1. If a Context is provided and it has a user who is Administrator,
 *     always grant access (return false).
 *  2. Return true if field is on the hidden list, false otherwise.
 *
 * The internal maps are populated from DSpace Configuration at the first
 * call, in case the properties are not available in the static context.
 *
 * Configuration Properties:
 *  ## hide a single metadata field
 *  #metadata.hide.SCHEMA.ELEMENT[.QUALIFIER] = true
 *  # example: dc.type
 *  metadata.hide.dc.type = true
 *  # example: dc.description.provenance
 *  metadata.hide.dc.description.provenance = true
 *
 * @author Larry Stone
 * @version $Revision: 3734 $
 */
public class MetadataExposure
{
    private static Logger log = Logger.getLogger(MetadataExposure.class);

    private static Map<String,Set<String>> hiddenElementSets = null;
    private static Map<String,Map<String,Set<String>>> hiddenElementMaps = null;

    private static final String CONFIG_PREFIX = "metadata.hide.";

    /**
     * Returns whether the given metadata field should be exposed (visible). The metadata field is in the DSpace's DC notation: schema.element.qualifier
     *
     * @param context DSpace context
     * @param schema metadata field schema (namespace), e.g. "dc"
     * @param element metadata field element
     * @param qualifier metadata field qualifier
     *
     * @return true (hidden) or false (exposed)
     * @throws SQLException
     */
    public static boolean isHidden(Context context, String schema, String element, String qualifier)
        throws SQLException
    {
        // the administrator's override
        if (context != null && AuthorizeManager.isAdmin(context))
        {
            return false;
        }

        // for schema.element, just check schema->elementSet
        if (!isInitialized())
        {
            init();
        }

        if (qualifier == null)
        {
            Set<String> elts = hiddenElementSets.get(schema);
            return elts == null ? false : elts.contains(element);
        }

        // for schema.element.qualifier, just schema->eltMap->qualSet
        else
        {
            Map<String,Set<String>> elts = hiddenElementMaps.get(schema);
            if (elts == null)
            {
                return false;
            }
            Set<String> quals = elts.get(element);
            return quals == null ? false : quals.contains(qualifier);
        }
    }

    /**
     * Returns whether the maps from configuration have already been loaded
     * into the hiddenElementSets property.
     *
     * @return true (initialized) or false (not initialized)
     */
    private static boolean isInitialized()
    {
        return hiddenElementSets != null;
    }

    /**
     * Loads maps from configuration unless it's already done.
     * The configuration properties are a map starting with the
     * "metadata.hide." prefix followed by schema, element and
     * qualifier separated by dots and the value is true (hidden)
     * or false (exposed).
     */
    private static synchronized void init()
    {
        if (!isInitialized())
        {
            hiddenElementSets = new HashMap<String,Set<String>>();
            hiddenElementMaps = new HashMap<String,Map<String,Set<String>>>();

            Enumeration pne = ConfigurationManager.propertyNames();
            while (pne.hasMoreElements())
            {
                String key = (String)pne.nextElement();
                if (key.startsWith(CONFIG_PREFIX))
                {
                    String mdField = key.substring(CONFIG_PREFIX.length());
                    String segment[] = mdField.split("\\.", 3);

                    // got schema.element.qualifier
                    if (segment.length == 3)
                    {
                        Map<String,Set<String>> eltMap = hiddenElementMaps.get(segment[0]);
                        if (eltMap == null)
                        {
                            eltMap = new HashMap<String,Set<String>>();
                            hiddenElementMaps.put(segment[0], eltMap);
                        }
                        if (!eltMap.containsKey(segment[1]))
                        {
                            eltMap.put(segment[1], new HashSet<String>());
                        }
                        eltMap.get(segment[1]).add(segment[2]);
                    }

                    // got schema.element
                    else if (segment.length == 2)
                    {
                        if (!hiddenElementSets.containsKey(segment[0]))
                        {
                            hiddenElementSets.put(segment[0], new HashSet<String>());
                        }
                        hiddenElementSets.get(segment[0]).add(segment[1]);
                    }

                    // oops..
                    else
                    {
                        log.warn("Bad format in hidden metadata directive, field=\""+mdField+"\", config property="+key);
                    }
                }
            }
        }
    }
}
