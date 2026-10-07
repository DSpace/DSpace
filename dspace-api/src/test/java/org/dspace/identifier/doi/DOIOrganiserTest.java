/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.dspace.AbstractUnitTest;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the batch processing of {@link DOIOrganiser}, which drives the bulk options of the
 * <code>doi-organiser</code> command line tool ({@code -r}, {@code -s}, {@code -u} and {@code -d}).
 *
 * <p>The batching queries the DOIs to process in windows of a fixed size. Successfully processed
 * DOIs change their status and drop out of the query, so the paging window must only move past the
 * DOIs that could not be processed. Getting that wrong breaks in one of two ways, both of which
 * are covered here: always querying from offset 0 makes the loop run forever as soon as a single
 * DOI cannot be processed, while moving the offset by the full batch size skips over the DOIs that
 * moved into the previous window.
 *
 * @author Bram Luyten
 */
public class DOIOrganiserTest extends AbstractUnitTest {

    private static final String PREFIX = "10.5072";
    private static final String NAMESPACE_SEPARATOR = "dspaceUnitTests-";

    /**
     * The statuses queried by the bulk registration option of the command line tool.
     */
    private static final List<Integer> QUEUED_FOR_REGISTRATION =
        Arrays.asList(DOIIdentifierProvider.TO_BE_REGISTERED);

    /**
     * Upper bound on the number of times the tested operation may be invoked. Every test below
     * queues a handful of DOIs, so exceeding this means that the loop keeps handing out the same
     * DOIs over and over and would never terminate.
     */
    private static final int MAX_INVOCATIONS = 50;

    protected DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();

    /**
     * The DOIs created by the running test, in creation order. As they are created in one
     * transaction, this is also the order of their ids and therefore the order in which
     * {@link DOIService#getDOIsByStatus(Context, List, int, int)} returns them.
     */
    private final List<DOI> createdDOIs = new ArrayList<>();

    /**
     * The DOIs of the running test that the operation was invoked for, in invocation order.
     */
    private final List<String> attemptedDOIs = new ArrayList<>();

    /**
     * Number of operation invocations, including any DOI that another test may have left behind.
     */
    private int invocations;

    /**
     * Makes the DOIs of the running test recognizable, so that the assertions ignore any DOI that
     * another test class may have committed to the shared test database.
     */
    private String testRunId;

    @Before
    @Override
    public void init() {
        super.init();
        testRunId = UUID.randomUUID().toString();
        createdDOIs.clear();
        attemptedDOIs.clear();
        invocations = 0;
    }

    @After
    @Override
    public void destroy() {
        // The batch processing commits after every DOI, so the test data survives the rollback
        // that AbstractUnitTest performs and has to be removed explicitly.
        if (!createdDOIs.isEmpty()) {
            try {
                for (DOI doi : createdDOIs) {
                    DOI reloadedDOI = context.reloadEntity(doi);
                    if (null != reloadedDOI) {
                        doiService.delete(context, reloadedDOI);
                    }
                }
                context.commit();
            } catch (SQLException ex) {
                fail("Could not clean up the DOIs created by this test: " + ex.getMessage());
            }
        }
        createdDOIs.clear();
        super.destroy();
    }

    /**
     * A run in which every DOI can be processed must process all of them, even when they span
     * several batches. Successful DOIs leave the query result, so moving the offset by the batch
     * size as well would skip the DOIs that took their place in the paging window.
     *
     * @throws SQLException if database error
     */
    @Test
    public void testProcessBatchedProcessesEveryQueuedDOI() throws SQLException {
        List<DOI> queuedDOIs = createQueuedDOIs(5);

        DOIOrganiser.processBatched(context, doiService, QUEUED_FOR_REGISTRATION,
                                    registrationSimulation(Collections.emptyList(), false),
                                    "registration", 2);

        assertEquals("Every queued DOI must be attempted exactly once",
                     doisOf(queuedDOIs), attemptedDOIs);
        for (DOI doi : queuedDOIs) {
            assertStatus("A processed DOI must not be queued anymore",
                         doi, DOIIdentifierProvider.IS_REGISTERED);
        }
    }

    /**
     * A DOI that cannot be processed keeps its status and is returned by the next query again.
     * The loop has to skip it instead of handing it out forever, while still processing the DOIs
     * queued behind it.
     *
     * @throws SQLException if database error
     */
    @Test
    public void testProcessBatchedSkipsDOIsThatFailWithoutThrowing() throws SQLException {
        List<DOI> queuedDOIs = createQueuedDOIs(5);
        List<String> failingDOIs = Arrays.asList(queuedDOIs.get(1).getDoi(), queuedDOIs.get(3).getDoi());

        DOIOrganiser.processBatched(context, doiService, QUEUED_FOR_REGISTRATION,
                                    registrationSimulation(failingDOIs, false),
                                    "registration", 2);

        assertEquals("Every queued DOI must be attempted exactly once, including the failing ones",
                     doisOf(queuedDOIs), attemptedDOIs);
        for (DOI doi : queuedDOIs) {
            if (failingDOIs.contains(doi.getDoi())) {
                assertStatus("A DOI that could not be processed stays queued for the next run",
                             doi, DOIIdentifierProvider.TO_BE_REGISTERED);
            } else {
                assertStatus("A DOI queued behind a failing one must still be processed",
                             doi, DOIIdentifierProvider.IS_REGISTERED);
            }
        }
    }

    /**
     * The same, for operations that report their failure by throwing: the DOI is rolled back and
     * keeps its status, so it has to be skipped as well.
     *
     * @throws SQLException if database error
     */
    @Test
    public void testProcessBatchedSkipsDOIsThatFailByThrowing() throws SQLException {
        List<DOI> queuedDOIs = createQueuedDOIs(5);
        List<String> failingDOIs = Arrays.asList(queuedDOIs.get(0).getDoi(), queuedDOIs.get(4).getDoi());

        DOIOrganiser.processBatched(context, doiService, QUEUED_FOR_REGISTRATION,
                                    registrationSimulation(failingDOIs, true),
                                    "registration", 2);

        assertEquals("Every queued DOI must be attempted exactly once, including the failing ones",
                     doisOf(queuedDOIs), attemptedDOIs);
        for (DOI doi : queuedDOIs) {
            if (failingDOIs.contains(doi.getDoi())) {
                assertStatus("A DOI whose processing threw stays queued for the next run",
                             doi, DOIIdentifierProvider.TO_BE_REGISTERED);
            } else {
                assertStatus("A DOI queued behind a failing one must still be processed",
                             doi, DOIIdentifierProvider.IS_REGISTERED);
            }
        }
    }

    /**
     * The worst case of the reported issue: none of the queued DOIs can be processed. The run has
     * to end after having attempted each of them once, instead of querying, processing and mailing
     * about the same DOIs forever.
     *
     * @throws SQLException if database error
     */
    @Test
    public void testProcessBatchedTerminatesWhenNoDOICanBeProcessed() throws SQLException {
        List<DOI> queuedDOIs = createQueuedDOIs(5);

        DOIOrganiser.processBatched(context, doiService, QUEUED_FOR_REGISTRATION,
                                    registrationSimulation(doisOf(queuedDOIs), false),
                                    "registration", 2);

        assertEquals("Every queued DOI must be attempted exactly once",
                     doisOf(queuedDOIs), attemptedDOIs);
        for (DOI doi : queuedDOIs) {
            assertStatus("A DOI that could not be processed stays queued for the next run",
                         doi, DOIIdentifierProvider.TO_BE_REGISTERED);
        }
    }

    /**
     * An empty queue must end the run instead of starting it.
     *
     * @throws SQLException if database error
     */
    @Test
    public void testProcessBatchedWithNothingQueued() throws SQLException {
        DOIOrganiser.processBatched(context, doiService, QUEUED_FOR_REGISTRATION,
                                    registrationSimulation(Collections.emptyList(), false),
                                    "registration", 2);

        assertTrue("Nothing was queued, so no DOI of this test may be attempted",
                   attemptedDOIs.isEmpty());
    }

    /**
     * The premise the skipping relies on: an operation can fail without throwing.
     * {@link DOIOrganiser#register(DOI)} reports an {@link org.dspace.identifier.IdentifierException}
     * from the connector by log entry and alert mail and then returns normally, leaving the status
     * of the DOI untouched. The batch processing can therefore not use an exception to recognize a
     * DOI that made no progress.
     *
     * @throws Exception passed through.
     */
    @Test
    public void testRegisterReportsConnectorFailureWithoutChangingTheStatus() throws Exception {
        Item item = mock(Item.class);
        when(item.getType()).thenReturn(Constants.ITEM);
        when(item.getID()).thenReturn(UUID.randomUUID());

        DOI doi = mock(DOI.class);
        when(doi.getDoi()).thenReturn(PREFIX + "/" + NAMESPACE_SEPARATOR + testRunId);
        when(doi.getDSpaceObject()).thenReturn(item);

        DOIIdentifierProvider provider = mock(DOIIdentifierProvider.class);
        doThrow(new DOIIdentifierException("Simulated failure of the DOI connector",
                                           DOIIdentifierException.INTERNAL_ERROR))
            .when(provider).registerOnline(any(Context.class), any(DSpaceObject.class), anyString(), any());

        // Must not throw: the exception is reported, but not passed on to the caller.
        new DOIOrganiser(context, provider).register(doi);

        verify(doi, never()).setStatus(any());
    }

    /**
     * Create DOIs queued for registration and commit them. The DOIs are not attached to an item:
     * the batch processing under test only queries and compares their status.
     *
     * @param count the number of DOIs to queue.
     * @return the created DOIs, in creation order.
     * @throws SQLException if database error
     */
    private List<DOI> createQueuedDOIs(int count) throws SQLException {
        for (int i = 0; i < count; i++) {
            DOI doi = doiService.create(context);
            doi.setDoi(PREFIX + "/" + NAMESPACE_SEPARATOR + testRunId + "-" + i);
            doi.setStatus(DOIIdentifierProvider.TO_BE_REGISTERED);
            doiService.update(context, doi);
            createdDOIs.add(doi);
        }
        // Commit the test data: the batch processing rolls back when an operation throws, which
        // would otherwise undo the creation of the DOIs we are about to process.
        context.commit();
        return new ArrayList<>(createdDOIs);
    }

    /**
     * An operation that behaves like {@link DOIOrganiser#register(DOI)}: the given DOIs are left
     * in their queued status, either silently (as register() does when the connector fails) or by
     * throwing, while all other DOIs are marked as registered.
     *
     * @param failingDOIs the DOIs that cannot be processed.
     * @param byThrowing  whether the failing DOIs report their failure by throwing.
     * @return the operation to hand to the batch processing.
     */
    private DOIOrganiser.DOIOperation registrationSimulation(List<String> failingDOIs, boolean byThrowing) {
        return doi -> {
            invocations++;
            if (invocations > MAX_INVOCATIONS) {
                throw new AssertionError("processBatched() does not terminate: it performed "
                                             + invocations + " operations for the "
                                             + createdDOIs.size() + " DOIs queued by this test.");
            }
            if (!isFromThisTest(doi)) {
                // A DOI committed by another test class: leave it untouched.
                return;
            }
            attemptedDOIs.add(doi.getDoi());
            if (failingDOIs.contains(doi.getDoi())) {
                if (byThrowing) {
                    throw new DOIIdentifierException("Simulated failure of the DOI connector",
                                                     DOIIdentifierException.INTERNAL_ERROR);
                }
                // Report the failure the way register() does, without changing the status.
                return;
            }
            doi.setStatus(DOIIdentifierProvider.IS_REGISTERED);
            doiService.update(context, doi);
        };
    }

    private boolean isFromThisTest(DOI doi) {
        return null != doi.getDoi() && doi.getDoi().contains(testRunId);
    }

    private List<String> doisOf(List<DOI> dois) {
        List<String> identifiers = new ArrayList<>(dois.size());
        for (DOI doi : dois) {
            identifiers.add(doi.getDoi());
        }
        return identifiers;
    }

    private void assertStatus(String message, DOI doi, Integer expectedStatus) throws SQLException {
        DOI reloadedDOI = context.reloadEntity(doi);
        assertNotNull("DOI " + doi.getDoi() + " disappeared from the database", reloadedDOI);
        assertEquals(message + " (" + doi.getDoi() + ")", expectedStatus, reloadedDOI.getStatus());
    }
}
