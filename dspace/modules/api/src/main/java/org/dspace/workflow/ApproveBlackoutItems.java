/*
 */
package org.dspace.workflow;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.doi.DryadDOIRegistrationHelper;

/**
 * Finds items in publication blackout with a dc.date.blackoutUntil date in the past
 * and approves them
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ApproveBlackoutItems {

    private static final Logger log = Logger.getLogger(ApproveBlackoutItems.class);

    public static void main(String args[]) {
        try {
            Context c = new Context();
            approveBlackoutItems(c);
            if(c != null) {
                c.complete();
            }
        } catch (SQLException ex) {
            log.error("SQLException approving blackout items", ex);
        } catch (AutoWorkflowProcessorException ex) {
            log.error("Exception approving blackout items", ex);
        }

    }


    public static void approveBlackoutItems(Context c) throws SQLException, AutoWorkflowProcessorException {
        // get all items in workflow
        WorkflowItem items[];
        try {
            items = WorkflowItem.findAll(c);
        } catch (AuthorizeException ex) {
            throw new AutoWorkflowProcessorException("Authorize Exception finding workflow items", ex);
        } catch (IOException ex) {
            throw new AutoWorkflowProcessorException("IO Exception finding workflow items", ex);
        }
        c.setCurrentUser(AutoWorkflowProcessor.getSystemCurator(c));

        for(WorkflowItem wfi : items) {
            if(checkAndApproveItem(c, wfi)) {
                log.info("Approved item " + wfi.getID() + " from blackout");
            } else {
                log.info("Unable to approve item " + wfi.getID() + " from blackout");
            }
        }
    }

    private static Boolean checkAndApproveItem(Context c, WorkflowItem wfi) {
        try {
            if(itemIsInBlackout(c, wfi) && publicationDateHasPassed(c, wfi)) {
                // item is in blackout and date has passed
                try {
                    AutoApproveBlackoutProcessor processor = new AutoApproveBlackoutProcessor(c);
                    return processor.processWorkflowItem(wfi);
                } catch (AutoWorkflowProcessorException ex) {
                    log.error("Unable to approve Workflow item: " + wfi.getID() + " in blackout", ex);
                    return Boolean.FALSE;
                } catch (ItemIsNotEligibleForStepException ex) {
                    log.error("Item: " + wfi.getID() + " is not in blackout", ex);
                    return Boolean.FALSE;
                }
            } else {
                return Boolean.FALSE;
            }
        } catch (SQLException ex) {
            log.error("SQL Exception approving Workflow item: " + wfi.getID() + " in blackout", ex);
            return Boolean.FALSE;
        }
    }

    private static Boolean itemIsInBlackout(Context c, WorkflowItem wfi) throws SQLException {
        return DryadDOIRegistrationHelper.isDataPackageInPublicationBlackout(wfi.getItem());
    }

    private static Boolean publicationDateHasPassed(Context c, WorkflowItem wfi) throws SQLException {
        DryadDataPackage dataPackage = new DryadDataPackage(wfi.getItem());
        Date blackoutUntilDate = dataPackage.getBlackoutUntilDate();
        if(blackoutUntilDate == null) {
            return Boolean.FALSE;
        } else {
            Date now = new Date();
            return blackoutUntilDate.before(now);
        }
    }

}
