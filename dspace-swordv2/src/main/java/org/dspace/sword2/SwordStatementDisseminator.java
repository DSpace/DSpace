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
import org.swordapp.server.Statement;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public interface SwordStatementDisseminator
{
    public Statement disseminate(Context context, Item item)
            throws DSpaceSwordException, SwordError, SwordServerException;
}
