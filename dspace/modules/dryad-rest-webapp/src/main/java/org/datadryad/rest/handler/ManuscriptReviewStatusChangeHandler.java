/*
 */
package org.datadryad.rest.handler;

import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.storage.StoragePath;
import org.datadryad.rest.utils.DryadPathUtilities;

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

    private void processChange(StoragePath path, Manuscript manuscript) {
        String organizationCode = DryadPathUtilities.getOrganizationCode(path);
        processChange(organizationCode, manuscript);
    }

    private void processChange(String organizationCode, Manuscript manuscript) {
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

    private void accept(String organizationCode, Manuscript manuscript) {
        // dspace review-item -a true
        // org.dspace.workflow.ApproveRejectReviewItem only works from CLI right now
        // refactoring
    }

    private void reject(String organizationCode, Manuscript manuscript) {
        // dspace review-item -a false
        // org.dspace.workflow.ApproveRejectReviewItem only works from CLI right now
        // refactoring
    }

}
