/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

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
        assertEquals(step.getUserSelectionMethod().getId(), "claimaction");
        assertEquals(step.getRole().getName(), "Reviewer");
        List<WorkflowActionConfig> actions = step.getActions();
        assert (this.containsActionNamed(actions, "reviewaction"));
        assertEquals(step.getNextStep(0).getId(), "editstep");
    }

    @Test
    public void defaultWorkflow_EditStep() throws WorkflowConfigurationException {
        Step step = defaultWorkflow.getStep("editstep");
        assertEquals(step.getUserSelectionMethod().getId(), "claimaction");
        assertEquals(step.getRole().getName(), "Editor");
        List<WorkflowActionConfig> actions = step.getActions();
        assert (this.containsActionNamed(actions, "editaction"));
        assertEquals(step.getNextStep(0).getId(), "finaleditstep");
    }

    @Test
    public void defaultWorkflow_FinalEditStep() throws WorkflowConfigurationException {
        Step step = defaultWorkflow.getStep("finaleditstep");
        assertEquals(step.getUserSelectionMethod().getId(), "claimaction");
        assertEquals(step.getRole().getName(), "Final Editor");
        List<WorkflowActionConfig> actions = step.getActions();
        assert (this.containsActionNamed(actions, "finaleditaction"));
        assertNull(step.getNextStep(0));
    }

    @Test
    public void selectSingleReviewer_SelectReviewerStep() throws WorkflowConfigurationException {
        Step step = selectSingleReviewer.getStep("selectReviewerStep");
        assertEquals(step.getUserSelectionMethod().getId(), "claimaction");
        assertEquals(step.getRole().getName(), "ReviewManagers");
        List<WorkflowActionConfig> actions = step.getActions();
        assert (this.containsActionNamed(actions, "selectrevieweraction"));
        assertEquals(step.getNextStep(0).getId(), "singleUserReviewStep");
    }

    @Test
    public void selectSingleReviewer_SingleUserReviewStep() throws WorkflowConfigurationException {
        Step step = selectSingleReviewer.getStep("singleUserReviewStep");
        assertEquals(step.getUserSelectionMethod().getId(), "autoassignAction");
        assert (step.getRole().getName().equals("Reviewer"));
        List<WorkflowActionConfig> actions = step.getActions();
        assert (this.containsActionNamed(actions, "singleuserreviewaction"));
        assertEquals(step.getNextStep(1).getId(), "selectReviewerStep");
    }

    @Test
    public void scoreReview_ScoreReviewStep() throws WorkflowConfigurationException {
        Step step = scoreReview.getStep("scoreReviewStep");
        assertEquals(step.getUserSelectionMethod().getId(), "claimaction");
        assertEquals(step.getRole().getName(), "ScoreReviewers");
        List<WorkflowActionConfig> actions = step.getActions();
        assert (this.containsActionNamed(actions, "scorereviewaction"));
        assertEquals(step.getNextStep(0).getId(), "evaluationStep");
        assertEquals(step.getRequiredUsers(), 2);
    }

    @Test
    public void scoreReview_EvaluationStep() throws WorkflowConfigurationException {
        Step step = scoreReview.getStep("evaluationStep");
        assertEquals(step.getUserSelectionMethod().getId(), "noUserSelectionAction");
        List<WorkflowActionConfig> actions = step.getActions();
        assert (this.containsActionNamed(actions, "evaluationaction"));
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
