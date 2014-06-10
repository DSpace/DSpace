/*
 */
package org.datadryad.api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
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
public class DryadDataPackage extends DryadObject {
    private static final String PACKAGES_COLLECTION_HANDLE_KEY = "stats.datapkgs.coll";
    private Set<DryadDataFile> dataFiles;
    private static Logger log = Logger.getLogger(DryadDataPackage.class);

    public DryadDataPackage(WorkspaceItem workspaceItem) {
        super(workspaceItem);
    }

    public static Collection getCollection(Context context) throws SQLException {
        String handle = ConfigurationManager.getProperty(PACKAGES_COLLECTION_HANDLE_KEY);
        return DryadObject.collectionFromHandle(context, handle);
    }

    public static DryadDataPackage create(Context context) throws SQLException {
        Collection collection = DryadDataPackage.getCollection(context);
        WorkspaceItem wsi = null;
        try {
            wsi = WorkspaceItem.create(context, collection, true);
        } catch (AuthorizeException ex) {
            log.error("Authorize exception creating a Data Package", ex);
        } catch (IOException ex) {
            log.error("IO exception creating a Data Package", ex);
        }
        if(wsi == null) {
            return null;
        } else {
            return new DryadDataPackage(wsi);
        }

    }

    static Set<DryadDataFile> getFilesInPackage(Context context, DryadDataPackage dataPackage) {
        // files and packages are linked by DOI
        return new HashSet<DryadDataFile>();
    }

    public Set<DryadDataFile> getDataFiles(Context context) {
        if(dataFiles == null) {
            // TODO: Get data files
            throw new RuntimeException("Not yet implemented");
        }
        return dataFiles;
    }

}
