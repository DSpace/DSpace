/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.bean;

import java.util.Hashtable;

public class TreeKeyMap extends
        Hashtable<Object, Hashtable<Object, Hashtable<Object, Object>>>
{
    public TreeKeyMap()
    {
        super();
    }

    public void addValue(Object key1, Object key2, Object key3, Object value)
    {
        if (this.get(key1) == null)
        {
            this.put(key1, new Hashtable<Object, Hashtable<Object, Object>>());
        }
        Hashtable<Object, Hashtable<Object, Object>> tmpMapKey1 = this
                .get(key1);

        if (tmpMapKey1.get(key2) == null)
        {
            tmpMapKey1.put(key2, new Hashtable<Object, Object>());
        }

        Hashtable<Object, Object> tmpMapKey2 = tmpMapKey1.get(key2);
        tmpMapKey2.put(key3, value);

        tmpMapKey1.put(key2, tmpMapKey2);

        this.put(key1, tmpMapKey1);
    }
}
