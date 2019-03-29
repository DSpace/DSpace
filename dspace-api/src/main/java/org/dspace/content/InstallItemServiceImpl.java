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
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.embargo.service.EmbargoService;
import org.dspace.event.Event;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.service.IdentifierService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Support to install an Item in the archive.
 * 
 * @author dstuve
 * @version $Revision$
 */
public class InstallItemServiceImpl implements InstallItemService
{

    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;
    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected EmbargoService embargoService;
    @Autowired(required = true)
    protected IdentifierService identifierService;
    @Autowired(required = true)
    protected ItemService itemService;

    protected InstallItemServiceImpl()
    {

    }

    @Override
    public Item installItem(Context c, InProgressSubmission is)
            throws SQLException, AuthorizeException
    {
        return installItem(c, is, null);
    }

    @Override
    public Item installItem(Context c, InProgressSubmission is,
            String suppliedHandle) throws SQLException,
            AuthorizeException
    {
        Item item = is.getItem();
        Collection collection = is.getCollection();
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

        // Finish up / archive the item
        item = finishItem(c, item, is);

        // As this is a BRAND NEW item, as a final step we need to remove the
        // submitter item policies created during deposit and replace them with
        // the default policies from the collection.
        itemService.inheritCollectionDefaultPolicies(c, item, collection);

        return item;
    }

    @Override
    public Item restoreItem(Context c, InProgressSubmission is,
            String suppliedHandle)
        throws SQLException, IOException, AuthorizeException
    {
        Item item = is.getItem();

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
        List<MetadataValue> dateAccessioned = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "date", "accessioned", Item.ANY);
        if (dateAccessioned.isEmpty())
        {
	        itemService.addMetadata(c, item, MetadataSchema.DC_SCHEMA, "date", "accessioned", null, now.toString());
        }
        
        // If issue date is set as "today" (literal string), then set it to current date
        // In the below loop, we temporarily clear all issued dates and re-add, one-by-one,
        // replacing "today" with today's date.
        // NOTE: As of DSpace 4.0, DSpace no longer sets an issue date by default
        List<MetadataValue> currentDateIssued = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);
        itemService.clearMetadata(c, item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);
        for (MetadataValue dcv : currentDateIssued)
        {
            if(dcv.getValue()!=null && dcv.getValue().equalsIgnoreCase("today"))
            {
                DCDate issued = new DCDate(now.getYear(),now.getMonth(),now.getDay(),-1,-1,-1);
                itemService.addMetadata(c, item, dcv.getMetadataField(), dcv.getLanguage(), issued.toString());
            }
            else if(dcv.getValue()!=null)
            {
                itemService.addMetadata(c, item, dcv.getMetadataField(), dcv.getLanguage(), dcv.getValue());
            }
        }
        
        // Record that the item was restored
        String provDescription = "Restored into DSpace on "+ now + " (GMT).";
        itemService.addMetadata(c, item, MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);

        return finishItem(c, item, is);
    }


    protected void populateMetadata(Context c, Item item)
        throws SQLException, AuthorizeException
    {
        // create accession date
        DCDate now = DCDate.getCurrent();
        itemService.addMetadata(c, item, MetadataSchema.DC_SCHEMA, "date", "accessioned", null, now.toString());

        // add date available if not under embargo, otherwise it will
        // be set when the embargo is lifted.
        // this will flush out fatal embargo metadata
        // problems before we set inArchive.
        if (embargoService.getEmbargoTermsAsDate(c, item) == null)
        {
            itemService.addMetadata(c, item, MetadataSchema.DC_SCHEMA, "date", "available", null, now.toString());
        }

        // If issue date is set as "today" (literal string), then set it to current date
        // In the below loop, we temporarily clear all issued dates and re-add, one-by-one,
        // replacing "today" with today's date.
        // NOTE: As of DSpace 4.0, DSpace no longer sets an issue date by default
        List<MetadataValue> currentDateIssued = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);
        itemService.clearMetadata(c, item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);
        for (MetadataValue dcv : currentDateIssued)
        {
            if(dcv.getValue()!=null && dcv.getValue().equalsIgnoreCase("today"))
            {
                DCDate issued = new DCDate(now.getYear(),now.getMonth(),now.getDay(),-1,-1,-1);
                itemService.addMetadata(c, item, dcv.getMetadataField(), dcv.getLanguage(), issued.toString());
            }
            else if(dcv.getValue()!=null)
            {
                itemService.addMetadata(c, item, dcv.getMetadataField(), dcv.getLanguage(), dcv.getValue());
            }
        }

         String provDescription = "Made available in DSpace on " + now
                + " (GMT). " + getBitstreamProvenanceMessage(c, item);

        // If an issue date was passed in and it wasn't set to "today" (literal string)
        // then note this previous issue date in provenance message
        if (!currentDateIssued.isEmpty())
        {
            String previousDateIssued = currentDateIssued.get(0).getValue();
            if(previousDateIssued!=null && !previousDateIssued.equalsIgnoreCase("today"))
            {
                DCDate d = new DCDate(previousDateIssued);
                provDescription = provDescription + "  Previous issue date: "
                        + d.toString();
            }
        }

        // Add provenance description
        itemService.addMetadata(c, item, MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
    }

    /**
     * Final housekeeping when adding a new Item into the archive.
     * This method is used by *both* installItem() and restoreItem(),
     * so all actions here will be run for a newly added item or a restored item.
     *
     * @param c DSpace Context
     * @param item Item in question
     * @param is InProgressSubmission object
     * @return final "archived" Item
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    protected Item finishItem(Context c, Item item, InProgressSubmission is)
        throws SQLException, AuthorizeException
    {
        // create collection2item mapping
        collectionService.addItem(c, is.getCollection(), item);

        // set owning collection
        item.setOwningCollection(is.getCollection());

        // set in_archive=true
        item.setArchived(true);
        
        // save changes ;-)
        itemService.update(c, item);

        // Notify interested parties of newly archived Item
        c.addEvent(new Event(Event.INSTALL, Constants.ITEM, item.getID(),
                item.getHandle(), itemService.getIdentifiers(c, item)));

        // remove in-progress submission
        contentServiceFactory.getInProgressSubmissionService(is).deleteWrapper(c, is);

        // set embargo lift date and take away read access if indicated.
        embargoService.setEmbargo(c, item);

        return item;
    }

    @Override
    public String getBitstreamProvenanceMessage(Context context, Item myitem)
    						throws SQLException
    {
        // Get non-internal format bitstreams
        List<Bitstream> bitstreams = itemService.getNonInternalBitstreams(context, myitem);

        // Create provenance description
        StringBuilder myMessage = new StringBuilder();
        myMessage.append("No. of bitstreams: ").append(bitstreams.size()).append("\n");

        // Add sizes and checksums of bitstreams
        for (Bitstream bitstream : bitstreams)
        {
            myMessage.append(bitstream.getName()).append(": ")
                    .append(bitstream.getSizeBytes()).append(" bytes, checksum: ")
                    .append(bitstream.getChecksum()).append(" (")
                    .append(bitstream.getChecksumAlgorithm()).append(")\n");
        }

        return myMessage.toString();
    }
}
