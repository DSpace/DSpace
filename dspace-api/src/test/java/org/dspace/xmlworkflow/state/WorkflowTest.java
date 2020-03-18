/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        assertEquals("reviewstep", defaultWorkflow.getFirstStep().getId());
        List<Step> steps = defaultWorkflow.getSteps();
        assertEquals(3, steps.size());
        assertTrue(this.containsStepNamed(steps, "reviewstep"));
        assertTrue(this.containsStepNamed(steps, "editstep"));
        assertTrue(this.containsStepNamed(steps, "finaleditstep"));
    }

    @Test
    public void selectSingleReviewer() {
        assertEquals("selectReviewerStep", selectSingleReviewer.getFirstStep().getId());
        List<Step> steps = selectSingleReviewer.getSteps();
        assertEquals(2, steps.size());
        assertTrue(this.containsStepNamed(steps, "selectReviewerStep"));
        assertTrue(this.containsStepNamed(steps, "singleUserReviewStep"));
    }

    @Test
    public void scoreReview() {
        assertEquals("scoreReviewStep", scoreReview.getFirstStep().getId());
        List<Step> steps = scoreReview.getSteps();
        assertEquals(2, steps.size());
        assertTrue(this.containsStepNamed(steps, "scoreReviewStep"));
        assertTrue(this.containsStepNamed(steps, "evaluationStep"));
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
