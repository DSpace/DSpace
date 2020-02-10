/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state;

import static junit.framework.TestCase.assertEquals;

import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.utils.DSpace;
import org.junit.Test;

/**
 * Tests that check that the spring beans (of type {@link Workflow}) in workflow.xml get created correctly
 *
 * @author Maria Verdonck (Atmire) on 19/12/2019
 */
public class WorkflowTest extends AbstractUnitTest {

    private Workflow defaultWorkflow
            = new DSpace().getServiceManager().getServiceByName("defaultWorkflow", Workflow.class);
    private Workflow selectSingleReviewer
            = new DSpace().getServiceManager().getServiceByName("selectSingleReviewer", Workflow.class);
    private Workflow scoreReview
            = new DSpace().getServiceManager().getServiceByName("scoreReview", Workflow.class);

    @Test
    public void defaultWorkflow() {
        assertEquals(defaultWorkflow.getFirstStep().getId(), "reviewstep");
        List<Step> steps = defaultWorkflow.getSteps();
        assertEquals(steps.size(), 3);
        assert (this.containsStepNamed(steps, "reviewstep"));
        assert (this.containsStepNamed(steps, "editstep"));
        assert (this.containsStepNamed(steps, "finaleditstep"));
    }

    @Test
    public void selectSingleReviewer() {
        assertEquals(selectSingleReviewer.getFirstStep().getId(), "selectReviewerStep");
        List<Step> steps = selectSingleReviewer.getSteps();
        assertEquals(steps.size(), 2);
        assert (this.containsStepNamed(steps, "selectReviewerStep"));
        assert (this.containsStepNamed(steps, "singleUserReviewStep"));
    }

    @Test
    public void scoreReview() {
        assertEquals(scoreReview.getFirstStep().getId(), "scoreReviewStep");
        List<Step> steps = scoreReview.getSteps();
        assertEquals(steps.size(), 2);
        assert (this.containsStepNamed(steps, "scoreReviewStep"));
        assert (this.containsStepNamed(steps, "evaluationStep"));
    }

    private boolean containsStepNamed(List<Step> steps, String stepName) {
        for (Step step : steps) {
            if (step.getId().equalsIgnoreCase(stepName)) {
                return true;
            }
        }
        return false;
    }
}
