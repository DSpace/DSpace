/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.dspace.content.DSpaceObject;

import org.purl.sword.base.Deposit;
import org.purl.sword.base.SWORDErrorException;

/**
 * Interface behind which can be implemented ingest mechanisms
 * for SWORD deposit requests.  Instances of this class should
 * be obtained via the SWORDIngesterFactory class.
 *
 * @author Richard Jones
 *
 */
public interface SWORDIngester
{
    /**
     * Ingest the package as described in the given Deposit object
     * within the given DSpace Context
     *
     * @param service
     *     SWORD service
     * @param deposit
     *     deposit request
     * @param target
     *     target DSpace object
     * @return the result of the deposit
     * @throws DSpaceSWORDException
     *     can be thrown by the internals of the DSpace SWORD implementation
     * @throws SWORDErrorException on generic SWORD exception
     */
    DepositResult ingest(SWORDService service, Deposit deposit,
            DSpaceObject target)
            throws DSpaceSWORDException, SWORDErrorException;
}
