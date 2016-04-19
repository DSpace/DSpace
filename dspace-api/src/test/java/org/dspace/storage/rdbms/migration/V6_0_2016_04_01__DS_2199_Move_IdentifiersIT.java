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
import org.apache.commons.dbcp2.BasicDataSource;
import org.dspace.AbstractDSpaceTest;
import org.dspace.identifier.DOI;
import org.dspace.identifier.EZIDIdentifierProvider;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
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
public class V6_0_2016_04_01__DS_2199_Move_IdentifiersIT
        extends AbstractDSpaceTest
{
    private static final String SHOULDER = "10.5072/FK2/";

    private int oldField;
    private int newField;

    private Connection cnctn;

    public V6_0_2016_04_01__DS_2199_Move_IdentifiersIT()
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
        cnctn = DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("dataSource", BasicDataSource.class)
                .getConnection();

        // Define the database
        DatabaseUtilsHelpers.updateDatabase();

        // Fill the schema registry
        int oldSchema = -1;
        try (PreparedStatement stmt = cnctn.prepareStatement(
                "SELECT metadata_schema_id FROM metadataschemaregistry"
                        + " WHERE short_id = ?"))
        {
            stmt.setString(1, V6_0_2016_04_01__DS_2199_Move_Identifiers.OLD_SCHEMA);
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
            stmt.setString(1, V6_0_2016_04_01__DS_2199_Move_Identifiers.NEW_SCHEMA);
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                    newSchema = rs.getInt(1);
            }
        }

        if (oldSchema < 0)
            try (PreparedStatement stmt = cnctn.prepareStatement(
                        "INSERT INTO metadataschemaregistry(namespace, short_id)"
                                + " VALUES(?, ?)",
                    new String[] {"metadata_schema_id"}))
            {
                stmt.setString(2, V6_0_2016_04_01__DS_2199_Move_Identifiers.OLD_SCHEMA);
                stmt.setString(3, V6_0_2016_04_01__DS_2199_Move_Identifiers.OLD_SCHEMA);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys())
                {
                    if (rs.next())
                        oldSchema = rs.getInt(1);
                }
                cnctn.commit();
            }

        if (newSchema < 0)
            try (PreparedStatement stmt = cnctn.prepareStatement(
                        "INSERT INTO metadataschemaregistry(namespace, short_id)"
                                + " VALUES(?, ?)",
                    new String[] {"metadata_schema_id"}))
            {
                stmt.setString(1, V6_0_2016_04_01__DS_2199_Move_Identifiers.NEW_SCHEMA);
                stmt.setString(2, V6_0_2016_04_01__DS_2199_Move_Identifiers.NEW_SCHEMA);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys())
                {
                    if (rs.next())
                        newSchema = rs.getInt(1);
                }
                cnctn.commit();
            }

        // Fill the field registry
        oldField = -1;
        try (PreparedStatement stmt = cnctn.prepareStatement(
                "SELECT metadata_field_id FROM metadatafieldregistry"
                        + " WHERE metadata_schema_id = ? and element = ? and qualifier = ?"))
        {
            stmt.setInt(1, oldSchema);
            stmt.setString(2, V6_0_2016_04_01__DS_2199_Move_Identifiers.OLD_ELEMENT);
            stmt.setString(3, V6_0_2016_04_01__DS_2199_Move_Identifiers.OLD_QUALIFIER);
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                    oldField = rs.getInt(1);
            }
        }

        newField = -1;
        try (PreparedStatement stmt = cnctn.prepareStatement(
                "SELECT metadata_field_id FROM metadatafieldregistry"
                        + " WHERE metadata_schema_id = ? and element = ? and qualifier = ?"))
        {
            stmt.setInt(1, oldSchema);
            stmt.setString(2, V6_0_2016_04_01__DS_2199_Move_Identifiers.NEW_ELEMENT);
            stmt.setString(3, V6_0_2016_04_01__DS_2199_Move_Identifiers.NEW_QUALIFIER);
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                    newField = rs.getInt(1);
            }
        }

        try (PreparedStatement stmt = cnctn.prepareStatement(
                "INSERT INTO metadatafieldregistry(metadata_field_id, metadata_schema_id, element, qualifier)"
                            + " VALUES(DEFAULT, ?, ?, ?)",
                new String[] {"metadata_field_id"}))
        {
            if (oldField < 0)
            {
                stmt.setInt(1, oldSchema);
                stmt.setString(2, V6_0_2016_04_01__DS_2199_Move_Identifiers.OLD_ELEMENT);
                stmt.setString(3, V6_0_2016_04_01__DS_2199_Move_Identifiers.OLD_QUALIFIER);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys())
                {
                    if (rs.next())
                        oldField = rs.getInt(1);
                }
                cnctn.commit();
            }

            if (newField < 0)
            {
                stmt.setInt(1, newSchema);
                stmt.setString(2, V6_0_2016_04_01__DS_2199_Move_Identifiers.NEW_ELEMENT);
                stmt.setString(3, V6_0_2016_04_01__DS_2199_Move_Identifiers.NEW_QUALIFIER);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys())
                {
                    if (rs.next())
                        newField = rs.getInt(1);
                }
                cnctn.commit();
            }
        }
    }

    @After
    public void tearDown()
            throws SQLException
    {
        cnctn.close();
    }

    /**
     * Test of migrate method, of class V6_0_2016_04_01__DS_2199_Move_Identifiers.
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

        // XXX DEBUG
        cnctn.createStatement().executeUpdate("SET TRACE_LEVEL_SYSTEM_OUT 3");

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
        V6_0_2016_04_01__DS_2199_Move_Identifiers instance
                = new V6_0_2016_04_01__DS_2199_Move_Identifiers();
        instance.migrate(cnctn);

        // Check the table for correct results
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
        V6_0_2016_04_01__DS_2199_Move_Identifiers instance = new V6_0_2016_04_01__DS_2199_Move_Identifiers();
        Integer expResult = null;
        Integer result = instance.getChecksum();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
}
