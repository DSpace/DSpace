/*
 */
package org.datadryad.journalstatistics.extractor;

import java.sql.SQLException;
import org.datadryad.api.DryadDataPackage;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
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
public class DataPackageCountTest {

    private Context context;
    public DataPackageCountTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        try {
            this.context = new Context();
            context.turnOffAuthorisationSystem();
        } catch (SQLException ex) {
            fail("Unable to instantiate Context " + ex);
        }
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of extract method, of class DataPackageCount.
     * Data packages are items in collection identified by 'stats.datapkgs.coll'
     * having prism.publicationName as provided
     */
    @Test
    public void testExtract() throws SQLException {
        // There should be one package in the database with journal name "Test Journal"
        // Create a data package
        DryadDataPackage dataPackage = DryadDataPackage.create(context);
        // set its journal
        String journalName = "Test Journal";
        dataPackage.setPublicationName(journalName);
        context.commit();

        DataPackageCount instance = new DataPackageCount(this.context);
        Integer expResult = 1;
        Integer result = instance.extract(journalName);
        assertEquals(expResult, result);
    }
}
