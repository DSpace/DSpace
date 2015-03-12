/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.datadryad.api;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.dspace.core.Context;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 *
 * @author Nathan Day
 */
public class DryadJournalTest {
    private static final String testJournalName = "Evolution";
    
    private DryadJournal dryadJournal;
    private Context context;
    
    @Before
    public void setUp() {
        try {
            context = new Context();
            dryadJournal = new DryadJournal(context, testJournalName);
        } catch (SQLException ex) {}
    }
    @After
    public void tearDown() {
        try {
            context.complete();
        } catch (SQLException ex) {}
    }
    
    /**
     * Test of getArchivedDataFiles method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedDataFiles() throws Exception {
        List<Integer> expResult = Arrays.asList(107333);
        List<Integer> result = dryadJournal.getArchivedDataFiles();
        assertEquals(expResult, result);
    }

    /**
     * Test of getArchivedPackagesCount method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedPackagesCount() {
        int expResult = 1;
        int result = dryadJournal.getArchivedPackagesCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of getArchivedPackagesSortedRecent method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedPackagesSortedRecent() throws Exception {
        int max = 10;
        Item item = Item.find(context, 107332);
        List<Item> expResult = Arrays.asList(item);
        List<Item> result = dryadJournal.getArchivedPackagesSortedRecent(max);
        assertEquals(expResult, result);
    }

    /**
     * Test of getRequestsPerJournal method, of class DryadJournal.
     */
    @Ignore("No Solr service running on test system")
    @Test
    public void testGetRequestsPerJournal() {
        /*
        String facetQueryField = "";
        String time = "";
        int max = 0;
        LinkedHashMap<Item, String> expResult = null;
        LinkedHashMap<Item, String> result = dryadJournal.getRequestsPerJournal(facetQueryField, time, max);
        assertEquals(expResult, result);
        */
    }
    
}
