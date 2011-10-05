package org.dspace.versioning;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Jun 3, 2011
 * Time: 3:26:52 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractVersionProvider {


    protected void copyMetadata(Item itemNew, Item nativeItem){
        DCValue[] md = nativeItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (int n = 0; n < md.length; n++){


            if( (md[n].schema.equals(MetadataSchema.DC_SCHEMA)  && md[n].element.equals("relation") &&  (md[n].qualifier!=null && md[n].qualifier.equals("haspart")) )
                    || (md[n].schema.equals(MetadataSchema.DC_SCHEMA)  && md[n].element.equals("relation") &&  (md[n].qualifier!=null && md[n].qualifier.equals("ispartof")) )
                      || (md[n].schema.equals(MetadataSchema.DC_SCHEMA)  && md[n].element.equals("identifier"))
                        || (md[n].schema.equals(MetadataSchema.DC_SCHEMA)  && md[n].element.equals("relation") &&  (md[n].qualifier!=null && md[n].qualifier.equals("isversionof")) )
                         || (md[n].schema.equals(MetadataSchema.DC_SCHEMA)  && md[n].element.equals("date") &&  (md[n].qualifier!=null && md[n].qualifier.equals("accessioned")) )
                          || (md[n].schema.equals(MetadataSchema.DC_SCHEMA)  && md[n].element.equals("date") &&  (md[n].qualifier!=null && md[n].qualifier.equals("available")) )
                           || (md[n].schema.equals(MetadataSchema.DC_SCHEMA)  && md[n].element.equals("description") &&  (md[n].qualifier!=null && md[n].qualifier.equals("provenance")) ) )
                continue;


            itemNew.addMetadata(md[n].schema, md[n].element, md[n].qualifier, md[n].language,
            md[n].value);
        }
    }

    protected void createBundlesAndAddBitstreams(Item itemNew, Item nativeItem) throws SQLException, AuthorizeException {
        for(Bundle b : nativeItem.getBundles()){
            Bundle bundleNew = itemNew.createBundle(b.getName());
            bundleNew.setPrimaryBitstreamID(b.getPrimaryBitstreamID());

            for(Bitstream bitstream : b.getBitstreams()){
                bundleNew.addBitstream(bitstream);
            }
        }
    }
}
