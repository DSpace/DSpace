/*
 */
package org.datadryad.rest.handler;

import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.storage.StoragePath;
import org.datadryad.rest.utils.DryadPathUtilities;
import org.dspace.workflow.ApproveRejectReviewItem;
import org.dspace.workflow.ApproveRejectReviewItemException;

/**
 * Processes accept/reject status and moves items in review.
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptReviewStatusChangeHandler implements HandlerInterface<Manuscript> {

    @Override
    public void handleCreate(StoragePath path, Manuscript manuscript) throws HandlerException {
        processChange(path, manuscript);
    }

    @Override
    public void handleUpdate(StoragePath path, Manuscript manuscript) throws HandlerException {
        processChange(path, manuscript);
    }

    @Override
    public void handleDelete(StoragePath path, Manuscript object) throws HandlerException {
        // Do nothing
    }

    private void processChange(StoragePath path, Manuscript manuscript) throws HandlerException {
        String organizationCode = DryadPathUtilities.getOrganizationCode(path);
        processChange(organizationCode, manuscript);
    }

    private void processChange(String organizationCode, Manuscript manuscript) throws HandlerException {
        String status = manuscript.status;
        if(Manuscript.STATUS_SUBMITTED.equals(status)) {
            // Do nothing for submitted
        } else if(Manuscript.STATUS_ACCEPTED.equals(status) || Manuscript.STATUS_PUBLISHED.equals(status)) {
            // accept for accepted or published
            accept(organizationCode, manuscript);
        } else if (Manuscript.STATUS_REJECTED.equals(status) || Manuscript.STATUS_NEEDS_REVISION.equals(status)) {
            // reject for rejected or needs revision
            reject(organizationCode, manuscript);
        }
    }

    private void accept(String organizationCode, Manuscript manuscript) throws HandlerException {
        // dspace review-item -a true
        try {
            ApproveRejectReviewItem.reviewItem(Boolean.TRUE, manuscript.manuscriptId);
        } catch (ApproveRejectReviewItemException ex) {
            throw new HandlerException("Exception handling acceptance notice for manuscript " + manuscript.manuscriptId, ex);
        }
    }

    private void reject(String organizationCode, Manuscript manuscript) throws HandlerException {
        // dspace review-item -a false
        try {
            ApproveRejectReviewItem.reviewItem(Boolean.FALSE, manuscript.manuscriptId);
        } catch (ApproveRejectReviewItemException ex) {
            throw new HandlerException("Exception handling rejection notice for manuscript " + manuscript.manuscriptId, ex);
        }
    }

}
