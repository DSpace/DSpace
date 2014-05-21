/*
 */
package org.datadryad.journalstatistics.extractor;

import java.sql.SQLException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * Base class for extracting statistics from a database
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class DatabaseExtractor<T> implements ExtractorInterface<T> {

    private static final String PACKAGES_COLLECTION_HANDLE_KEY = "stats.datapkgs.coll";
    private static final String FILES_COLLECTION_HANDLE_KEY = "stats.datafiles.coll";
    
    private Context context;
    public DatabaseExtractor(Context context) {
        this.context = context;
    }

    protected DatabaseExtractor() {
    }

    protected final Context getContext() {
        return context;
    }

    private Collection collectionFromHandle(String handle) throws SQLException {
        DSpaceObject object = HandleManager.resolveToObject(context, handle);
        if(object.getType() == Constants.COLLECTION) {
            return (Collection)object;
        } else {
            return null;
        }
    }

    /**
     * Dryad Data packages are in stats.datapkgs.coll
     * @return
     */
    protected Collection getDataPackagesCollection() throws SQLException {
        String handle = ConfigurationManager.getProperty(PACKAGES_COLLECTION_HANDLE_KEY);
        return collectionFromHandle(handle);
    }

    /**
     * Dryad Data files are in stats.datafiles.coll
     * @return
     */
    protected Collection getDataFilesCollection() throws SQLException {
        String handle = ConfigurationManager.getProperty(FILES_COLLECTION_HANDLE_KEY);
        return collectionFromHandle(handle);
    }

    /**
     * Subclasses must override to do their work
     * @param journalName
     * @return
     */
    @Override
    public abstract T extract(String journalName);

}
