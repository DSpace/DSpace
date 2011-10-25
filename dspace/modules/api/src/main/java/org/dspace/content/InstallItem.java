/*
 * InstallItem.java
 *
 * Version: $Revision: 4277 $
 *
 * Date: $Date: 2009-09-22 23:27:54 +0200 (di, 22 sep 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.embargo.EmbargoManager;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

/**
 * Support to install item in the archive
 *
 * @author dstuve
 * @version $Revision: 4277 $
 *
 * The class has been altered to support NESCents embargo functionality and the identifier services
 */
public class InstallItem
{
    /**
     * Take an InProgressSubmission and turn it into a fully-archived Item,
     * creating a new Handle
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
     *            the existing Handle to give the installed item
     *
     * @return the fully archived Item
     */
    public static Item installItem(Context c, InProgressSubmission is,
            String suppliedHandle) throws SQLException,
            IOException, AuthorizeException
    {
        Item item = is.getItem();
        String handle;

        // this is really just to flush out fatal embargo metadata
        // problems before we set inArchive.
//        DCDate liftDate = EmbargoManager.getEmbargoDate(c, item);

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
                    DCDate embargoDcDate = new DCDate(embargoDate.get(Calendar.YEAR), embargoDate.get(Calendar.MONTH) + 1, embargoDate.get(Calendar.DATE), -1, -1, -1);
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
                    DCDate embargoDcDate = new DCDate(embargoDate.get(Calendar.YEAR), embargoDate.get(Calendar.MONTH) + 1, embargoDate.get(Calendar.DATE), -1, -1, -1);
                    liftDate = embargoDcDate;
//                    EmbargoManager.setEmbargo(c, item, embargoDcDate);
                }
            }


        }
        //Clear any remaining submit value concerning the showing of an embargo
        item.clearMetadata("internal", "submit", "showEmbargo", org.dspace.content.Item.ANY);



        // create accession date
        DCDate now = DCDate.getCurrent();
        item.addDC("date", "accessioned", null, now.toString());

        // add date available if not under embargo, otherwise it will
        // be set when the embargo is lifted.
        if (liftDate == null)
            item.addDC("date", "available", null, now.toString());

        // create issue date if not present
        DCValue[] currentDateIssued = item.getDC("date", "issued", Item.ANY);

        if (currentDateIssued.length == 0)
        {
            DCDate issued = new DCDate(now.getYear(),now.getMonth(),now.getDay(),-1,-1,-1);
            item.addDC("date", "issued", null, issued.toString());
        }

        if(item.getHandle() == null)
        {
        	// if no previous handle supplied, create one
        	if (suppliedHandle == null)
        	{
            	// create handle
            	handle = HandleManager.createHandle(c, item);
        	}
        	else
        	{
            	handle = HandleManager.createHandle(c, item, suppliedHandle);
        	}
		}
		else
		{
			handle = item.getHandle();
		}

        String handleref = HandleManager.getCanonicalForm(handle);

        // Add handle as identifier.uri DC value, first check that identifier dosn't allready exist
        boolean identifierExists = false;
        DCValue[] identifiers = item.getDC("identifier", "uri", Item.ANY);
        for (DCValue identifier : identifiers)
        	if (handleref.equals(identifier.value))
        		identifierExists = true;
        if (!identifierExists)
        	item.addDC("identifier", "uri", null, handleref);

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

      	//Check if we are a part of a publication
        /*
        Item publication = DryadWorkflowUtils.getDataPackage(c, item);
        if(publication != null){
            //We are a part of a publication
            String currentItemUrl = HandleManager.resolveToURL(c, item.getHandle());
            String doi = null;
            DCValue[] doiMetadata = item.getMetadata(MetadataSchema.DC_SCHEMA, "identifier", null, Item.ANY);
            if(0 < doiMetadata.length)
                doi = doiMetadata[0].value;

            boolean alreadyAPart = false;
            //Check if we already hold a reference to this item.
            DCValue[] parts = publication.getMetadata(MetadataSchema.DC_SCHEMA, "relation", "haspart", Item.ANY);
            for (DCValue part : parts) {
                if(part.value.equals(currentItemUrl) || part.value.equals(doi))
                    alreadyAPart = true;
            }

            //Just make sure that we do not add the same thing twice !
            if(!alreadyAPart){
                //Make sure the publication knows about the newly added dataset
                publication.addMetadata(MetadataSchema.DC_SCHEMA, "relation", "haspart", null, HandleManager.resolveToURL(c, item.getHandle()));
                publication.update();
            }
        }
        */
        DSpace dspace = new DSpace();
        IdentifierService service = new DSpace().getSingletonService(IdentifierService.class);
        try {
            service.register(c, item);
        } catch (IdentifierException e) {
            throw new IOException(e);
        }

        // create collection2item mapping
        is.getCollection().addItem(item);

        // set owning collection
        item.setOwningCollection(is.getCollection());

        // set in_archive=true
        item.setArchived(true);

        // save changes ;-)
        item.update();

        // remove in-progress submission
        is.deleteWrapper();

        // remove the item's policies and replace them with
        // the defaults from the collection
        item.inheritCollectionDefaultPolicies(is.getCollection());

        // set embargo lift date and take away read access if indicated.
        if (liftDate != null)
            EmbargoManager.setEmbargo(c, item, liftDate);

        return item;
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
        String mymessage = "No. of bitstreams: " + bitstreams.length + "\n";

        // Add sizes and checksums of bitstreams
        for (int j = 0; j < bitstreams.length; j++)
        {
            mymessage = mymessage + bitstreams[j].getName() + ": "
                    + bitstreams[j].getSize() + " bytes, checksum: "
                    + bitstreams[j].getChecksum() + " ("
                    + bitstreams[j].getChecksumAlgorithm() + ")\n";
        }

        return mymessage;
    }
}