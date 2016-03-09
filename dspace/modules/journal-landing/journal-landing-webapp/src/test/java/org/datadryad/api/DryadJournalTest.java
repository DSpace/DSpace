/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.datadryad.api;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.xmlui.aspect.journal.landing.JournalLandingBaseTest;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * These are integration tests assuming that the sql script has
 * been run on the test machine:
 *
 *   ./dryad-repo/test/etc/postgres/test-journal-landing.sql
 *
 * @author Nathan Day
 */
public class DryadJournalTest extends JournalLandingBaseTest {

    private static final String testJournalName = "Evolution";
    private static final String badJournalName = "noitulovE";

    private static final String solrDatePastMonth = ConfigurationManager.getProperty("landing-page.stats.query.month");
    private static final String solrDatePastYear  = ConfigurationManager.getProperty("landing-page.stats.query.year");
    private static final String solrDateAllTime   = ConfigurationManager.getProperty("landing-page.stats.query.alltime");
    private static final String facetQuery        = ConfigurationManager.getProperty("landing-page.stats.query.facet");

    @Before
    public void setUp() {
        super.setUp();
    }

    /**
     * Test of getArchivedDataFiles method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedDataFiles() {
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(3);
        List<Integer> result = null;
        try {
            //result = DryadJournalStats.getArchivedDataFiles(context, testJournalName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(expected, result);
    }
    @Test
    public void testGetArchivedDataFilesNoJournal() throws MalformedURLException, SQLException {
        List<Integer> expected = new ArrayList<Integer>();
        List<Integer> result = null;
        //result = DryadJournalStats.getArchivedDataFiles(badJournalName);
        assertEquals(expected, result);
    }

    /**
     * Test of getArchivedPackagesCount method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedPackagesCount() throws SQLException {
        long result = DryadJournalStats.getArchivedPackagesCount(context, testJournalName);
        assertEquals(result, 2);
    }

    /**
     * Test of getArchivedPackagesCount method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedPackagesCountNoJournal() throws SQLException {
        long result = DryadJournalStats.getArchivedPackagesCount(context, badJournalName);
        assertEquals(result, 0);
    }

    /**
     * Test of getArchivedPackagesSortedRecent method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedPackagesSortedRecent() throws Exception {
        List<Item> result = null;
        // List<Item> result = DryadJournalStats.getArchivedPackagesSortedRecent(context, testJournalName,10);
        assertEquals(result.size(), 2);
        String dda = result.get(0).getMetadata("dc", "date", "accessioned", null)[0].value;
        assertEquals(dda, "2015-02-25T19:32:20Z");
    }

    /**
     * Test of getArchivedPackagesSortedRecent method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedPackagesSortedRecentNoJournal() throws SQLException {
        SimpleDateFormat fmt = null;
        LinkedHashMap<Item,String> result = DryadJournalStats.getArchivedPackagesSortedRecent(null, badJournalName, fmt, 10);
        assertEquals(result.size(), 0);
    }

    /**
     * Test of getRequestsPerJournal method, of class DryadJournal.
     */
    @Test
    public void testGetRequestsPerJournal() throws SQLException, FileNotFoundException, SolrServerException, MalformedURLException {
        String facetQueryField = facetQuery;
        int max = 10;
        String path = this.getClass().getResource("/solr.1.xml").getFile();
        LinkedHashMap<Item, String> expected = new LinkedHashMap<Item, String>();
        LinkedHashMap<Item, String> result = null;
        expected.put(Item.find(context,1),"888");
        expected.put(Item.find(context,2),"831");
        setQueryResponse(path, "utf-8") ;
        // result = DryadJournalStats.getRequestsPerJournal(testJournalName, facetQueryField, solrDateAllTime, max);
        assertEquals(expected, result);
    }

    /**
     * Test of getRequestsPerJournal method, of class DryadJournal.
     */
    @Test
    public void testGetRequestsPerJournalNone() throws FileNotFoundException, SolrServerException, MalformedURLException {
        String facetQueryField = facetQuery;
        int max = 10;
        String path = this.getClass().getResource("/solr.2.xml").getFile();
        LinkedHashMap<Item, String> expected = new LinkedHashMap<Item, String>();
        LinkedHashMap<Item, String> result = null;
        setQueryResponse(path, "utf-8") ;
        //result = DryadJournalStats.getRequestsPerJournal(testJournalName, facetQueryField, solrDateAllTime, max);
        assertEquals(expected, result);
    }
}

