/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.utils.DSpace;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.junit.Test;

/**
 * Tests that check that the spring beans (of type {@link Step}) in workflow.xml get created correctly
 *
 * @author Maria Verdonck (Atmire) on 19/12/2019
 */
public class StepTest extends AbstractUnitTest {

    private Workflow defaultWorkflow
            = new DSpace().getServiceManager().getServiceByName("defaultWorkflow", Workflow.class);
    private Workflow selectSingleReviewer
            = new DSpace().getServiceManager().getServiceByName("selectSingleReviewer", Workflow.class);
    private Workflow scoreReview
            = new DSpace().getServiceManager().getServiceByName("scoreReview", Workflow.class);

    @Test
    public void defaultWorkflow_ReviewStep() throws WorkflowConfigurationException {
        Step step = defaultWorkflow.getStep("reviewstep");
        assertEquals("claimaction", step.getUserSelectionMethod().getId());
        assertEquals("Reviewer", step.getRole().getName());
        List<WorkflowActionConfig> actions = step.getActions();
        assertTrue(this.containsActionNamed(actions, "reviewaction"));
        assertEquals("editstep", step.getNextStep(0).getId());
    }

    @Test
    public void defaultWorkflow_EditStep() throws WorkflowConfigurationException {
        Step step = defaultWorkflow.getStep("editstep");
        assertEquals("claimaction", step.getUserSelectionMethod().getId());
        assertEquals("Editor", step.getRole().getName());
        List<WorkflowActionConfig> actions = step.getActions();
        assertTrue(this.containsActionNamed(actions, "editaction"));
        assertEquals("finaleditstep", step.getNextStep(0).getId());
    }

    @Test
    public void defaultWorkflow_FinalEditStep() throws WorkflowConfigurationException {
        Step step = defaultWorkflow.getStep("finaleditstep");
        assertEquals("claimaction", step.getUserSelectionMethod().getId());
        assertEquals("Final Editor", step.getRole().getName());
        List<WorkflowActionConfig> actions = step.getActions();
        assertTrue(this.containsActionNamed(actions, "finaleditaction"));
        assertNull(step.getNextStep(0));
    }

    @Test
    public void selectSingleReviewer_SelectReviewerStep() throws WorkflowConfigurationException {
        Step step = selectSingleReviewer.getStep("selectReviewerStep");
        assertEquals("claimaction", step.getUserSelectionMethod().getId());
        assertEquals("ReviewManagers", step.getRole().getName());
        List<WorkflowActionConfig> actions = step.getActions();
        assertTrue(this.containsActionNamed(actions, "selectrevieweraction"));
        assertEquals("singleUserReviewStep", step.getNextStep(0).getId());
    }

    @Test
    public void selectSingleReviewer_SingleUserReviewStep() throws WorkflowConfigurationException {
        Step step = selectSingleReviewer.getStep("singleUserReviewStep");
        assertEquals("autoassignAction", step.getUserSelectionMethod().getId());
        assertEquals("Reviewer", step.getRole().getName());
        List<WorkflowActionConfig> actions = step.getActions();
        assertTrue(this.containsActionNamed(actions, "singleuserreviewaction"));
        assertEquals("selectReviewerStep", step.getNextStep(1).getId());
    }

    @Test
    public void scoreReview_ScoreReviewStep() throws WorkflowConfigurationException {
        Step step = scoreReview.getStep("scoreReviewStep");
        assertEquals("claimaction", step.getUserSelectionMethod().getId());
        assertEquals("ScoreReviewers", step.getRole().getName());
        List<WorkflowActionConfig> actions = step.getActions();
        assertTrue(this.containsActionNamed(actions, "scorereviewaction"));
        assertEquals("evaluationStep", step.getNextStep(0).getId());
        assertEquals(2, step.getRequiredUsers());
    }

    @Test
    public void scoreReview_EvaluationStep() throws WorkflowConfigurationException {
        Step step = scoreReview.getStep("evaluationStep");
        assertEquals("noUserSelectionAction", step.getUserSelectionMethod().getId());
        List<WorkflowActionConfig> actions = step.getActions();
        assertTrue(this.containsActionNamed(actions, "evaluationaction"));
        assertNull(step.getNextStep(0));
    }

    private boolean containsActionNamed(List<WorkflowActionConfig> actions, String actionName) {
        for (WorkflowActionConfig workflowActionConfig : actions) {
            if (workflowActionConfig.getId().equalsIgnoreCase(actionName)) {
                return true;
            }
        }
        return false;
    }
}
