/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.doi;

import java.sql.SQLException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;

/**
 * Convenience methods involved in registering DOIs.
 * @author dan
 */
public class DryadDOIRegistrationHelper {

    public static final String REGISTER_PENDING_PUBLICATION_STEP = "registerPendingPublicationStep";

    public static boolean isDataPackageInPublicationBlackout(Item dataPackage) throws SQLException {
        // Publication blackout is indicated by provenance metadata
        boolean isInBlackout = false;
        DCValue provenance[] =  dataPackage.getMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", Item.ANY);
        for(DCValue dcValue : provenance) {
            // only return true if the last recorded provenance indicates publication blackout
            if(dcValue.value != null)
                if(dcValue.value.contains("Entered publication blackout")) {
                    isInBlackout = true;
                } else {
                    isInBlackout = false;
            }

        }
        // now find something that would negate blackout
        return isInBlackout;
    }

}
