/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.embargo.EmbargoManager;
import org.dspace.event.Event;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

/**
 * Support to install an Item in the archive.
 * 
 * @author dstuve
 * @version $Revision$
 */
public class InstallItem
{
    /**
     * Take an InProgressSubmission and turn it into a fully-archived Item,
     * creating a new Handle.
     * 
     * @param c
     *            DSpace Context
     * @param is
     *            submission to install
     * 
     * @return the fully archived Item
     */
    public static Item installItem(Context c, InProgressSubmission is)
            throws SQLException, IOException, AuthorizeException
    {
        return installItem(c, is, null);
    }

    /**
     * Take an InProgressSubmission and turn it into a fully-archived Item.
     * 
     * @param c  current context
     * @param is
     *            submission to install
     * @param suppliedHandle
     *            the existing Handle to give to the installed item
     * 
     * @return the fully archived Item
     */
    public static Item installItem(Context c, InProgressSubmission is,
            String suppliedHandle) throws SQLException,
            IOException, AuthorizeException
    {
        Item item = is.getItem();
        IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
        try {
            if(suppliedHandle == null)
            {
                identifierService.register(c, item);
            }else{
                identifierService.register(c, item, suppliedHandle);
            }
        } catch (IdentifierException e) {
            throw new RuntimeException("Can't create an Identifier!", e);
        }

        populateMetadata(c, item);

        return finishItem(c, item, is);
    }

    /**
     * Turn an InProgressSubmission into a fully-archived Item, for
     * a "restore" operation such as ingestion of an AIP to recreate an
     * archive.  This does NOT add any descriptive metadata (e.g. for
     * provenance) to preserve the transparency of the ingest.  The
     * ingest mechanism is assumed to have set all relevant technical
     * and administrative metadata fields.
     *
     * @param c  current context
     * @param is
     *            submission to install
     * @param suppliedHandle
     *            the existing Handle to give the installed item, or null
     *            to create a new one.
     *
     * @return the fully archived Item
     */
    public static Item restoreItem(Context c, InProgressSubmission is,
            String suppliedHandle)
        throws SQLException, IOException, AuthorizeException
    {
        Item item = is.getItem();

        IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
        try {
            if(suppliedHandle == null)
            {
                identifierService.register(c, item);
            }else{
                identifierService.register(c, item, suppliedHandle);
            }
        } catch (IdentifierException e) {
            throw new RuntimeException("Can't create an Identifier!");
        }

        // Even though we are restoring an item it may not have the proper dates. So let's
        // double check its associated date(s)
        DCDate now = DCDate.getCurrent();
        
        // If the item doesn't have a date.accessioned, set it to today
        Metadatum[] dateAccessioned = item.getDC("date", "accessioned", Item.ANY);
        if (dateAccessioned.length == 0)
        {
	        item.addDC("date", "accessioned", null, now.toString());
        }
        
        // If issue date is set as "today" (literal string), then set it to current date
        // In the below loop, we temporarily clear all issued dates and re-add, one-by-one,
        // replacing "today" with today's date.
        // NOTE: As of DSpace 4.0, DSpace no longer sets an issue date by default
        Metadatum[] currentDateIssued = item.getDC("date", "issued", Item.ANY);
        item.clearDC("date", "issued", Item.ANY);
        for (Metadatum dcv : currentDateIssued)
        {
            if(dcv.value!=null && dcv.value.equalsIgnoreCase("today"))
            {
                DCDate issued = new DCDate(now.getYear(),now.getMonth(),now.getDay(),-1,-1,-1);
                item.addDC(dcv.element, dcv.qualifier, dcv.language, issued.toString());
            }
            else if(dcv.value!=null)
            {
                item.addDC(dcv.element, dcv.qualifier, dcv.language, dcv.value);
            }
        }
        
        // Record that the item was restored
        String provDescription = "Restored into DSpace on "+ now + " (GMT).";
        item.addDC("description", "provenance", "en", provDescription);

        return finishItem(c, item, is);
    }


    private static void populateMetadata(Context c, Item item)
        throws SQLException, IOException, AuthorizeException
    {
        // create accession date
        DCDate now = DCDate.getCurrent();
        item.addDC("date", "accessioned", null, now.toString());

        // add date available if not under embargo, otherwise it will
        // be set when the embargo is lifted.
        // this will flush out fatal embargo metadata
        // problems before we set inArchive.
        if (EmbargoManager.getEmbargoTermsAsDate(c, item) == null)
        {
             item.addDC("date", "available", null, now.toString());
        }

        // If issue date is set as "today" (literal string), then set it to current date
        // In the below loop, we temporarily clear all issued dates and re-add, one-by-one,
        // replacing "today" with today's date.
        // NOTE: As of DSpace 4.0, DSpace no longer sets an issue date by default
        Metadatum[] currentDateIssued = item.getDC("date", "issued", Item.ANY);
        item.clearDC("date", "issued", Item.ANY);
        for (Metadatum dcv : currentDateIssued)
        {
            if(dcv.value!=null && dcv.value.equalsIgnoreCase("today"))
            {
                DCDate issued = new DCDate(now.getYear(),now.getMonth(),now.getDay(),-1,-1,-1);
                item.addDC(dcv.element, dcv.qualifier, dcv.language, issued.toString());
            }
            else if(dcv.value!=null)
            {
                item.addDC(dcv.element, dcv.qualifier, dcv.language, dcv.value);
            }
        }

         String provDescription = "Made available in DSpace on " + now
                + " (GMT). " + getBitstreamProvenanceMessage(item);

        // If an issue date was passed in and it wasn't set to "today" (literal string)
        // then note this previous issue date in provenance message
        if (currentDateIssued.length != 0)
        {
            String previousDateIssued = currentDateIssued[0].value;
            if(previousDateIssued!=null && !previousDateIssued.equalsIgnoreCase("today"))
            {
                DCDate d = new DCDate(previousDateIssued);
                provDescription = provDescription + "  Previous issue date: "
                        + d.toString();
            }
        }

        // Add provenance description
        item.addDC("description", "provenance", "en", provDescription);
    }

    // final housekeeping when adding new Item to archive
    // common between installing and "restoring" items.
    private static Item finishItem(Context c, Item item, InProgressSubmission is)
        throws SQLException, IOException, AuthorizeException
    {
        // create collection2item mapping
        is.getCollection().addItem(item);

        // set owning collection
        item.setOwningCollection(is.getCollection());

        // set in_archive=true
        item.setArchived(true);
        
        // save changes ;-)
        item.update();

        // Notify interested parties of newly archived Item
        c.addEvent(new Event(Event.INSTALL, Constants.ITEM, item.getID(),
                item.getHandle(), item.getIdentifiers(c)));

        // remove in-progress submission
        is.deleteWrapper();

        // remove the item's policies and replace them with
        // the defaults from the collection
        item.inheritCollectionDefaultPolicies(is.getCollection());

        // set embargo lift date and take away read access if indicated.
        EmbargoManager.setEmbargo(c, item);

        return item;
    }

    /**
     * Generate provenance-worthy description of the bitstreams contained in an
     * item.
     * 
     * @param myitem  the item to generate description for
     * 
     * @return provenance description
     */
    public static String getBitstreamProvenanceMessage(Item myitem)
    						throws SQLException
    {
        // Get non-internal format bitstreams
        Bitstream[] bitstreams = myitem.getNonInternalBitstreams();

        // Create provenance description
        StringBuilder myMessage = new StringBuilder();
        myMessage.append("No. of bitstreams: ").append(bitstreams.length).append("\n");

        // Add sizes and checksums of bitstreams
        for (int j = 0; j < bitstreams.length; j++)
        {
            myMessage.append(bitstreams[j].getName()).append(": ")
                    .append(bitstreams[j].getSize()).append(" bytes, checksum: ")
                    .append(bitstreams[j].getChecksum()).append(" (")
                    .append(bitstreams[j].getChecksumAlgorithm()).append(")\n");
        }

        return myMessage.toString();
    }
}
