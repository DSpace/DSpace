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
 * @author Richard Jones
 *
 * Abstract class for depositing content into the archive.
 */
public abstract class Depositor
{
    /**
     * The SWORD service implementation
     */
    protected SWORDService swordService;

    /**
     * Construct a new Depositor with the given SWORD service on the given
     * dspace object.  It is anticipated that extensions of this class will
     * specialise in certain kinds of dspace object
     *
     * @param swordService
     *     SWORD service
     * @param dso
     *     DSpace object
     */
    public Depositor(SWORDService swordService, DSpaceObject dso)
    {
        this.swordService = swordService;
    }

    /**
     * Execute the deposit process with the given SWORD deposit.
     *
     * @param deposit
     *     deposit request
     * @return deposit result
     * @throws SWORDErrorException on generic SWORD exception
     * @throws DSpaceSWORDException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public abstract DepositResult doDeposit(Deposit deposit)
            throws SWORDErrorException, DSpaceSWORDException;

    /**
     * Undo any changes to the archive effected by the deposit
     *
     * @param result
     *     deposit result
     * @throws DSpaceSWORDException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public abstract void undoDeposit(DepositResult result)
            throws DSpaceSWORDException;
}
