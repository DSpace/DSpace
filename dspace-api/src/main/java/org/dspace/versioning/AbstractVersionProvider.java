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
import java.util.Set;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public abstract class AbstractVersionProvider {

    private Set<String> ignoredMetadataFields;

    protected void copyMetadata(Item itemNew, Item nativeItem){
        DCValue[] md = nativeItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (DCValue aMd : md) {
            String unqualifiedMetadataField = aMd.schema + "." + aMd.element;
            String qualifiedMetadataField = unqualifiedMetadataField + (aMd.qualifier == null ? "" : "." + aMd.qualifier);
            if(getIgnoredMetadataFields().contains(qualifiedMetadataField) ||
                    getIgnoredMetadataFields().contains(unqualifiedMetadataField + "." + Item.ANY))
            {
                //Skip this metadata field
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

    public void setIgnoredMetadataFields(Set<String> ignoredMetadataFields) {
        this.ignoredMetadataFields = ignoredMetadataFields;
    }

    public Set getIgnoredMetadataFields() {
        return ignoredMetadataFields;
    }
}
