/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import static junit.framework.TestCase.assertEquals;

import org.dspace.AbstractUnitTest;
import org.dspace.utils.DSpace;
import org.dspace.xmlworkflow.state.Workflow;
import org.junit.Test;

/**
 * Tests that check that the spring beans (of type {@link Role}) in workflow.xml get created correctly
 *
 * @author Maria Verdonck (Atmire) on 19/12/2019
 */
public class RoleTest extends AbstractUnitTest {

    private Workflow defaultWorkflow
            = new DSpace().getServiceManager().getServiceByName("defaultWorkflow", Workflow.class);
    private Workflow selectSingleReviewer
            = new DSpace().getServiceManager().getServiceByName("selectSingleReviewer", Workflow.class);
    private Workflow scoreReview
            = new DSpace().getServiceManager().getServiceByName("scoreReview", Workflow.class);

    @Test
    public void defaultWorkflow_RoleReviewer() {
        Role role = defaultWorkflow.getRoles().get("Reviewer");
        assertEquals("The people responsible for this step are able to edit the metadata of incoming submissions, " +
                        "and then accept or reject them.", role.getDescription());
        assertEquals("Reviewer", role.getName());
        assertEquals(Role.Scope.COLLECTION, role.getScope());
    }

    @Test
    public void defaultWorkflow_RoleEditor() {
        Role role = defaultWorkflow.getRoles().get("Editor");
        assertEquals("The people responsible for this step are able to edit the " +
                "metadata of incoming submissions, and then accept or reject them.", role.getDescription());
        assertEquals("Editor", role.getName());
        assertEquals(Role.Scope.COLLECTION, role.getScope());
    }

    @Test
    public void defaultWorkflow_RoleFinalEditor() {
        Role role = defaultWorkflow.getRoles().get("Final Editor");
        assertEquals("The people responsible for this step are able to edit the " +
                "metadata of incoming submissions, but will not be able to reject them.", role.getDescription());
        assertEquals("Final Editor", role.getName());
        assertEquals(Role.Scope.COLLECTION, role.getScope());
    }

    @Test
    public void selectSingleReviewer_RoleReviewManagers() {
        Role role = selectSingleReviewer.getRoles().get("ReviewManagers");
        assertEquals("ReviewManagers", role.getName());
        assertEquals(Role.Scope.REPOSITORY, role.getScope());
    }

    @Test
    public void selectSingleReviewer_RoleReviewer() {
        Role role = selectSingleReviewer.getRoles().get("Reviewer");
        assertEquals("Reviewer", role.getName());
        assertEquals(Role.Scope.ITEM, role.getScope());
    }

    @Test
    public void scoreReview_RoleScoreReviewers() {
        Role role = scoreReview.getRoles().get("ScoreReviewers");
        assertEquals("ScoreReviewers", role.getName());
        assertEquals(Role.Scope.COLLECTION, role.getScope());
    }
}
