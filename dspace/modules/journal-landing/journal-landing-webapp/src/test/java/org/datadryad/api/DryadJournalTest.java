/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.datadryad.api;

import java.util.LinkedHashMap;
import java.util.List;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.datadryad.test.ContextUnitTest;

/**
 *
 * @author Nathan Day
 */
public class DryadJournalTest extends ContextUnitTest {
    private static Logger log = Logger.getLogger(DryadJournalTest.class);
    private static final testJournal = "Evolution";
    
    /**
     * Test of getArchivedDataFiles method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedDataFiles() throws Exception {
        log.debug("getArchivedDataFiles");        
        DryadJournal dryadJournal = new DryadJournal(context,testJournal);
        List<Integer> expResult = null;
        List<Integer> result = dryadJournal.getArchivedDataFiles();
        assertEquals(expResult, result);
    }

    /**
     * Test of getArchivedPackagesCount method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedPackagesCount() {
        log.debug("getArchivedPackagesCount");
        DryadJournal instance = null;
        int expResult = 0;
        int result = instance.getArchivedPackagesCount();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getArchivedPackagesSortedRecent method, of class DryadJournal.
     */
    @Test
    public void testGetArchivedPackagesSortedRecent() throws Exception {
        log.debug("getArchivedPackagesSortedRecent");
        int max = 0;
        DryadJournal instance = null;
        List<Item> expResult = null;
        List<Item> result = instance.getArchivedPackagesSortedRecent(max);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getRequestsPerJournal method, of class DryadJournal.
     */
    @Test
    public void testGetRequestsPerJournal() {
        log.debug("getRequestsPerJournal");
        String facetQueryField = "";
        String time = "";
        int max = 0;
        DryadJournal instance = null;
        LinkedHashMap<Item, String> expResult = null;
        LinkedHashMap<Item, String> result = instance.getRequestsPerJournal(facetQueryField, time, max);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
