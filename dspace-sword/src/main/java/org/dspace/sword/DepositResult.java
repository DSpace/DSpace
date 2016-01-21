/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.dspace.content.Item;
import org.dspace.content.Bitstream;

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
    /** the handle assigned to the item, if available */
    private String handle;

    /** the item created during deposit */
    private Item item;

    /** Bitstream created as a result of the deposit */
    private Bitstream bitstream;

    /** The treatment of the item during deposit */
    private String treatment;

    /** The media linkto the created object */
    private String mediaLink;

    public Bitstream getBitstream()
    {
        return bitstream;
    }

    public void setBitstream(Bitstream bitstream)
    {
        this.bitstream = bitstream;
    }

    public String getTreatment()
    {
        return treatment;
    }

    public void setTreatment(String treatment)
    {
        this.treatment = treatment;
    }

    /**
     * @return the item
     */
    public Item getItem()
    {
        return item;
    }

    /**
     * @param item the item to set
     */
    public void setItem(Item item)
    {
        this.item = item;
    }

    /**
     * @return the handle
     */
    public String getHandle()
    {
        return handle;
    }

    /**
     * @param handle    the item handle
     */
    public void setHandle(String handle)
    {
        this.handle = handle;
    }

    public String getMediaLink()
    {
        return mediaLink;
    }

    public void setMediaLink(String mediaLink)
    {
        this.mediaLink = mediaLink;
    }
}
