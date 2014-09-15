/*
 */
package org.datadryad.rest.handler;

import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.core.Context;

// TODO: Update this as a general purpose handler that synchronizes metadata between
// manuscript object and dryad data package (if exists)

/**
 * Extracts publication date from manuscript and places in data package metadata
 * (if exists)
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptReleaseDateHandler implements HandlerInterface<Manuscript>{
    private static final Logger log = Logger.getLogger(ManuscriptReleaseDateHandler.class);

    // Move to an abstract base class
    private static Context getContext() {
        Context context = null;
        try {
            context = new Context();
        } catch (SQLException ex) {
            log.error("Unable to instantiate DSpace context", ex);
        }
        return context;
    }

    private static void completeContext(Context context) throws SQLException {
        try {
            context.complete();
        } catch (SQLException ex) {
            // Abort the context to force a new connection
            abortContext(context);
            throw ex;
        }
    }

    private static void abortContext(Context context) {
        context.abort();
    }

    @Override
    public void handleCreate(StoragePath path, Manuscript manuscript) throws HandlerException {
        processChange(manuscript);
    }

    @Override
    public void handleUpdate(StoragePath path, Manuscript manuscript) throws HandlerException {
        processChange(manuscript);
    }

    @Override
    public void handleDelete(StoragePath path, Manuscript object) throws HandlerException {
        // TODO: Anything for deletions?
    }

    private void processChange(Manuscript manuscript) throws HandlerException {
        // TODO: Prefer to find by Dryad DOI
        try {
            Context context = getContext();
            String manuscriptId = manuscript.manuscriptId;
            DryadDataPackage dataPackage = DryadDataPackage.findByManuscriptNumber(context, manuscriptId);
            if(dataPackage == null) {
                throw new HandlerException("Data package not found with manuscriptId: " + manuscriptId);
            }
            dataPackage.setBlackoutUntilDate(manuscript.publicationDate);
            completeContext(context);
        } catch (SQLException ex) {
            throw new HandlerException("SQLException updating publicationDate", ex);
        }
    }
}
