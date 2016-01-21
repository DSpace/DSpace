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

public interface SwordEntryDisseminator
{
    public DepositReceipt disseminate(Context context, Item item,
            DepositReceipt receipt)
            throws DSpaceSwordException, SwordError, SwordServerException;
}
