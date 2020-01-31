/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.AbstractIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ScriptService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.export.factory.OpenURLTrackerLoggerServiceFactory;
import org.dspace.statistics.export.service.OpenURLTrackerLoggerService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Class to test the RetryOpenUrlTracker
 */
@RunWith(MockitoJUnitRunner.class)
public class ITRetryOpenUrlTracker extends AbstractIntegrationTest {

    private static Logger log = Logger.getLogger(ITRetryOpenUrlTracker.class);


    protected OpenURLTrackerLoggerService openURLTrackerLoggerService =
            OpenURLTrackerLoggerServiceFactory.getInstance().getOpenUrlTrackerLoggerService();

    protected ArrayList testProcessedUrls = DSpaceServicesFactory.getInstance().getServiceManager()
                                                       .getServiceByName("testProcessedUrls", ArrayList.class);

    private ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();


    /**
     * Clean up the logged entries from the db after each test
     */
    @After
    @Override
    public void destroy() {
        try {
            context.turnOffAuthorisationSystem();

            List<OpenURLTracker> all = openURLTrackerLoggerService.findAll(context);
            for (OpenURLTracker tracker : all) {
                openURLTrackerLoggerService.remove(context, tracker);
            }

            // Clear the list of processedUrls
            testProcessedUrls.clear();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            context.restoreAuthSystemState();
        }
        super.destroy();
    }

    /**
     * Test the mode of the script that allows the user to add a failed url to the database
     * @throws Exception
     */
    @Test
    public void testAddNewFailedUrl() throws Exception {

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        DSpaceRunnable retryOpenUrlTracker = scriptService.getScriptForName("retry-tracker");
        String urlToAdd = "test-failed-url";
        String[] args = {"-a", urlToAdd};

        retryOpenUrlTracker.initialize(args, testDSpaceRunnableHandler);
        retryOpenUrlTracker.internalRun();

        List<OpenURLTracker> all = openURLTrackerLoggerService.findAll(context);

        assertThat(testProcessedUrls.size(), is(0));
        assertThat(all.size(), is(1));
        assertThat(all.get(0).getUrl(), is(urlToAdd));
    }

    /**
     * Test to check that all logged failed urls are reprocessed succesfully and removed from the db
     * @throws Exception
     */
    @Test
    public void testReprocessAllUrls() throws Exception {

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        DSpaceRunnable retryOpenUrlTracker = scriptService.getScriptForName("retry-tracker");
        String[] args = {};

        OpenURLTracker tracker1 = openURLTrackerLoggerService.create(context);
        tracker1.setUrl("test-url-1");
        OpenURLTracker tracker2 = openURLTrackerLoggerService.create(context);
        tracker2.setUrl("test-url-2");
        OpenURLTracker tracker3 = openURLTrackerLoggerService.create(context);
        tracker3.setUrl("test-url-3");


        retryOpenUrlTracker.initialize(args, testDSpaceRunnableHandler);
        retryOpenUrlTracker.internalRun();

        List<OpenURLTracker> all = openURLTrackerLoggerService.findAll(context);

        assertThat(testProcessedUrls.size(), is(3));
        assertThat(testProcessedUrls.contains("test-url-1"), is(true));
        assertThat(testProcessedUrls.contains("test-url-2"), is(true));
        assertThat(testProcessedUrls.contains("test-url-3"), is(true));

        assertThat(all.size(), is(0));
    }

    /**
     * Test to check that the successful retries are removed, but the failed retries remain in the db
     * @throws Exception
     */
    @Test
    public void testReprocessPartOfUrls() throws Exception {

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        DSpaceRunnable retryOpenUrlTracker = scriptService.getScriptForName("retry-tracker");
        String[] args = {};

        OpenURLTracker tracker1 = openURLTrackerLoggerService.create(context);
        tracker1.setUrl("test-url-1");
        OpenURLTracker tracker2 = openURLTrackerLoggerService.create(context);
        tracker2.setUrl("test-url-2-fail");
        OpenURLTracker tracker3 = openURLTrackerLoggerService.create(context);
        tracker3.setUrl("test-url-3-fail");
        OpenURLTracker tracker4 = openURLTrackerLoggerService.create(context);
        tracker4.setUrl("test-url-4-fail");
        OpenURLTracker tracker5 = openURLTrackerLoggerService.create(context);
        tracker5.setUrl("test-url-5");


        retryOpenUrlTracker.initialize(args, testDSpaceRunnableHandler);
        retryOpenUrlTracker.internalRun();

        List<OpenURLTracker> all = openURLTrackerLoggerService.findAll(context);
        List<String> storedTrackerUrls = new ArrayList<>();
        for (OpenURLTracker tracker : all) {
            storedTrackerUrls.add(tracker.getUrl());
        }

        assertThat(testProcessedUrls.size(), is(2));
        assertThat(testProcessedUrls.contains("test-url-1"), is(true));
        assertThat(testProcessedUrls.contains("test-url-5"), is(true));

        assertThat(all.size(), is(3));
        assertThat(storedTrackerUrls.contains("test-url-2-fail"), is(true));
        assertThat(storedTrackerUrls.contains("test-url-3-fail"), is(true));
        assertThat(storedTrackerUrls.contains("test-url-4-fail"), is(true));
    }


}
