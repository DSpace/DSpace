/*
 */
package org.datadryad.rest.storage.rdbms;

import org.apache.log4j.Logger;
import org.datadryad.test.ContextUnitTest;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.datadryad.api.DryadJournalConcept;
import org.dspace.core.Context;
import org.dspace.JournalUtils;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class JournalDatabaseStorageImplTest extends ContextUnitTest {
    private static Logger log = Logger.getLogger(JournalDatabaseStorageImplTest.class);
    private static final String TEST_JOURNAL_CODE_1 = "test1";
    private static final String TEST_JOURNAL_NAME_1 = "Test Journal 1";
    private static final String TEST_JOURNAL_CODE_2 = "test2";
    private static final String TEST_JOURNAL_NAME_2 = "Test Journal 2";
    private DryadJournalConcept journalConcept = null;

    @Before
    public void setUp() {
        super.setUp();
        // Create a row in the database
        // Create an journal
        try {
            journalConcept = new DryadJournalConcept();
            journalConcept.setFullName(TEST_JOURNAL_NAME_1);
            journalConcept.setJournalID(TEST_JOURNAL_CODE_1);
            Context context = new Context();
            JournalUtils.addDryadJournalConcept(context, journalConcept);
            context.complete();
        } catch (Exception ex) {
            fail("Exception setting up test journal: " + ex);
        }
    }

    @Override
    public void tearDown() {
        try {
            Context context = new Context();
            JournalUtils.removeDryadJournalConcept(context, journalConcept);
            context.complete();
        } catch (Exception ex) {
            fail("Exception clearing test journal: " + ex);
        }
        super.tearDown();
    }

    /**
     * Test of addAll method, of class JournalDatabaseStorageImpl.
     */
    @Test
    public void testAddAll() throws Exception {
//        log.info("addAll");
//        StoragePath path = new StoragePath();
//        List<Journal> journals = new ArrayList<Journal>();
//        JournalDatabaseStorageImpl instance = new JournalDatabaseStorageImpl();
//        instance.addAll(path, journals);
//        Integer expectedSize = 1;
//        Integer actualSize = journals.size();
//        assertEquals("There should be 1 journal", expectedSize, actualSize);
    }

    /**
     * Test of createObject method, of class JournalDatabaseStorageImpl.
     */
    @Test
    public void testCreateObject() throws Exception {
//        log.info("createObject");
//        StoragePath path = StoragePath.createJournalPath(TEST_JOURNAL_CODE_2);
//        JournalDatabaseStorageImpl instance = new JournalDatabaseStorageImpl();
//        Journal journal = instance.readObject(path);
//        assertNull("Object must not exist before creating", journal);
//        journal = new Journal();
//        journal.journalRef = TEST_JOURNAL_CODE_2;
//        journal.fullName = TEST_JOURNAL_NAME_2;
//        path = new StoragePath();
//        instance.createObject(path, journal);
//        Boolean exists = instance.objectExists(path, journal);
//        assertTrue("Newly saved object should exist", exists);
    }

    @Test
    public void testUpdateObject() throws Exception {
//        log.info("updateObject");
//        JournalDatabaseStorageImpl instance = new JournalDatabaseStorageImpl();
//        StoragePath path = StoragePath.createJournalPath(TEST_JOURNAL_CODE_1);
//        Journal journal = instance.readObject(path);
//        assertNotNull("Object must exist before updating", journal);
//        journal.fullName = TEST_JOURNAL_NAME_2;
//        instance.updateObject(path, journal);
//        journal = instance.readObject(path);
//        assertEquals("Updated object should have updated name", TEST_JOURNAL_NAME_2, journal.fullName);
//        assertEquals("Updated object should have original code", TEST_JOURNAL_CODE_1, journal.journalRef);
    }

    /**
     * Test of readObject method, of class JournalDatabaseStorageImpl.
     */
    @Test
    public void testReadObject() throws Exception {
//        log.info("readObject");
//        // Read object requires full storage path
//        StoragePath path = StoragePath.createJournalPath(TEST_JOURNAL_CODE_1);
//        JournalDatabaseStorageImpl instance = new JournalDatabaseStorageImpl();
//        String expectedName = TEST_JOURNAL_NAME_1;
//        Journal result = instance.readObject(path);
//        String resultName = result.fullName;
//        assertEquals("Read object should have same name as original", expectedName, resultName);
    }

    /**
     * Test of deleteObject method, of class JournalDatabaseStorageImpl.
     */
    @Test
    public void testDeleteObject() throws Exception {
//        log.info("deleteObject");
//        // Delete object requires full storage path
//        StoragePath path = StoragePath.createJournalPath(TEST_JOURNAL_CODE_1);
//        JournalDatabaseStorageImpl instance = new JournalDatabaseStorageImpl();
//        instance.deleteObject(path);
//        Journal dummyJournal = new Journal();
//        dummyJournal.journalRef = TEST_JOURNAL_CODE_1;
//        Boolean exists = instance.objectExists(path, dummyJournal);
//        assertFalse("Deleted object should not exist", exists);
    }
}
