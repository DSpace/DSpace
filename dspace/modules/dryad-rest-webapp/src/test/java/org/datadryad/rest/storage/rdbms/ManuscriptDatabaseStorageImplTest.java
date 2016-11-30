/*
 */
package org.datadryad.rest.storage.rdbms;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Journal;
import org.datadryad.rest.storage.StoragePath;
import org.datadryad.test.ContextUnitTest;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
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
public class ManuscriptDatabaseStorageImplTest extends ContextUnitTest {
    private static Logger log = Logger.getLogger(ManuscriptDatabaseStorageImplTest.class);
    private static final String TEST_JOURNAL_CODE = "test";
    private static final String TEST_JOURNAL_NAME = "Test Journal";
    private static final String TEST_MANUSCRIPT_ID_1 = "MS_TEST_12345";
    private static final String TEST_MANUSCRIPT_ID_2 = "MS_TEST_99999";

    private StoragePath collectionPath = new StoragePath();
    private StoragePath manuscriptPath1 = new StoragePath();
    private StoragePath manuscriptPath2 = new StoragePath();

    public ManuscriptDatabaseStorageImplTest() {
        collectionPath = StoragePath.createJournalPath(TEST_JOURNAL_CODE);
        manuscriptPath1 = StoragePath.createManuscriptPath(TEST_JOURNAL_CODE, TEST_MANUSCRIPT_ID_1);
        manuscriptPath2 = StoragePath.createManuscriptPath(TEST_JOURNAL_CODE, TEST_MANUSCRIPT_ID_2);
    }

    @Before
    public void setUp() {
        super.setUp();
        // Create a journal
        Journal journal = null;
        try {
            DryadJournalConcept journalConcept = new DryadJournalConcept();
            journalConcept.setFullName(TEST_JOURNAL_NAME);
            journalConcept.setJournalID(TEST_JOURNAL_CODE);
            Context context = new Context();
            JournalUtils.addDryadJournalConcept(context, journalConcept);
            journal = JournalDatabaseStorageImpl.getJournalByCodeOrISSN(context, TEST_JOURNAL_CODE);
            context.complete();
        } catch (Exception ex) {
            fail("Exception setting up test journal: " + ex);
        }

        // Create a manuscript
        Manuscript manuscript = new Manuscript();
        manuscript.configureTestValues();
        manuscript.setManuscriptId(TEST_MANUSCRIPT_ID_1);
        try {
            DatabaseManager.deleteByValue(context, ManuscriptDatabaseStorageImpl.MANUSCRIPT_TABLE, ManuscriptDatabaseStorageImpl.COLUMN_MSID, TEST_MANUSCRIPT_ID_1);
            TableRow manuscriptRow = ManuscriptDatabaseStorageImpl.tableRowFromManuscript(manuscript, journal.conceptID);
            manuscriptRow.setColumn(ManuscriptDatabaseStorageImpl.COLUMN_VERSION, 1);
            manuscriptRow.setColumn(ManuscriptDatabaseStorageImpl.COLUMN_ACTIVE, ManuscriptDatabaseStorageImpl.ACTIVE_TRUE);
            DatabaseManager.insert(context, manuscriptRow);
            context.commit();
        } catch (Exception ex) {
            fail("Exception setting up test manuscript: " + ex);
        }
    }

    @Override
    public void tearDown() {
        try {
            DatabaseManager.deleteByValue(context, ManuscriptDatabaseStorageImpl.MANUSCRIPT_TABLE, ManuscriptDatabaseStorageImpl.COLUMN_MSID, TEST_MANUSCRIPT_ID_1);
            DatabaseManager.deleteByValue(context, ManuscriptDatabaseStorageImpl.MANUSCRIPT_TABLE, ManuscriptDatabaseStorageImpl.COLUMN_MSID, TEST_MANUSCRIPT_ID_2);
            DatabaseManager.deleteByValue(context, JournalDatabaseStorageImpl.JOURNAL_TABLE, JournalDatabaseStorageImpl.COLUMN_CODE, TEST_JOURNAL_CODE);
        } catch (SQLException ex) {
            fail("Exception clearing test journal and manuscript: " + ex);
        }
        super.tearDown();
    }

    /**
     * Test of objectExists method, of class ManuscriptDatabaseStorageImpl.
     */
    @Test
    public void testObjectExists() throws Exception {
        log.info("objectExists");
        StoragePath path = collectionPath;
        Manuscript manuscript = new Manuscript();
        manuscript.setManuscriptId(TEST_MANUSCRIPT_ID_1);
        ManuscriptDatabaseStorageImpl instance = new ManuscriptDatabaseStorageImpl();
        Boolean expResult = Boolean.TRUE;
        Boolean result = instance.objectExists(path, manuscript);
        assertEquals("Test manuscript should exist", expResult, result);
    }

    /**
     * Test of addAll method, of class ManuscriptDatabaseStorageImpl.
     */
    @Test
    public void testAddAll() throws Exception {
        log.info("addAll");
        StoragePath path = collectionPath;
        List<Manuscript> manuscripts = new ArrayList<Manuscript>();
        ManuscriptDatabaseStorageImpl instance = new ManuscriptDatabaseStorageImpl();
        instance.addAll(path, manuscripts);
        Integer expectedSize = 1;
        Integer actualSize = manuscripts.size();
        assertEquals("There should be 1  manuscript", expectedSize, actualSize);
    }

    /**
     * Test of createObject method, of class ManuscriptDatabaseStorageImpl.
     */
    @Test
    public void testCreateObject() throws Exception {
        log.info("createObject");
        StoragePath path = manuscriptPath2;
        ManuscriptDatabaseStorageImpl instance = new ManuscriptDatabaseStorageImpl();
        Manuscript manuscript = instance.readObject(path);
        assertNull("Object must not exist before creating", manuscript);
        manuscript = new Manuscript();
        manuscript.configureTestValues();
        manuscript.setManuscriptId(TEST_MANUSCRIPT_ID_2);
        path = collectionPath;
        instance.createObject(path, manuscript);
        Boolean exists = instance.objectExists(path, manuscript);
        assertTrue("Newly saved object should exist", exists);
    }

    /**
     * Test of readObject method, of class ManuscriptDatabaseStorageImpl.
     */
    @Test
    public void testReadObject() throws Exception {
        log.info("readObject");
        StoragePath path = manuscriptPath1;
        ManuscriptDatabaseStorageImpl instance = new ManuscriptDatabaseStorageImpl();
        String expManuscriptId = TEST_MANUSCRIPT_ID_1;
        Manuscript result = instance.readObject(path);
        assertEquals("Read object should have same id as original", expManuscriptId, result.getManuscriptId());
    }

    /**
     * Test of deleteObject method, of class ManuscriptDatabaseStorageImpl.
     */
    @Test
    public void testDeleteObject() throws Exception {
        log.info("deleteObject");
        StoragePath path = manuscriptPath1;
        ManuscriptDatabaseStorageImpl instance = new ManuscriptDatabaseStorageImpl();
        instance.deleteObject(path);
        Manuscript dummyManuscript = new Manuscript();
        dummyManuscript.setManuscriptId(TEST_MANUSCRIPT_ID_1);
        dummyManuscript.setStatus(Manuscript.STATUS_ACCEPTED);
        Boolean exists = instance.objectExists(path, dummyManuscript);
        assertFalse("Deleted object should not exist", exists);
    }

    /**
     * Test of updateObject method, of class ManuscriptDatabaseStorageImpl.
     */
    @Test
    public void testUpdateObject() throws Exception {
        log.info("updateObject");
        StoragePath path = manuscriptPath1;
        Manuscript manuscript = new Manuscript();
        manuscript.configureTestValues();
        manuscript.setManuscriptId(TEST_MANUSCRIPT_ID_1);
        String updatedTitle = "Updated Title";
        manuscript.setTitle(updatedTitle);
        ManuscriptDatabaseStorageImpl instance = new ManuscriptDatabaseStorageImpl();
        instance.updateObject(path, manuscript);
        manuscript = instance.readObject(path);
        assertEquals(updatedTitle, manuscript.getTitle());

        // version should change internally!
        String query = "SELECT * FROM MANUSCRIPT where msid = ? and active = ?";
        TableRow row = DatabaseManager.querySingleTable(context, ManuscriptDatabaseStorageImpl.MANUSCRIPT_TABLE, query, manuscript.getManuscriptId(), ManuscriptDatabaseStorageImpl.ACTIVE_TRUE);
    }
}
