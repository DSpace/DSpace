/*
 */
package org.datadryad.rest.handler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;
import org.dspace.workflow.ApproveRejectReviewItem;
import org.dspace.workflow.ApproveRejectReviewItemException;

/**
 * Extracts metadata from manuscript objects and places in corresponding Dryad
 * Data Package submission (if DOI or reviewer URL present)
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptMetadataSynchronizationHandler implements HandlerInterface<Manuscript>{
    private static final Logger log = Logger.getLogger(ManuscriptMetadataSynchronizationHandler.class);

    // Move to an abstract base class
    private static Context getContext() {
        Context context = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
        } catch (SQLException ex) {
            log.error("Unable to instantiate DSpace context", ex);
        }
        return context;
    }

    private static void completeContext(Context context) throws SQLException {
        try {
            if(context != null) {
                context.complete();
            }
        } catch (SQLException ex) {
            // Abort the context to force a new connection
            abortContext(context);
            throw ex;
        }
    }

    private static void abortContext(Context context) {
        if(context != null) {
            context.abort();
        }
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

    /**
     * Find a data package based on manuscript data. Prefer DOI, then reviewer URL, then manuscript number
     * @param manuscript a manuscript object containing one of the identifiers
     * @param context database context
     * @return a {@link DryadDataPackage} object if the item could be found, null otherwise
     * @throws HandlerException if an error occurred during the lookup
     */
    private static DryadDataPackage findDataPackage(Manuscript manuscript, Context context) throws HandlerException {
        String doi = manuscript.dryadDataDOI;
        String reviewerURL = manuscript.dataReviewURL;
        String manuscriptId = manuscript.manuscriptId;
        DryadDataPackage dataPackage = null;
        try {
            if(doi != null && !doi.isEmpty()) {
                // 1. look up by DOI
                dataPackage = DryadDataPackage.findByIdentifier(context, doi);
            } else if(reviewerURL != null && !reviewerURL.isEmpty()) {
                // 2. look up by reviewer url
                dataPackage = DryadDataPackage.findByReviewerURL(context, reviewerURL);
            } else if(manuscriptId != null && !manuscriptId.isEmpty()) {
                dataPackage = DryadDataPackage.findByManuscriptNumber(context, manuscriptId);
            }
        } catch (IdentifierException ex) {
            throw new HandlerException("Identifier exception finding data package", ex);
        } catch (SQLException ex) {
            throw new HandlerException("SQL Exception finding data package", ex);
        }
        return dataPackage;
    }


    private void processChange(Manuscript manuscript) throws HandlerException {
        try {
            ApproveRejectReviewItem.reviewManuscript(manuscript);
        } catch (ApproveRejectReviewItemException ex) {
            throw new HandlerException("Exception handling acceptance notice for manuscript " + manuscript.manuscriptId, ex);
        }
    }
}
