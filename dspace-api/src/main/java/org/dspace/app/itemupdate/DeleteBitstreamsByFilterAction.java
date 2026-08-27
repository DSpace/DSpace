/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
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
 * Action to delete bitstreams using a specified filter implementing BitstreamFilter
 * Derivatives for the target bitstreams are not deleted.
 *
 * The dc.description.provenance field is amended to reflect the deletions
 *
 * Note:  Multiple filters are impractical if trying to manage multiple properties files
 * in a commandline environment
 */
public class DeleteBitstreamsByFilterAction extends UpdateBitstreamsAction {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(DeleteBitstreamsByFilterAction.class);

    protected BitstreamFilter filter;
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * Set filter
     *
     * @param filter BitstreamFilter
     */
    public void setBitstreamFilter(BitstreamFilter filter) {
        this.filter = filter;
    }

    /**
     * Get filter
     *
     * @return filter
     */
    public BitstreamFilter getBitstreamFilter() {
        return filter;
    }

    /**
     * Delete bitstream
     *
     * @param context      DSpace Context
     * @param itarch       item archive
     * @param isTest       test flag
     * @param suppressUndo undo flag
     * @throws IOException              if IO error
     * @throws SQLException             if database error
     * @throws AuthorizeException       if authorization error
     * @throws ParseException           if parse error
     * @throws BitstreamFilterException if filter error
     */
    @Override
    public void execute(Context context, ItemArchive itarch, boolean isTest,
                        boolean suppressUndo) throws AuthorizeException,
        BitstreamFilterException, IOException, ParseException, SQLException {

        List<String> deleted = new ArrayList<String>();

        Item item = itarch.getItem();
        List<Bundle> bundles = item.getBundles();

        for (Bundle b : bundles) {
            List<Bitstream> bitstreams = b.getBitstreams();
            String bundleName = b.getName();

            for (Bitstream bs : bitstreams) {
                if (filter.accept(bs)) {
                    if (isTest) {
                        ItemUpdate.pr("Delete from bundle " + bundleName + " bitstream " + bs.getName()
                                          + " with id = " + bs.getID());
                    } else {
                        //provenance is not maintained for derivative bitstreams
                        if (!bundleName.equals("THUMBNAIL") && !bundleName.equals("TEXT")) {
                            deleted.add(bs.getName());
                        }
                        bundleService.removeBitstream(context, b, bs);
                        ItemUpdate.pr("Deleted " + bundleName + " bitstream " + bs.getName()
                                          + " with id = " + bs.getID());
                    }
                }
            }
        }

        // Go ahead and update the bitstreamurl information.
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
        // End change.

        if (alterProvenance && !deleted.isEmpty()) {
            StringBuilder sb = new StringBuilder("  Bitstreams deleted on ");
            sb.append(DCDate.getCurrent()).append(": ");

            for (String s : deleted) {
                sb.append(s).append(", ");
            }

            DtoMetadata dtom = DtoMetadata.create("dc.description.provenance", "en", "");

            ItemUpdate.pr("Append provenance with: " + sb.toString());

            if (!isTest) {
                MetadataUtilities.appendMetadata(context, item, dtom, false, sb.toString());
            }
        }
    }

}
