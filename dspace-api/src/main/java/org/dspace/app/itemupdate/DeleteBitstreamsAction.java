/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.Context;

// UM Change
import org.dspace.content.service.ItemService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.apache.logging.log4j.Logger;

/**
 * Action to delete bitstreams
 *
 * Undo not supported for this UpdateAction
 *
 * Derivatives of the bitstream to be deleted are not also deleted
 */
public class DeleteBitstreamsAction extends UpdateBitstreamsAction {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(DeleteBitstreamsAction.class);

    /**
     * Delete bitstream from item
     *
     * @param context      DSpace Context
     * @param itarch       item archive
     * @param isTest       test flag
     * @param suppressUndo undo flag
     * @throws IOException              if IO error
     * @throws IllegalArgumentException if arg exception
     * @throws SQLException             if database error
     * @throws AuthorizeException       if authorization error
     * @throws ParseException           if parse error
     */
    @Override
    public void execute(Context context, ItemArchive itarch, boolean isTest,
                        boolean suppressUndo) throws IllegalArgumentException, IOException,
        SQLException, AuthorizeException, ParseException {

        Item item = null;            
        File f = new File(itarch.getDirectory(), ItemUpdate.DELETE_CONTENTS_FILE);
        if (!f.exists()) {
            ItemUpdate.pr("Warning: Delete_contents file for item " + itarch.getDirectoryName() + " not found.");
        } else {
            List<String> list = MetadataUtilities.readDeleteContentsFile(f);
            if (list.isEmpty()) {
                ItemUpdate.pr("Warning: empty delete_contents file for item " + itarch.getDirectoryName());
            } else {
                for (String id : list) {
                    try {
                        Bitstream bs = bitstreamService.findByIdOrLegacyId(context, id);
                        if (bs == null) {
                            ItemUpdate.pr("Bitstream not found by id: " + id);
                        } else {
                            List<Bundle> bundles = bs.getBundles();
                            for (Bundle b : bundles) {
                                if (isTest) {
                                    ItemUpdate.pr("Delete bitstream with id = " + id);
                                } else {
                                    bundleService.removeBitstream(context, b, bs);
                                    ItemUpdate.pr("Deleted bitstream with id = " + id);

                                }
                            }

                            item = bundles.iterator().next().getItems().iterator().next();
                            if (alterProvenance) {
                                DtoMetadata dtom = DtoMetadata.create("dc.description.provenance", "en", "");

                                String append = "Bitstream " + bs.getName() + " deleted on " + DCDate
                                    .getCurrent() + "; ";
                                //Item item = bundles.iterator().next().getItems().iterator().next();
                                ItemUpdate.pr("Append provenance with: " + append);

                                if (!isTest) {
                                    MetadataUtilities.appendMetadata(context, item, dtom, false, append);
                                }
                            }
                        }
                    } catch (SQLException e) {
                        ItemUpdate.pr("Error finding bitstream from id: " + id + " : " + e.toString());
                    }
                }
            }
        }

       // Go ahead and update the bitstreamurl information.
       if(item != null){
               itemService.clearMetadata(context, item, MetadataSchemaEnum.DC.getName(), "description", "bitstreamurl", Item.ANY);


                String handle = item.getHandle();

                List<Bundle> bundlesList = item.getBundles ("ORIGINAL");
                Bundle[] bunds = bundlesList.toArray(new Bundle[bundlesList.size()]);
                if ( bunds.length != 0 )
                {
                    if (bunds[0] != null)
                    {
                         List<Bitstream> bitsList = bunds[0].getBitstreams ();
                         Bitstream[] bits = bitsList.toArray(new Bitstream[bitsList.size()]);

                        for (int i = 0; (i < bits.length); i++)
                        {
                            String sequence_id =  Integer.toString(bits[i].getSequenceID());
                            String filename =  bits[i].getName();
                            String bit_uuid =  bits[i].getID().toString();

                            String biturl = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.ui.url")  + "/bitstreams/" + bit_uuid + "/download";
                            itemService.addMetadata(context, item, MetadataSchemaEnum.DC.getName(), "description", "bitstreamurl", "en", biturl);


                            // //Add the link to image class if item has jpeg
                            // Collection owningCollection = item.getOwningCollection();
                            // String format = bits[i].getUserFormatDescription();

                            // if ( IsAJpegCollection(handle) && format.equals("JPEG 2000 Pt. 1") )
                            // {
                            //     String internal_id =  bits[i].getInternalId();
                            //     String image_url = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("image.url") + "?c=deepblueic;evl=full-image;quality=4;view=entry;subview=detail;cc=deepblueic;entryid=" + handle + ";viewid=" + internal_id + ";start=;resnum=";
                            //     //String image_url = ConfigurationManager.getProperty("image.url") + "?c=deepblueic;evl=full-image;quality=4;view=entry;subview=detail;cc=deepblueic;entryid=" + handle + ";viewid=" + internal_id + ";start=;resnum=";
                            //     //item.addDC("identifier", "imageclass", null, image_url);
                            //     itemService.addMetadata(context, item, MetadataSchema.DC_SCHEMA, "identifier", "imageclass", "en", image_url);
                            // }

                        }
                    }
                }
                itemService.update(context, item);
            }
            // End change.
    }

}
