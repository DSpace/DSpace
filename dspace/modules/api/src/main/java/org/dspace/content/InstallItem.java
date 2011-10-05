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
import java.util.Calendar;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.embargo.EmbargoManager;
import org.dspace.event.Event;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

/**
 * Support to install an Item in the archive.
 * 
 * @author dstuve
 * @version $Revision: 5844 $
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
        String handle;

        if (item.getHandle() == null) {
            // if no previous handle supplied, create one
            if (suppliedHandle == null) {
                // create a new handle for this item
                handle = HandleManager.createHandle(c, item);
            } else {
                // assign the supplied handle to this item
                handle = HandleManager.createHandle(c, item, suppliedHandle);
            }
        } else {
            handle = item.getHandle();
        }

        populateHandleMetadata(item, handle);

        // this is really just to flush out fatal embargo metadata
        // problems before we set inArchive.
        //DCDate liftDate = EmbargoManager.getEmbargoDate(c, item);
        DCDate liftDate = getLiftDate(item);

        populateMetadata(c, item, liftDate);

        return finishItem(c, item, is, liftDate);

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
        String handle;

        // if no handle supplied
        if (suppliedHandle == null)
        {
            // create a new handle for this item
            handle = HandleManager.createHandle(c, item);
            //only populate handle metadata for new handles
            // (existing handles should already be in the metadata -- as it was restored by ingest process)
            populateHandleMetadata(item, handle);
        }
        else
        {
            // assign the supplied handle to this item
            handle = HandleManager.createHandle(c, item, suppliedHandle);
        }

        //NOTE: this method specifically skips over "populateMetadata()"
        // As this is a "restore" all the metadata should have already been restored

        //@TODO: Do we actually want a "Restored on ..." provenance message?  Or perhaps kick off an event?

        return finishItem(c, item, is, null);
    }

    private static void populateHandleMetadata(Item item, String handle)
        throws SQLException, IOException, AuthorizeException
    {
        String handleref = HandleManager.getCanonicalForm(handle);

        // Add handle as identifier.uri DC value.
        // First check that identifier dosn't already exist.
        boolean identifierExists = false;
        DCValue[] identifiers = item.getDC("identifier", "uri", Item.ANY);
        for (DCValue identifier : identifiers)
        {
        	if (handleref.equals(identifier.value))
            {
        		identifierExists = true;
            }
        }
        if (!identifierExists)
        {
        	item.addDC("identifier", "uri", null, handleref);
        }
    }

    // fill in metadata needed by new Item.
    private static void populateMetadata(Context c, Item item, DCDate embargoLiftDate)
        throws SQLException, IOException, AuthorizeException
    {
        // create accession date
        DCDate now = DCDate.getCurrent();
        item.addDC("date", "accessioned", null, now.toString());

        // add date available if not under embargo, otherwise it will
        // be set when the embargo is lifted.
        if (embargoLiftDate == null)
        {
            item.addDC("date", "available", null, now.toString());
        }

        // create issue date if not present
        DCValue[] currentDateIssued = item.getDC("date", "issued", Item.ANY);

        if (currentDateIssued.length == 0)
        {
            DCDate issued = new DCDate(now.getYear(),now.getMonth(),now.getDay(),-1,-1,-1);
            item.addDC("date", "issued", null, issued.toString());
        }

         String provDescription = "Made available in DSpace on " + now
                + " (GMT). " + getBitstreamProvenanceMessage(item);

        if (currentDateIssued.length != 0)
        {
            DCDate d = new DCDate(currentDateIssued[0].value);
            provDescription = provDescription + "  Previous issue date: "
                    + d.toString();
        }

        // Add provenance description
        item.addDC("description", "provenance", "en", provDescription);
    }

    // final housekeeping when adding new Item to archive
    // common between installing and "restoring" items.
    private static Item finishItem(Context c, Item item, InProgressSubmission is, DCDate embargoLiftDate)
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
                item.getHandle()));

        // remove in-progress submission
        is.deleteWrapper();

        // remove the item's policies and replace them with
        // the defaults from the collection
        item.inheritCollectionDefaultPolicies(is.getCollection());

        // set embargo lift date and take away read access if indicated.
        if (embargoLiftDate != null)
        {
            EmbargoManager.setEmbargo(c, item, embargoLiftDate);
        }

        return item;
    }


private static void registerDOI(Context c, Item item) throws AuthorizeException, SQLException, IOException {
        DSpace dspace = new DSpace();
        IdentifierService service = new DSpace().getSingletonService(IdentifierService.class);
        try {
            service.register(c, item);
        } catch (IdentifierException e) {
            throw new IOException(e);
        }
    }

    private static DCDate getLiftDate(Item item) {
        DCDate liftDate = null;
        //Get our embargo date !
        String embargoType = ConfigurationManager.getProperty("embargo.field.type");
        String embargoLoc = ConfigurationManager.getProperty("embargo.field.terms");
        if(embargoLoc != null && embargoType != null){
            DCValue embargoTypeValues[] = item.getMetadata(embargoType);
            if(0 < embargoTypeValues.length){
                //Since we have retrieved our type values remove them from the item, they are no longer needed
                DCValue dcEmbargoType = embargoTypeValues[0];
//                item.clearMetadata(dcEmbargoType.schema, dcEmbargoType.element, dcEmbargoType.qualifier, Item.ANY);

                Calendar embargoDate = Calendar.getInstance();

                // Time to resolve our type
                if(dcEmbargoType.value.equals("none")){
                    //No embargo, so clear the date
                    embargoDate = null;
                }else
                if(dcEmbargoType.value.equals("oneyear") || dcEmbargoType.value.equals("custom") || dcEmbargoType.value.equals("untilArticleAppears")){
                    //We have an embargo that (initially) lasts forever, which will be updated when the publication appears
                    embargoDate.set(Calendar.YEAR, 9999);
                    embargoDate.set(Calendar.MONTH, 0);
                    embargoDate.set(Calendar.DATE, 1);
                }


                if(embargoDate != null){
                    DCDate embargoDcDate = new DCDate(embargoDate.getTime());
                    liftDate = embargoDcDate;
//                    EmbargoManager.setEmbargo(c, item, embargoDcDate);
                }
            }else{
                //No embargo type found, make sure that we are compatible with old values
                DCValue embargoDateValues[] = item.getMetadata(embargoLoc);
                //Check if we have an embargo
                if(0 < embargoDateValues.length && !embargoDateValues[0].value.equals("none")){
                    //We have a year embargo so add it
                    //Get an embargo of one year
                    Calendar embargoDate = Calendar.getInstance();
                    //Add one year
                    embargoDate.add(Calendar.YEAR, 1);
                    //Store it
                    //Create and store our embargodate
                    DCDate embargoDcDate = new DCDate(embargoDate.getTime());
                    liftDate = embargoDcDate;
//                    EmbargoManager.setEmbargo(c, item, embargoDcDate);
                }
            }


        }
        //Clear any remaining submit value concerning the showing of an embargo
        item.clearMetadata("internal", "submit", "showEmbargo", Item.ANY);
        return liftDate;
    }


      /**
     * Generate provenance-worthy description of the bitstreams contained in an
     * item.
     *
     * @param myitem  the item generate description for
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
