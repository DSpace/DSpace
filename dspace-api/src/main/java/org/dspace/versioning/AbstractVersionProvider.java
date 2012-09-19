/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.storage.bitstore.BitstreamStorageManager;

import java.sql.SQLException;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public abstract class AbstractVersionProvider {


    protected void copyMetadata(Item itemNew, Item nativeItem){
        DCValue[] md = nativeItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (DCValue aMd : md) {
            if ((aMd.schema.equals(MetadataSchema.DC_SCHEMA) && aMd.element.equals("relation") && (aMd.qualifier != null && aMd.qualifier.equals("haspart")))
                    || (aMd.schema.equals(MetadataSchema.DC_SCHEMA) && aMd.element.equals("relation") && (aMd.qualifier != null && aMd.qualifier.equals("ispartof")))
                    || (aMd.schema.equals(MetadataSchema.DC_SCHEMA) && aMd.element.equals("identifier"))
                    || (aMd.schema.equals(MetadataSchema.DC_SCHEMA) && aMd.element.equals("relation") && (aMd.qualifier != null && aMd.qualifier.equals("isversionof")))
                    || (aMd.schema.equals(MetadataSchema.DC_SCHEMA) && aMd.element.equals("date") && (aMd.qualifier != null && aMd.qualifier.equals("accessioned")))
                    || (aMd.schema.equals(MetadataSchema.DC_SCHEMA) && aMd.element.equals("date") && (aMd.qualifier != null && aMd.qualifier.equals("available")))
                    || (aMd.schema.equals(MetadataSchema.DC_SCHEMA) && aMd.element.equals("description") && (aMd.qualifier != null && aMd.qualifier.equals("provenance"))))
            {
                continue;
            }


            itemNew.addMetadata(aMd.schema, aMd.element, aMd.qualifier, aMd.language, aMd.value);
        }
    }

    protected void createBundlesAndAddBitstreams(Context c, Item itemNew, Item nativeItem) throws SQLException, AuthorizeException {
        for(Bundle nativeBundle : nativeItem.getBundles())
        {
            Bundle bundleNew = itemNew.createBundle(nativeBundle.getName());

            for(Bitstream nativeBitstream : nativeBundle.getBitstreams())
            {

                Bitstream bitstreamNew = createBitstream(c, nativeBitstream);
                bundleNew.addBitstream(bitstreamNew);

                if(nativeBundle.getPrimaryBitstreamID() == nativeBitstream.getID())
                {
                    bundleNew.setPrimaryBitstreamID(bitstreamNew.getID());
                }
            }
        }
    }


    protected Bitstream createBitstream(Context context, Bitstream nativeBitstream) throws AuthorizeException, SQLException {
        int idNew = BitstreamStorageManager.clone(context, nativeBitstream.getID());
        return Bitstream.find(context, idNew);
    }
}
