/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import java.util.Map;

public class SimpleDCEntryDisseminator extends AbstractSimpleDC
        implements SwordEntryDisseminator
{
    public SimpleDCEntryDisseminator()
    {
    }

    public DepositReceipt disseminate(Context context, Item item,
            DepositReceipt receipt)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        SimpleDCMetadata md = this.getMetadata(item);

        Map<String, String> dc = md.getDublinCore();
        for (String element : dc.keySet())
        {
            String value = dc.get(element);
            receipt.addDublinCore(element, value);
        }

        Map<String, String> atom = md.getAtom();
        for (String element : atom.keySet())
        {
            String value = atom.get(element);
            if ("author".equals(element))
            {
                receipt.getWrappedEntry().addAuthor(value);
            }
            else if ("published".equals(element))
            {
                receipt.getWrappedEntry().setPublished(value);
            }
            else if ("rights".equals(element))
            {
                receipt.getWrappedEntry().setRights(value);
            }
            else if ("summary".equals(element))
            {
                receipt.getWrappedEntry().setSummary(value);
            }
            else if ("title".equals(element))
            {
                receipt.getWrappedEntry().setTitle(value);
            }
            else if ("updated".equals(element))
            {
                receipt.getWrappedEntry().setUpdated(value);
            }
        }

        return receipt;
    }
}
