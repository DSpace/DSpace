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

/**
 * Interface behind which can be implemented ingest mechanisms
 * for SWORD deposit requests.  Instances of this class should
 * be obtained via the SwordIngesterFactory class.
 *
 * @author Richard Jones
 *
 */
public interface SwordContentIngester
{
    /**
     * Ingest the package as described in the given Deposit object
     * within the given DSpace Context
     *
     * @param context
     *     The relevant DSpace Context.
     * @param deposit
     *     the original deposit request
     * @param target
     *     target DSpace object
     * @param verboseDescription
     *     The description.
     * @return the result of the deposit
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     * @throws SwordError
     *     SWORD error per SWORD spec
     * @throws SwordAuthException
     *     thrown if unable to authenticate
     * @throws SwordServerException
     *     thrown by SWORD server implementation
     */
    DepositResult ingest(Context context, Deposit deposit, DSpaceObject target,
            VerboseDescription verboseDescription)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException;

    DepositResult ingest(Context context, Deposit deposit, DSpaceObject target,
            VerboseDescription verboseDescription, DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException;
}
