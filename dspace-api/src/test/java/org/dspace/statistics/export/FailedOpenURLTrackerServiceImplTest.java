/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.statistics.export.dao.OpenURLTrackerDAO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Class to test the FailedOpenURLTrackerServiceImpl
 */
@RunWith(MockitoJUnitRunner.class)
public class FailedOpenURLTrackerServiceImplTest {

    @InjectMocks
    private FailedOpenURLTrackerServiceImpl openURLTrackerLoggerService;

    @Mock
    private Context context;

    @Mock
    private OpenURLTracker openURLTracker;

    @Mock
    private OpenURLTrackerDAO openURLTrackerDAO;

    /**
     * Tests the remove method
     * @throws SQLException
     */
    @Test
    public void testRemove() throws SQLException {
        openURLTrackerLoggerService.remove(context, openURLTracker);

        Mockito.verify(openURLTrackerDAO, times(1)).delete(context, openURLTracker);

    }

    /**
     * Tests the findAll method
     * @throws SQLException
     */
    @Test
    public void testFindAll() throws SQLException {
        List<OpenURLTracker> trackers = new ArrayList<>();

        when(openURLTrackerDAO.findAll(context, OpenURLTracker.class)).thenReturn(trackers);

        assertEquals("TestFindAll 0", trackers, openURLTrackerLoggerService.findAll(context));
    }

    /**
     * Tests the create method
     * @throws SQLException
     */
    @Test
    public void testCreate() throws SQLException {
        OpenURLTracker tracker = new OpenURLTracker();

        when(openURLTrackerDAO.create(any(), any())).thenReturn(tracker);

        assertEquals("TestCreate 0", tracker, openURLTrackerLoggerService.create(context));
    }


}
