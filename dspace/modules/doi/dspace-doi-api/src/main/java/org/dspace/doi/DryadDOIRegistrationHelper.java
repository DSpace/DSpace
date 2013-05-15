/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.doi;

import com.sun.org.apache.bcel.internal.generic.L2D;
import java.sql.SQLException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Convenience methods involved in registering DOIs.
 * @author dan
 */
public class DryadDOIRegistrationHelper {

    public static final String REGISTER_PENDING_PUBLICATION_STEP = "registerPendingPublicationStep";

    public static boolean isDataPackageInPublicationBlackout(Item dataPackage) throws SQLException {
        // Publication blackout is indicated by provenance metadata
        boolean isInBlackout = false;
        DCValue provenance[] =  dataPackage.getMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en");
        for(DCValue dcValue : provenance) {
            if(dcValue.value != null && dcValue.value.contains("Entered publication blackout")) {
                isInBlackout = true;
            }
        }
        // now find something that would negate blackout
        return isInBlackout;
    }

}
