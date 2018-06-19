/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class MetadataFieldCache
{
    private static Logger log = LogManager
            .getLogger(MetadataFieldCache.class);

    private Map<String, Integer> fields;

    public MetadataFieldCache()
    {
        fields = new HashMap<String, Integer>();
    }

    public boolean hasField(String field)
    {
        return fields.containsKey(field);
    }

    public int getField(String field)
    {
        return fields.get(field).intValue();
    }

    public void add(String field, int id)
    {
        fields.put(field, new Integer(id));
    }
}
