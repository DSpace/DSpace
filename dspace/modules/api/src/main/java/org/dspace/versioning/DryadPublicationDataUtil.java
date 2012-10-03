package org.dspace.versioning;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Jun 6, 2011
 * Time: 12:03:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class DryadPublicationDataUtil {

    public static void wire(Item dataPackage, Item dataFile){
       wireDP2DF(dataPackage, dataFile);
       wireDF2DP(dataPackage, dataFile);
    }

     public static void wireDP2DF(Item dataPackage, Item dataFile){
        String dataFileID = getIdentifier(dataFile);
        dataPackage.addMetadata(MetadataSchema.DC_SCHEMA, "relation", "haspart", null, dataFileID);

    }

    public static void wireDF2DP(Item dataPackage, Item dataFile){
        String dataPackageID = getIdentifier(dataPackage);
        dataFile.addMetadata(MetadataSchema.DC_SCHEMA, "relation", "ispartof", null, dataPackageID);
    }
    
    private static String getIdentifier(Item item){
        DCValue[] identifiers = item.getMetadata(MetadataSchema.DC_SCHEMA, "identifier", null, Item.ANY);
        if(identifiers==null || identifiers.length==0)  return null;
        return identifiers[0].value;
    }
}
