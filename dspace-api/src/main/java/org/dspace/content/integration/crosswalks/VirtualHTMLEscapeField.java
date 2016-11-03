/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;

/**
 * Effettua l'escape html di un altro metadato
 * 
 * @author bollini
 */
public class VirtualHTMLEscapeField implements VirtualFieldDisseminator,
        VirtualFieldIngester
{
    public String[] getMetadata(Item item, Map<String, String> fieldCache,
            String fieldName)
    {
        // Check to see if the virtual field is already in the cache
        // - processing is quite intensive, so we generate all the values on
        // first request
        if (fieldCache.containsKey(fieldName))
            return fieldCache.get(fieldName).split("\\|");

        String[] virtualFieldName = fieldName.split("\\.", 3);

        // "virtual.escape.dc.identifier.citation"
        String metadata = virtualFieldName[2];

        // Get the metadatavalue
        Metadatum[] dcvs = item.getMetadataByMetadataString(metadata);

        if (dcvs != null && dcvs.length > 0)
        {
            StringBuffer sb = new StringBuffer();
            boolean start = true;
            for (Metadatum dc : dcvs)
            {
                if (!start)
                {
                    sb.append("|");
                }
                sb.append(StringEscapeUtils.escapeHtml(dc.value));
                start = false;
            }
            fieldCache.put("virtual.escape." + metadata, sb.toString());

            return new String[] { sb.toString() };
        }

        return null;
    }

    public boolean addMetadata(Item item, Map<String, String> fieldCache,
            String fieldName, String value)
    {
        // NOOP - we won't add any metadata yet, we'll pick it up when we
        // finalise the item
        return true;
    }

    public boolean finalizeItem(Item item, Map<String, String> fieldCache)
    {
        return false;
    }
}