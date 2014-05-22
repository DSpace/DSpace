/*
 */
package org.datadryad.journalstatistics.extractor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.dspace.AbstractUnitTest;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataPackageCountTest extends AbstractUnitTest {

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
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of extract method, of class DataPackageCount.
     */
    @Test
    public void testExtract() {
        System.out.println("extract");
        String journalName = "";
        DataPackageCount instance = new DataPackageCount(null);
        Integer expResult = null;
        Integer result = instance.extract(journalName);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
