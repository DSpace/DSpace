/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public interface SwordEntryIngester
{
    DepositResult ingest(Context context, Deposit deposit, DSpaceObject target,
            VerboseDescription verboseDescription)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException;

    DepositResult ingest(Context context, Deposit deposit, DSpaceObject target,
            VerboseDescription verboseDescription, DepositResult result,
            boolean replace)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException;
}
