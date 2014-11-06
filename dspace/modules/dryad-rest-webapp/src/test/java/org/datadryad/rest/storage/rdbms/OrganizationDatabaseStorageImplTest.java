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
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class OrganizationDatabaseStorageImplTest extends ContextUnitTest {
    private static Logger log = Logger.getLogger(OrganizationDatabaseStorageImplTest.class);
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
        try {
            DatabaseManager.deleteByValue(context, OrganizationDatabaseStorageImpl.ORGANIZATION_TABLE, OrganizationDatabaseStorageImpl.COLUMN_CODE, TEST_ORGANIZATION_CODE_1);
            DatabaseManager.deleteByValue(context, OrganizationDatabaseStorageImpl.ORGANIZATION_TABLE, OrganizationDatabaseStorageImpl.COLUMN_CODE, TEST_ORGANIZATION_CODE_2);
            DatabaseManager.insert(context, row);
            context.commit();
        } catch (SQLException ex) {
            fail("Exception setting up test organization: " + ex);
        }
    }

    @Override
    public void tearDown() {
        try {
            DatabaseManager.deleteByValue(context, OrganizationDatabaseStorageImpl.ORGANIZATION_TABLE, OrganizationDatabaseStorageImpl.COLUMN_CODE, TEST_ORGANIZATION_CODE_1);
            DatabaseManager.deleteByValue(context, OrganizationDatabaseStorageImpl.ORGANIZATION_TABLE, OrganizationDatabaseStorageImpl.COLUMN_CODE, TEST_ORGANIZATION_CODE_2);
        } catch (SQLException ex) {
            fail("Exception clearing test organization: " + ex);
        }
        super.tearDown();
    }

    /**
     * Test of organizationFromTableRow method, of class OrganizationDatabaseStorageImpl.
     */
    @Test
    public void testOrganizationFromTableRow() {
        log.info("organizationFromTableRow");
        TableRow row = new TableRow(OrganizationDatabaseStorageImpl.ORGANIZATION_TABLE, OrganizationDatabaseStorageImpl.ORGANIZATION_COLUMNS);
        row.setColumn(OrganizationDatabaseStorageImpl.COLUMN_CODE, TEST_ORGANIZATION_CODE_1);
        row.setColumn(OrganizationDatabaseStorageImpl.COLUMN_NAME, TEST_ORGANIZATION_NAME_1);
        Organization organization = OrganizationDatabaseStorageImpl.organizationFromTableRow(row);
        assertEquals("Organization code should match", organization.organizationCode, TEST_ORGANIZATION_CODE_1);
        assertEquals("Organization name should match", organization.organizationName, TEST_ORGANIZATION_NAME_1);
    }

    /**
     * Test of tableRowFromOrganization method, of class OrganizationDatabaseStorageImpl.
     */
    @Test
    public void testTableRowFromOrganization() {
        log.info("tableRowFromOrganization");
        Organization organization = new Organization();
        organization.organizationCode = TEST_ORGANIZATION_CODE_2;
        organization.organizationName = TEST_ORGANIZATION_NAME_2;
        TableRow row = OrganizationDatabaseStorageImpl.tableRowFromOrganization(organization);
        assertEquals("Organization code should match", row.getStringColumn(OrganizationDatabaseStorageImpl.COLUMN_CODE), TEST_ORGANIZATION_CODE_2);
        assertEquals("Organization name should match", row.getStringColumn(OrganizationDatabaseStorageImpl.COLUMN_NAME), TEST_ORGANIZATION_NAME_2);
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
     * Test of createObject method, of class OrganizationDatabaseStorageImpl.
     */
    @Test
    public void testCreateObject() throws Exception {
        log.info("createObject");
        StoragePath path = new StoragePath();
        path.addPathElement("organizationCode", TEST_ORGANIZATION_CODE_2);
        OrganizationDatabaseStorageImpl instance = new OrganizationDatabaseStorageImpl();
        Organization organization = instance.readObject(path);
        assertNull("Object must not exist before creating", organization);
        organization = new Organization();
        organization.organizationCode = TEST_ORGANIZATION_CODE_2;
        organization.organizationName = TEST_ORGANIZATION_NAME_2;
        path = new StoragePath();
        instance.createObject(path, organization);
        Boolean exists = instance.objectExists(path, organization);
        assertTrue("Newly saved object should exist", exists);
    }

    @Test
    public void testUpdateObject() throws Exception {
        log.info("updateObject");
        OrganizationDatabaseStorageImpl instance = new OrganizationDatabaseStorageImpl();
        StoragePath path = new StoragePath();
        path.addPathElement("organizationCode", TEST_ORGANIZATION_CODE_1);
        Organization organization = instance.readObject(path);
        assertNotNull("Object must exist before updating", organization);
        organization.organizationName = TEST_ORGANIZATION_NAME_2;
        instance.updateObject(path, organization);
        organization = instance.readObject(path);
        assertEquals("Updated object should have updated name", TEST_ORGANIZATION_NAME_2, organization.organizationName);
        assertEquals("Updated object should have original code", TEST_ORGANIZATION_CODE_1, organization.organizationCode);
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
