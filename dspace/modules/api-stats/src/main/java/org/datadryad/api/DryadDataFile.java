/*
 */
package org.datadryad.api;

import java.io.IOException;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadDataFile extends DryadObject {
    private static final String FILES_COLLECTION_HANDLE_KEY = "stats.datafiles.coll";
    private DryadDataPackage dataPackage;
    private static Logger log = Logger.getLogger(DryadDataFile.class);

    public DryadDataFile(WorkspaceItem workspaceItem) {
        super(workspaceItem);
    }

    public static Collection getCollection(Context context) throws SQLException {
        String handle = ConfigurationManager.getProperty(FILES_COLLECTION_HANDLE_KEY);
        return DryadObject.collectionFromHandle(context, handle);
    }

    public static DryadDataFile create(Context context) throws SQLException {
        Collection collection = DryadDataFile.getCollection(context);
        WorkspaceItem wsi = null;
        try {
            wsi = WorkspaceItem.create(context, collection, true);
        } catch (AuthorizeException ex) {
            log.error("Authorize exception creating a Data File", ex);
        } catch (IOException ex) {
            log.error("IO exception creating a Data File", ex);
        }
        if(wsi == null) {
            return null;
        } else {
            return new DryadDataFile(wsi);
        }
    }

    public DryadDataPackage getDataPackage(Context context) {
        if(dataPackage == null) {
            // Find the data package for this file
            throw new RuntimeException("Not yet implemented");
        }
        return dataPackage;
    }

}
