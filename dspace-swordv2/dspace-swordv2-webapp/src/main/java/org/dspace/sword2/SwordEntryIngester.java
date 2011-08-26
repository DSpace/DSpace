package org.dspace.sword2;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public interface SwordEntryIngester
{
    DepositResult ingest(Context context, Deposit deposit, DSpaceObject target, VerboseDescription verboseDescription)
			throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException;

    DepositResult ingest(Context context, Deposit deposit, DSpaceObject target, VerboseDescription verboseDescription, DepositResult result, boolean replace)
			throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException;
}
