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
     * The sword service implementation
     */
    protected SWORDService swordService;

    /**
     * Construct a new Depositor with the given sword service on the given
     * dspace object.  It is anticipated that extensions of this class will
     * specialise in certain kinds of dspace object
     *
     * @param swordService
     * @param dso
     */
    public Depositor(SWORDService swordService, DSpaceObject dso)
    {
        this.swordService = swordService;
    }

    /**
     * Execute the deposit process with the given sword deposit.
     *
     * @param deposit
     * @throws SWORDErrorException
     * @throws DSpaceSWORDException
     */
    public abstract DepositResult doDeposit(Deposit deposit)
            throws SWORDErrorException, DSpaceSWORDException;

    /**
     * Undo any changes to the archive effected by the deposit
     *
     * @param result
     * @throws DSpaceSWORDException
     */
    public abstract void undoDeposit(DepositResult result)
            throws DSpaceSWORDException;
}
