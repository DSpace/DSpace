/*
 */
package org.datadryad.rest.storage.rdbms;

import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.StoragePath;
import org.datadryad.test.ContextUnitTest;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class OrganizationDatabaseStorageImplTest extends ContextUnitTest {
    private static Logger log = Logger.getLogger(OrganizationDatabaseStorageImplTest.class);
    private static final Integer TEST_ORGANIZATION_ID = 1;
    private static final String TEST_ORGANIZATION_CODE_1 = "test1";
    private static final String TEST_ORGANIZATION_NAME_1 = "Test Organization 1";
    private static final String TEST_ORGANIZATION_CODE_2 = "test2";
    private static final String TEST_ORGANIZATION_NAME_2 = "Test Organization 2";

    @Before
    public void setUp() {
        super.setUp();
        // Create a row in the database
        Organization organization = new Organization();
        organization.organizationCode = TEST_ORGANIZATION_CODE_1;
        organization.organizationName = TEST_ORGANIZATION_NAME_1;
        TableRow row = OrganizationDatabaseStorageImpl.tableRowFromOrganization(organization);
        row.setColumn(OrganizationDatabaseStorageImpl.COLUMN_ID, TEST_ORGANIZATION_ID);
        try {
            DatabaseManager.deleteByValue(context, OrganizationDatabaseStorageImpl.ORGANIZATION_TABLE, OrganizationDatabaseStorageImpl.COLUMN_ID, TEST_ORGANIZATION_ID);
            DatabaseManager.insert(context, row);
        } catch (SQLException ex) {
            fail("Exception setting up test organization: " + ex);
        }
    }

    @Override
    public void tearDown() {
        try {
            DatabaseManager.deleteByValue(context, OrganizationDatabaseStorageImpl.ORGANIZATION_TABLE, OrganizationDatabaseStorageImpl.COLUMN_ID, 1);
        } catch (SQLException ex) {
            fail("Exception clearing test organization: " + ex);
        }
        super.tearDown();
    }

    /**
     * Test of objectExists method, of class OrganizationDatabaseStorageImpl.
     */
    @Test
    public void testObjectExists() throws Exception {
        log.info("objectExists");
        StoragePath path = new StoragePath();
        Organization organization = new Organization();
        organization.organizationCode = TEST_ORGANIZATION_CODE_1;
        organization.organizationName = TEST_ORGANIZATION_NAME_1;
        OrganizationDatabaseStorageImpl instance = new OrganizationDatabaseStorageImpl();
        Boolean expResult = true;
        Boolean result = instance.objectExists(path, organization);
        assertEquals("Object should exist", expResult, result);
    }

    /**
     * Test of addAll method, of class OrganizationDatabaseStorageImpl.
     */
    @Test
    public void testAddAll() throws Exception {
        log.info("addAll");
        StoragePath path = new StoragePath();
        List<Organization> organizations = new ArrayList<Organization>();
        OrganizationDatabaseStorageImpl instance = new OrganizationDatabaseStorageImpl();
        instance.addAll(path, organizations);
        Integer expectedSize = 1;
        Integer actualSize = organizations.size();
        assertEquals("There should be 1 organization", expectedSize, actualSize);
    }

    /**
     * Test of saveObject method, of class OrganizationDatabaseStorageImpl.
     */
    @Test
    public void testSaveObject() throws Exception {
        log.info("saveObject");
        StoragePath path = new StoragePath();
        Organization organization = new Organization();
        organization.organizationCode = TEST_ORGANIZATION_CODE_2;
        organization.organizationName = TEST_ORGANIZATION_NAME_2;
        OrganizationDatabaseStorageImpl instance = new OrganizationDatabaseStorageImpl();
        instance.saveObject(path, organization);
        Boolean exists = instance.objectExists(path, organization);
        assertTrue("Newly saved object should exist", exists);
    }

    /**
     * Test of readObject method, of class OrganizationDatabaseStorageImpl.
     */
    @Test
    public void testReadObject() throws Exception {
        log.info("readObject");
        // Read object requires full storage path
        StoragePath path = new StoragePath();
        path.addPathElement("organizationCode", TEST_ORGANIZATION_CODE_1);
        OrganizationDatabaseStorageImpl instance = new OrganizationDatabaseStorageImpl();
        String expectedName = TEST_ORGANIZATION_NAME_1;
        Organization result = instance.readObject(path);
        String resultName = result.organizationName;
        assertEquals("Read object should have same name as original", expectedName, resultName);
    }

    /**
     * Test of deleteObject method, of class OrganizationDatabaseStorageImpl.
     */
    @Test
    public void testDeleteObject() throws Exception {
        log.info("deleteObject");
        // Delete object requires full storage path
        StoragePath path = new StoragePath();
        path.addPathElement("organizationCode", TEST_ORGANIZATION_CODE_1);
        OrganizationDatabaseStorageImpl instance = new OrganizationDatabaseStorageImpl();
        instance.deleteObject(path);
        Organization dummyOrganization = new Organization();
        dummyOrganization.organizationCode = TEST_ORGANIZATION_CODE_1;
        Boolean exists = instance.objectExists(path, dummyOrganization);
        assertFalse("Deleted object should not exist", exists);
    }
}
