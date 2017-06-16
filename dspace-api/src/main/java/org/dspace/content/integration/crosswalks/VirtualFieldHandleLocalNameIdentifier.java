/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;

/**
 * Implements virtual field processing to build custom identifier
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science dot it)
 */
public class VirtualFieldHandleLocalNameIdentifier
    implements VirtualFieldDisseminator, VirtualFieldIngester
{

    public VirtualFieldHandleLocalNameIdentifier()
    {
    }

    public String[] getMetadata(Item item, Map fieldCache, String fieldName)
    {
        if(fieldCache.containsKey(fieldName))
            return (new String[] {
                (String)fieldCache.get(fieldName)
            });
        String handle = item.getHandle();
        int position = handle.indexOf("/");
        if(StringUtils.isNotBlank(handle))
            fieldCache.put("virtual.handlelocalname", (new StringBuilder(handle.substring(position+1))).toString());
        else
            fieldCache.put("virtual.handlelocalname", (new StringBuilder("ATT-")).append(item.getID()).toString());
        if(fieldCache.containsKey(fieldName))
            return (new String[] {
                (String)fieldCache.get(fieldName)
            });
        else
            return null;
    }

    public boolean addMetadata(Item item, Map fieldCache, String fieldName, String s)
    {
        // NOOP - we won't add any metadata yet, we'll pick it up when we
        // finalise the item
        return true;
    }

    public boolean finalizeItem(Item item, Map fieldCache)
    {
        return false;
    }
}
