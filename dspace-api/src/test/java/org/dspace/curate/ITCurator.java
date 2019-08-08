/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drive the Curator and check results.
 *
 * @author mhwood
 */
public class ITCurator
        extends AbstractUnitTest {
    Logger LOG = LoggerFactory.getLogger(ITCurator.class);

    public ITCurator() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * The report should contain contributions from all tasks and all curated objects.
     *
     * @throws SQLException passed through.
     * @throws IOException passed through.
     * @throws AuthorizeException passed through.
     */
    @Test
    public void testCurate_Reporting()
            throws SQLException, IOException, AuthorizeException {
        // Configure for testing.
        ConfigurationService cfg = kernelImpl.getConfigurationService();
        cfg.setProperty("plugin.named.org.dspace.curate.CurationTask",
                Task1.class.getName() + " = task1");
        cfg.addPropertyValue("plugin.named.org.dspace.curate.CurationTask",
                Task2.class.getName() + " = task2");

        // Create some structure.
        context.turnOffAuthorisationSystem();
        Site site = ContentServiceFactory.getInstance()
                .getSiteService()
                .findSite(context);
        Community community = ContentServiceFactory.getInstance()
                .getCommunityService()
                .create(null, context);

        // Run some tasks.
        ListReporter reporter = new ListReporter();
        Curator curator = new Curator();
        curator.setReporter(reporter);
        curator.addTask("task1");
        curator.addTask("task2");
        curator.curate(context, site);

        // Validate the results.
        List<String> report = reporter.getReport();
        for (String aReport : report) {
            LOG.info("Report:  {}", aReport);
        }
        Pattern pattern;
        pattern = Pattern.compile(String.format("task1.*%s", site.getHandle()));
        Assert.assertTrue("A report should mention 'task1' and site's handle",
                reportMatcher(report, pattern));
        pattern = Pattern.compile(String.format("task1.*%s", community.getHandle()));
        Assert.assertTrue("A report should mention 'task1' and the community's handle",
                reportMatcher(report, pattern));
        pattern = Pattern.compile(String.format("task2.*%s", site.getHandle()));
        Assert.assertTrue("A report should mention 'task2' and the Site's handle",
                reportMatcher(report, pattern));
        pattern = Pattern.compile(String.format("task2.*%s", community.getHandle()));
        Assert.assertTrue("A report should mention 'task2' and the community's handle",
                reportMatcher(report, pattern));
    }

    /**
     * Match a collection of strings against a regular expression.\
     *
     * @param reports strings to be searched.
     * @param pattern expression to be matched.
     * @return true if at least one string matches the expression.
     */
    private boolean reportMatcher(List<String> reports, Pattern pattern) {
        for (String aReport : reports) {
            if (pattern.matcher(aReport).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dummy curation task for testing.  Reports how it was invoked.
     *
     * @author mhwood
     */
    public static class Task1 extends AbstractCurationTask {
        public Task1() {
        }

        @Override
        public int perform(DSpaceObject dso)
                throws IOException {
            curator.report(String.format(
                    "Task1 received 'perform' on taskId '%s' for object '%s'%n",
                    taskId, dso.getHandle()));
            return Curator.CURATE_SUCCESS;
        }
    }

    /**
     * Dummy curation task for testing.  Reports how it was invoked.
     *
     * @author mhwood
     */
    public static class Task2 extends AbstractCurationTask {
        public Task2() {
        }

        @Override
        public int perform(DSpaceObject dso) throws IOException {
            curator.report(String.format(
                    "Task2 received 'perform' on taskId '%s' for object '%s'%n",
                    taskId, dso.getHandle()));
            return Curator.CURATE_SUCCESS;
        }
    }

    /**
     * Absorb report strings into a sequential collection.
     */
    static class ListReporter
            implements Appendable {
        private final List<String> report = new ArrayList<>();

        /**
         * Get the content of the report accumulator.
         * @return accumulated reports.
         */
        List<String> getReport() {
            return report;
        }

        @Override
        public Appendable append(CharSequence cs)
                throws IOException {
            report.add(cs.toString());
            return this;
        }

        @Override
        public Appendable append(CharSequence cs, int i, int i1)
                throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Appendable append(char c)
                throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
