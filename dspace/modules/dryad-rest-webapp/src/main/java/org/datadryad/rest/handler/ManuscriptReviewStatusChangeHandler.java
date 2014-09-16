/*
 */
package org.datadryad.rest.handler;

import org.apache.log4j.Logger;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.core.ConfigurationManager;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.workflow.ApproveRejectReviewItem;
import org.dspace.workflow.ApproveRejectReviewItemException;

/**
 * Processes accept/reject status and moves items in review.
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptReviewStatusChangeHandler implements HandlerInterface<Manuscript> {
    private static final Logger log = Logger.getLogger(ManuscriptReviewStatusChangeHandler.class);
    private DSpaceKernelImpl kernelImpl;
    public ManuscriptReviewStatusChangeHandler() {
        // Requires DSpace kernel to be running
        try {
            kernelImpl = DSpaceKernelInit.getKernel(null);
            if (!kernelImpl.isRunning())
            {
                kernelImpl.start(ConfigurationManager.getProperty("dspace.dir"));
            }
        } catch (Exception ex) {
            // Failed to start so destroy it and log and throw an exception
            try {
                if(kernelImpl != null) {
                    kernelImpl.destroy();
                }
            } catch (Exception e1) {
                // Nothing to do
            }
            log.error("Error Initializing DSpace kernel in ManuscriptReviewStatusChangeHandler", ex);
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
        // Do nothing
    }

    private void processChange(Manuscript manuscript) throws HandlerException {
        if(kernelImpl == null) {
            throw new HandlerException("Cannot process change, DSpace Kernel is not initialized");
        } else if(!kernelImpl.isRunning()) {
            throw new HandlerException("Cannot process change, DSpace Kernel is not running");
        }

        String status = manuscript.status;
        if(Manuscript.STATUS_SUBMITTED.equals(status)) {
            // Do nothing for submitted
        } else if(Manuscript.STATUS_ACCEPTED.equals(status) || Manuscript.STATUS_PUBLISHED.equals(status)) {
            // accept for accepted or published
            accept(manuscript);
        } else if (Manuscript.STATUS_REJECTED.equals(status) || Manuscript.STATUS_NEEDS_REVISION.equals(status)) {
            // reject for rejected or needs revision
            reject(manuscript);
        }
    }

    private void accept(Manuscript manuscript) throws HandlerException {
        // dspace review-item -a true
        try {
            if(manuscript.dryadDataDOI != null) {
                ApproveRejectReviewItem.reviewItemDOI(Boolean.TRUE, manuscript.dryadDataDOI);
            } else {
                ApproveRejectReviewItem.reviewItem(Boolean.TRUE, manuscript.manuscriptId);
            }
        } catch (ApproveRejectReviewItemException ex) {
            throw new HandlerException("Exception handling acceptance notice for manuscript " + manuscript.manuscriptId, ex);
        }
    }

    private void reject(Manuscript manuscript) throws HandlerException {
        // dspace review-item -a false
        try {
            if(manuscript.dryadDataDOI != null) {
                ApproveRejectReviewItem.reviewItemDOI(Boolean.FALSE, manuscript.dryadDataDOI);
            } else {
                ApproveRejectReviewItem.reviewItem(Boolean.FALSE, manuscript.manuscriptId);
            }
        } catch (ApproveRejectReviewItemException ex) {
            throw new HandlerException("Exception handling rejection notice for manuscript " + manuscript.manuscriptId, ex);
        }
    }

}
