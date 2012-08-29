package org.dspace.app.importer;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;

public class AllKeywordsSanitizeMetadata implements SanitizeImportedMetadata
{
    public void sanitize(Context context, Item item, String schema, String element,
            String qualifier)
    {
        Set<String> uniqueSet = new HashSet<String>();
        DCValue[] dcvalues = item.getMetadata(schema, element, qualifier, Item.ANY);
        
        for (DCValue dcvalue : dcvalues)
        {
            if (StringUtils.isNotBlank(dcvalue.value))
            {
                uniqueSet.add(dcvalue.value.trim());
            }
        }
        
        item.clearMetadata(schema, element, qualifier, Item.ANY);
        StringBuffer sb = new StringBuffer();
        for (String v : uniqueSet)
        {
            sb.append(v);
            sb.append("; ");
        }
        
        if (sb.length() > 2)
        {
            item.addMetadata(schema, element, qualifier, "en", sb.substring(0, sb.length()-2));
        }
    }
}
