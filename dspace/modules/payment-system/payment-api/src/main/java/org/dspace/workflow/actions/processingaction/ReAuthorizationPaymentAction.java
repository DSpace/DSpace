/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow.actions.processingaction;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.paymentsystem.PaypalService;
import org.dspace.utils.DSpace;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.ActionResult;
import org.dspace.workflow.actions.WorkflowActionConfig;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

/**
 * ReAuthorization Payment Action, used in workflow to alert users
 * to failed cc transactions and to facilitate capture of new cc details.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class ReAuthorizationPaymentAction extends ProcessingAction {

    private static Logger log = Logger.getLogger(DryadReviewAction.class);



    @Override
    public void activate(Context c, WorkflowItem wfItem) {
        boolean test=true;
        log.debug("here");
    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {

        try{

            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);


        } catch (Exception e){
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }
    }
}
