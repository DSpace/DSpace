/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Thumbnail;
import org.dspace.core.Context;

import java.sql.SQLException;

public class ItemService
{
    private static Logger log = Logger.getLogger(ItemService.class);

    public static Thumbnail getThumbnail(Context context, int itemId, boolean requireOriginal) throws SQLException
    {
        ItemDAO dao = ItemDAOFactory.getInstance(context);

        Bitstream thumbBitstream = null;
        Bitstream primaryBitstream = dao.getPrimaryBitstream(itemId, "ORIGINAL");
        if (primaryBitstream != null)
        {
            if (primaryBitstream.getFormat().getMIMEType().equals("text/html"))
            {
                return null;
            }

            thumbBitstream = dao.getNamedBitstream(itemId, "THUMBNAIL", primaryBitstream.getName() + ".jpg");
        }
        else
        {
            if (requireOriginal)
            {
                primaryBitstream = dao.getFirstBitstream(itemId, "ORIGINAL");
            }
            
            thumbBitstream   = dao.getFirstBitstream(itemId, "THUMBNAIL");
        }

        if (thumbBitstream != null)
        {
            return new Thumbnail(thumbBitstream, primaryBitstream);
        }

        return null;
    }

    public static String getFirstMetadataValue(Item item, String metadataKey) {
        Metadatum[] dcValue = item.getMetadataByMetadataString(metadataKey);
        if(dcValue.length > 0) {
            return dcValue[0].value;
        } else {
            return "";
        }
    }

    /**
     * Service method for knowing if this Item should be visible in the item list.
     * Items only show up in the "item list" if the user has READ permission
     * and if the Item isn't flagged as unlisted.
     * @param context
     * @param item
     * @return
     */
    public static boolean isItemListedForUser(Context context, Item item) {
        try {
            if (AuthorizeManager.isAdmin(context)) {
                return true;
            }

            if (AuthorizeManager.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) {
                if(item.isDiscoverable()) {
                    return true;
                }
            }

            log.debug("item(" + item.getID() + ") " + item.getName() + " is unlisted.");
            return false;
        } catch (SQLException e) {
            log.error(e.getMessage());
            return false;
        }

    }
}
