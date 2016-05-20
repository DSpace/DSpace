/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.storage.rdbms.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dspace.AbstractDSpaceTest;
import org.dspace.identifier.DOI;
import org.dspace.identifier.EZIDIdentifierProvider;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.rdbms.DatabaseDebug;
import org.dspace.storage.rdbms.DatabaseUtilsHelpers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test migration of certain identifiers to new fields, for DS-2199.
 * This class must do its own database setup, because it tests code that is run
 * by Flyway before the persistence unit is initialized.
 *
 * @author mwood
 */
public class V6_0_2016_04_01_0002__DS_2199_Move_IdentifiersIT
        extends AbstractDSpaceTest
{
    private static final String SHOULDER = "10.5072/FK2/";

    private int oldField;
    private int newField;

    private Connection cnctn;

    public V6_0_2016_04_01_0002__DS_2199_Move_IdentifiersIT()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
            throws SQLException
    {
        // Set up a DBMS connection
        DataSource ds = DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("dataSource", BasicDataSource.class);
        cnctn = ds.getConnection();

        // Define the database.
        DatabaseUtilsHelpers.updateDatabase(ds, cnctn);

        // Look up schemas
        int oldSchema = -1;
        try (PreparedStatement stmt = cnctn.prepareStatement(
                "SELECT metadata_schema_id FROM metadataschemaregistry"
                        + " WHERE short_id = ?"))
        {
            stmt.setString(1, V6_0_2016_04_01_0002__DS_2199_Move_Identifiers.OLD_SCHEMA);
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                    oldSchema = rs.getInt(1);
            }
        }

        int newSchema = -1;
        try (PreparedStatement stmt = cnctn.prepareStatement(
                "SELECT metadata_schema_id FROM metadataschemaregistry"
                        + " WHERE short_id = ?"))
        {
            stmt.setString(1, V6_0_2016_04_01_0002__DS_2199_Move_Identifiers.NEW_SCHEMA);
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                    newSchema = rs.getInt(1);
            }
        }

        // Look up old and new fields
        oldField = lookUpField(cnctn, oldSchema,
                V6_0_2016_04_01_0002__DS_2199_Move_Identifiers.OLD_ELEMENT,
                V6_0_2016_04_01_0002__DS_2199_Move_Identifiers.OLD_QUALIFIER);
        newField = lookUpField(cnctn, newSchema,
                V6_0_2016_04_01_0002__DS_2199_Move_Identifiers.NEW_ELEMENT,
                V6_0_2016_04_01_0002__DS_2199_Move_Identifiers.NEW_QUALIFIER);
    }

    @After
    public void tearDown()
            throws SQLException
    {
        cnctn.close();
    }

    /**
     * Test of migrate method, of class V6_0_2016_04_01_0002__DS_2199_Move_Identifiers.
     * @throws java.lang.Exception passed through.
     */
    @Test
    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public void testMigrate()
            throws Exception
    {
        System.out.println("migrate");

        // Ensure that the table is empty.
        try (Statement stmt = cnctn.createStatement())
        {
            stmt.executeUpdate("DELETE from metadatavalue");
            cnctn.commit();
        }

        // Set up the necessary configuration
        final ConfigurationService cfg
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        cfg.setProperty(EZIDIdentifierProvider.CFG_SHOULDER, SHOULDER);

        // Set up some records to be migrated (or not, depending on their content)
        int valueId = 0;
        final String prefix = DOI.SCHEME + SHOULDER;
        try (PreparedStatement stmt = cnctn.prepareStatement(
                "INSERT INTO metadatavalue(metadata_value_id, metadata_field_id, text_value)"
                        + " VALUES(?, ?, ?)"))
        {
            stmt.setInt(1, ++valueId);
            stmt.setInt(2, oldField);
            stmt.setString(3, prefix + "123");
            stmt.executeUpdate();

            stmt.setInt(1, ++valueId);
            stmt.setInt(2, newField);
            stmt.setString(3, prefix + "456");
            stmt.executeUpdate();

            cnctn.commit();
        }

        // Migrate!
        DatabaseDebug.dumpATable(cnctn, "metadatavalue"); // XXX DEBUG
        V6_0_2016_04_01_0002__DS_2199_Move_Identifiers instance
                = new V6_0_2016_04_01_0002__DS_2199_Move_Identifiers();
        //cnctn.createStatement().executeUpdate("SET TRACE_LEVEL_SYSTEM_OUT 3"); // XXX DEBUG
        instance.migrate(cnctn);
        //cnctn.createStatement().executeUpdate("SET TRACE_LEVEL_SYSTEM_OUT 0"); // XXX DEBUG

        // Check the table for correct results
        DatabaseDebug.dumpATable(cnctn, "metadatavalue"); // XXX DEBUG
        try (PreparedStatement stmt = cnctn.prepareStatement(
                "SELECT text_value FROM metadatavalue WHERE metadata_field_id = ?"))
        {
            int results;

            stmt.setInt(1, oldField);
            try (ResultSet rs = stmt.executeQuery())
            {
                results = 0;
                while(rs.next())
                {
                    System.out.format("Old value:  %s%n", rs.getString(1));
                    results++;
                }
            }
            assertEquals("There should be zero old values:", 0, results);

            stmt.setInt(1, newField);
            try (ResultSet rs = stmt.executeQuery())
            {
                results = 0;
                while(rs.next())
                {
                    System.out.format("New value:  %s%n", rs.getString(1));
                    results++;
                }
            }
            assertEquals("There should be two new values:", 2, results);
        }
    }

    /**
     * Test of getChecksum method, of class V6_0_2016_04_01__DS_2199_Move_Identifiers.
     */
    /*
    @Test
    public void testGetChecksum()
    {
    System.out.println("getChecksum");
    V6_0_2016_04_01_0002__DS_2199_Move_Identifiers instance = new V6_0_2016_04_01_0002__DS_2199_Move_Identifiers();
    Integer expResult = null;
    Integer result = instance.getChecksum();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
    }
     */

    private static final String FIELD_NON_NULL_QUALIFIER
            = "SELECT metadata_field_id FROM metadatafieldregistry"
            + " WHERE metadata_schema_id = ? AND element = ? and qualifier = ?";

    private static final String FIELD_NULL_QUALIFIER
            = "SELECT metadata_field_id FROM metadatafieldregistry"
            + " WHERE metadata_schema_id = ? AND element = ? AND qualifier IS NULL";

    int lookUpField(Connection cnctn, int schema, String element, String qualifier)
            throws SQLException
    {
        String sql;
        if (null == qualifier)
            sql = FIELD_NULL_QUALIFIER;
        else
            sql = FIELD_NON_NULL_QUALIFIER;

        int field = -1;
        try (PreparedStatement stmt = cnctn.prepareStatement(sql))
        {
            stmt.setInt(1, schema);
            stmt.setString(2, element);
            if (null != qualifier)
                stmt.setString(3, qualifier);
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                    field = rs.getInt(1);
            }
        }
        return field;
    }
}
