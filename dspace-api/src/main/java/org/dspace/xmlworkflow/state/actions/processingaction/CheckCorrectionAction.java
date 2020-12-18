/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Context;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action to check if the current workflow item is a correction of another item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CheckCorrectionAction extends ProcessingAction {

    @Autowired
    private ItemCorrectionService itemCorrectionService;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {
    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException {

        if (itemCorrectionService.checkIfIsCorrectionItem(c, wfi.getItem())) {
            c.turnOffAuthorisationSystem();
            itemCorrectionService.replaceCorrectionItemWithNative(c, wfi);
            c.restoreAuthSystemState();
        }

        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<String>();
    }

}
