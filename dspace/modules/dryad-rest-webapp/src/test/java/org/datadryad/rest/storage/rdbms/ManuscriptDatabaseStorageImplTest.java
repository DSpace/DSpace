/*
 */
package org.datadryad.rest.storage.rdbms;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.StoragePath;
import org.datadryad.test.ContextUnitTest;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptDatabaseStorageImplTest extends ContextUnitTest {
    private static Logger log = Logger.getLogger(ManuscriptDatabaseStorageImplTest.class);
    private static final String TEST_ORGANIZATION_CODE = "test";
    private static final String TEST_ORGANIZATION_NAME = "Test Organization";
    private static final String TEST_MANUSCRIPT_ID_1 = "MS_TEST_12345";
    private static final String TEST_MANUSCRIPT_ID_2 = "MS_TEST_99999";

    private StoragePath collectionPath = new StoragePath();
    private StoragePath manuscriptPath1 = new StoragePath();
    private StoragePath manuscriptPath2 = new StoragePath();

    public ManuscriptDatabaseStorageImplTest() {
        collectionPath.addPathElement("organizationCode", TEST_ORGANIZATION_CODE);
        manuscriptPath1.addPathElement("organizationCode", TEST_ORGANIZATION_CODE);
        manuscriptPath1.addPathElement("manuscriptId", TEST_MANUSCRIPT_ID_1);
        manuscriptPath2.addPathElement("organizationCode", TEST_ORGANIZATION_CODE);
        manuscriptPath2.addPathElement("manuscriptId", TEST_MANUSCRIPT_ID_2);
    }

    @Before
    public void setUp() {
        super.setUp();
        // Create an organization
        Organization organization = new Organization();
        organization.organizationCode = TEST_ORGANIZATION_CODE;
        organization.organizationName = TEST_ORGANIZATION_NAME;
        TableRow row = OrganizationDatabaseStorageImpl.tableRowFromOrganization(organization);
        Integer organizationId = null;
        try {
            DatabaseManager.deleteByValue(context, OrganizationDatabaseStorageImpl.ORGANIZATION_TABLE, OrganizationDatabaseStorageImpl.COLUMN_CODE, TEST_ORGANIZATION_CODE);
            DatabaseManager.insert(context, row);
            organizationId = row.getIntColumn(OrganizationDatabaseStorageImpl.COLUMN_ID);
        } catch (SQLException ex) {
            fail("Exception setting up test organization: " + ex);
        }

        // Create a manuscript
        Manuscript manuscript = new Manuscript();
        manuscript.configureTestValues();
        manuscript.manuscriptId = TEST_MANUSCRIPT_ID_1;
        try {
            DatabaseManager.deleteByValue(context, ManuscriptDatabaseStorageImpl.MANUSCRIPT_TABLE, ManuscriptDatabaseStorageImpl.COLUMN_MSID, TEST_MANUSCRIPT_ID_1);
            TableRow manuscriptRow = ManuscriptDatabaseStorageImpl.tableRowFromManuscript(manuscript, organizationId);
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
            DatabaseManager.deleteByValue(context, OrganizationDatabaseStorageImpl.ORGANIZATION_TABLE, OrganizationDatabaseStorageImpl.COLUMN_CODE, TEST_ORGANIZATION_CODE);
        } catch (SQLException ex) {
            fail("Exception clearing test organization and manuscript: " + ex);
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
        manuscript.manuscriptId = TEST_MANUSCRIPT_ID_1;
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
        manuscript.manuscriptId = TEST_MANUSCRIPT_ID_2;
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
        assertEquals("Read object should have same id as original", expManuscriptId, result.manuscriptId);
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
        Manuscript dummyManuscript = new Manuscript(TEST_MANUSCRIPT_ID_1, "accepted");
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
        manuscript.manuscriptId = TEST_MANUSCRIPT_ID_1;
        String updatedTitle = "Updated Title";
        manuscript.title = updatedTitle;
        ManuscriptDatabaseStorageImpl instance = new ManuscriptDatabaseStorageImpl();
        instance.updateObject(path, manuscript);
        manuscript = instance.readObject(path);
        assertEquals(updatedTitle, manuscript.title);

        // version should change internally!
        String query = "SELECT * FROM MANUSCRIPT where msid = ? and active = ?";
        TableRow row = DatabaseManager.querySingleTable(context, ManuscriptDatabaseStorageImpl.MANUSCRIPT_TABLE, query, manuscript.manuscriptId, ManuscriptDatabaseStorageImpl.ACTIVE_TRUE);
    }
}
