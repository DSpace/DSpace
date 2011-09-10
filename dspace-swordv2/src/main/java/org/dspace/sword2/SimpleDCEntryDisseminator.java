/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SimpleDCEntryDisseminator implements SwordEntryDisseminator
{
    private Map<String, String> dcMap;

    public SimpleDCEntryDisseminator()
    {
        // we should load our DC map from configuration
        this.dcMap = new HashMap<String, String>();
        Properties props = ConfigurationManager.getProperties();
        for (Object key : props.keySet())
        {
            String keyString = (String) key;
            if (keyString.startsWith("sword2.simpledc."))
            {
                String k = keyString.substring("sword2.simpledc.".length());
                String v = (String) props.get(key);
                this.dcMap.put(k, v);
            }
        }
    }

    public DepositReceipt disseminate(Context context, Item item, DepositReceipt receipt)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        DCValue[] all = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        for (DCValue dcv : all)
        {
            String valueMatch = dcv.schema + "." + dcv.element;
            if (dcv.qualifier != null)
            {
                valueMatch += "." + dcv.qualifier;
            }
             for (String key : this.dcMap.keySet())
             {
                 String value = this.dcMap.get(key);
                 if (valueMatch.equals(value))
                 {
                     receipt.addDublinCore(key, dcv.value);
                     if (key.equals("title"))
                     {
                         receipt.getWrappedEntry().setTitle(dcv.value);
                     }
                     if (key.equals("abstract"))
                     {
                         receipt.getWrappedEntry().setSummary(dcv.value);
                     }
                 }
             }
        }

        return receipt;
    }
}
