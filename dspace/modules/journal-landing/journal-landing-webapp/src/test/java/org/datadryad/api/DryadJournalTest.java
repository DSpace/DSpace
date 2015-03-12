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
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.datadryad.test.ContextUnitTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 *
 * @author Nathan Day
 */
public class DryadJournalTest extends ContextUnitTest {
    private static Logger log = Logger.getLogger(DryadJournalTest.class);
    private static final String testJournalName = "Evolution";
    private DryadJournal dryadJournal;
    
    @Before
    public void setUp() {
        super.setUp();
        dryadJournal = new DryadJournal(this.context, testJournalName);
    }
    @After
    public void tearDown() {
        super.tearDown();
    }
    
    // override beforeClass, afterClass to avoid kernel setup
    @BeforeClass
    public static void setupClass() {}
    @AfterClass
    public static void tearDownClass() {}
    
    /**
     * Test of getArchivedDataFiles method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedDataFiles() throws Exception {
        log.debug("getArchivedDataFiles");        
        List<Integer> expResult = Arrays.asList(107333);
        List<Integer> result = dryadJournal.getArchivedDataFiles();
        assertEquals(expResult, result);
    }

    /**
     * Test of getArchivedPackagesCount method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedPackagesCount() {
        log.debug("getArchivedPackagesCount");
        int expResult = 1;
        int result = dryadJournal.getArchivedPackagesCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of getArchivedPackagesSortedRecent method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedPackagesSortedRecent() throws Exception {
        log.debug("getArchivedPackagesSortedRecent");
        int max = 10;
        Item item = Item.find(this.context, 107332);
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
        log.debug("getRequestsPerJournal");
        String facetQueryField = "";
        String time = "";
        int max = 0;
        LinkedHashMap<Item, String> expResult = null;
        LinkedHashMap<Item, String> result = dryadJournal.getRequestsPerJournal(facetQueryField, time, max);
        assertEquals(expResult, result);
        */
    }
    
}
