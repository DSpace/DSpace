/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.content.Item;
import org.dspace.content.Bitstream;

import java.util.List;
import java.util.Map;

/**
 * The DSpace class for representing the results of a deposit
 * request.  This class can be used to hold all of the relevant
 * components required to later build the SWORD response
 *
 * @author Richard Jones
 *
 */
public class DepositResult
{
    /** the item created during deposit */
    private Item item;

    /** Bitstream created as a result of the deposit */
    private Bitstream originalDeposit;

    private List<Bitstream> derivedResources;

    /** The treatment of the item during deposit */
    private String treatment;

    public Item getItem()
    {
        return item;
    }

    public void setItem(Item item)
    {
        this.item = item;
    }

    public Bitstream getOriginalDeposit()
    {
        return originalDeposit;
    }

    public void setOriginalDeposit(Bitstream originalDeposit)
    {
        this.originalDeposit = originalDeposit;
    }

    public List<Bitstream> getDerivedResources()
    {
        return derivedResources;
    }

    public void setDerivedResources(List<Bitstream> derivedResources)
    {
        this.derivedResources = derivedResources;
    }

    public String getTreatment()
    {
        return treatment;
    }

    public void setTreatment(String treatment)
    {
        this.treatment = treatment;
    }

}
