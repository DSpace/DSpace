/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.submit;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.MetadataValue;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataValueISSNExtractor implements ISSNItemExtractor
{
    @Autowired(required = true)
    public ItemService itemService;

    private List<String> metadataList;

    public void setMetadataList(List<String> metadataList)
    {
        this.metadataList = metadataList;
    }

    @Override
    public List<String> getISSNs(Context context, Item item)
    {
        List<String> values = new ArrayList<String>();
        for (String metadata : metadataList)
        {
            List<MetadataValue> dcvalues = itemService.getMetadataByMetadataString(item, metadata);
            for (MetadataValue dcvalue : dcvalues)
            {
                values.add(dcvalue.getValue());
            }
        }
        return values;
    }
}
