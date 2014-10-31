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

import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.core.Context;

public class MetadataValueISSNExtractor implements ISSNItemExtractor
{
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
            Metadatum[] dcvalues = item.getMetadataByMetadataString(metadata);
            for (Metadatum dcvalue : dcvalues)
            {
                values.add(dcvalue.value);
            }
        }
        return values;
    }
}
