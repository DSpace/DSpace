package org.dspace.versioning;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Jun 6, 2011
 * Time: 12:03:43 PM
 * To change this template use File | Settings | File Templates.
 *
 *
 * Deprecated!
 */
public class VersionServiceUtil {

    public static void wire(Item nativeItem, Item itemNew){
        String nativeItemID = getIdentifier(nativeItem);
        String itemNewID = getIdentifier(itemNew);

        if( nativeItem.getMetadata(MetadataSchema.DC_SCHEMA, "relation", "hasversion", Item.ANY)==null
                || nativeItem.getMetadata(MetadataSchema.DC_SCHEMA, "relation", "isversionof", Item.ANY)==null ){
            throw new RuntimeException("Metadata fields registry hasversion and isversionof not present on the database! ");
        }


        nativeItem.addMetadata(MetadataSchema.DC_SCHEMA, "relation", "hasversion", null, itemNewID);
        itemNew.addMetadata(MetadataSchema.DC_SCHEMA, "relation", "isversionof", null, nativeItemID);
    }


    public static Item getFatherItem(Context c, int  itemID) throws AuthorizeException, IOException, SQLException {
        Item item = Item.find(c, itemID);
        return getFatherItem(c, item);
    }


    public static Item getFatherItem(Context c, Item item) throws AuthorizeException, IOException, SQLException {
        DCValue[] isVersionOf = item.getMetadata(MetadataSchema.DC_SCHEMA, "relation", "isversionof", Item.ANY);
        if(isVersionOf.length==0) return null;

        ItemIterator iter = Item.findByMetadataField(c, MetadataSchema.DC_SCHEMA, "identifier", null, isVersionOf[0].value);
        return (Item)iter.next();
    }

    public static boolean existNextVersion(Context c, Item item){
        DCValue[] values = item.getMetadata(MetadataSchema.DC_SCHEMA, "relation", "hasversion", Item.ANY);
        if(values==null || values.length==0) return false;

        return true;
    }


    private static String getIdentifier(Item item){
        DCValue[] identifiers = item.getMetadata(MetadataSchema.DC_SCHEMA, "identifier", null, Item.ANY);
        return identifiers[0].value;
    }
}
