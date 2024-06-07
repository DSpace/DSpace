/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.dspace.AbstractUnitTest;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.storedcomponents.dao.ClaimedTaskDAO;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * Unit Tests for class ClaimedTaskServiceImpl
 *
 * @author Andrei Alesik (andrewalesik at pcgacademia)
 */

public class ClaimedTaskServiceImplTest extends AbstractUnitTest {

    @Mock
    private ClaimedTaskDAO claimedTaskDAO;

    @Mock
    private Context context;

    @Mock
    private XmlWorkflowItem workflowItem;

    @InjectMocks
    private ClaimedTaskServiceImpl claimedTaskServiceImpl;

    /**
     * Test method for {@link ClaimedTaskServiceImpl#findByFirstWorkflowItem(Context, XmlWorkflowItem)}.
     * <p>
     * This test verifies that the method returns the expected ClaimedTask when it is found by the DAO.
     *
     * @throws SQLException if any SQL error occurs.
     */
    @Test
    public void testFindByFirstWorkflowItem() throws SQLException {
        ClaimedTask expectedClaimedTask = new ClaimedTask();
        when(claimedTaskDAO.findFirstByWorkflowItem(context, workflowItem)).thenReturn(expectedClaimedTask);

        ClaimedTask result = claimedTaskServiceImpl.findByFirstWorkflowItem(context, workflowItem);

        assertEquals(expectedClaimedTask, result);
        verify(claimedTaskDAO, times(1)).findFirstByWorkflowItem(context, workflowItem);
    }

    /**
     * Test method for {@link ClaimedTaskServiceImpl#findByFirstWorkflowItem(Context, XmlWorkflowItem)}.
     * <p>
     * This test verifies that the method returns null when no ClaimedTask is found by the DAO.
     *
     * @throws SQLException if any SQL error occurs.
     */
    @Test
    public void testFindByFirstWorkflowItemNoResult() throws SQLException {
        when(claimedTaskDAO.findFirstByWorkflowItem(context, workflowItem)).thenReturn(null);

        ClaimedTask result = claimedTaskServiceImpl.findByFirstWorkflowItem(context, workflowItem);

        assertNull(result);
        verify(claimedTaskDAO, times(1)).findFirstByWorkflowItem(context, workflowItem);
    }
}