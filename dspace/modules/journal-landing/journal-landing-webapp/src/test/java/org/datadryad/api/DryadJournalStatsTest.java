/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.datadryad.api;

import org.dspace.JournalUtils;
import org.dspace.app.xmlui.aspect.journal.landing.Const;
import org.dspace.app.xmlui.aspect.journal.landing.JournalLandingBaseTest;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * These are integration tests assuming that the sql script has
 * been run on the test machine:
 *
 *   ./dryad-repo/test/etc/postgres/test-journal-landing.sql
 *
 * @author Nathan Day
 */
public class DryadJournalStatsTest extends JournalLandingBaseTest
{

    private static final String testJournalName = "Evolution";
    private static final String badJournalName = "noitulovE";

    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testGetArchivedDataFiles() {
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(3);
        List<Integer> result = null;
        try {
            result = DryadJournalStats.getArchivedDataFiles(context, testJournalName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(expected, result);
    }
    @Test
    public void testGetArchivedDataFilesNoJournal() throws MalformedURLException, SQLException {
        List<Integer> expected = new ArrayList<Integer>();
        List<Integer> result = DryadJournalStats.getArchivedDataFiles(context, badJournalName);
        assertEquals(expected, result);
    }

    @Test
    public void testGetArchivedPackagesCount() throws SQLException {
        long result = DryadJournalStats.getArchivedPackagesCount(context, testJournalName);
        assertEquals(result, 2);
    }

    @Test
    public void testGetArchivedPackagesCountNoJournal() throws SQLException {
        long result = DryadJournalStats.getArchivedPackagesCount(context, badJournalName);
        assertEquals(result, 0);
    }

    @Test
    public void testGetArchivedPackagesSortedRecent() throws Exception {
        Map<Item, String> result = JournalUtils.getArchivedPackagesSortedRecent(context, testJournalName, 10);
        assertEquals(result.size(), 2);
        String dda = result.entrySet().iterator().next().getValue();
        assertEquals(dda, "2015-02-25");
    }

    @Test
    public void testGetArchivedPackagesSortedRecentNoJournal() throws SQLException {
        SimpleDateFormat fmt = null;
        Map<Item,String> result = JournalUtils.getArchivedPackagesSortedRecent(context, badJournalName, 10);
        assertEquals(result.size(), 0);
    }
}

