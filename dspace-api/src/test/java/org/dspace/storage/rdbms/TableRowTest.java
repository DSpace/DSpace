/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.storage.rdbms;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author mwood
 */
public class TableRowTest
{

    public TableRowTest()
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
    {
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of getTable method, of class TableRow.
     */
/*
    @Test
    public void testGetTable()
    {
        System.out.println("getTable");
        TableRow instance = null;
        String expResult = "";
        String result = instance.getTable();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setTable method, of class TableRow.
     */
/*
    @Test
    public void testSetTable()
    {
        System.out.println("setTable");
        String table = "";
        TableRow instance = null;
        instance.setTable(table);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of hasColumn method, of class TableRow.
     */
/*
    @Test
    public void testHasColumn()
    {
        System.out.println("hasColumn");
        String column = "";
        TableRow instance = null;
        boolean expResult = false;
        boolean result = instance.hasColumn(column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of hasColumnChanged method, of class TableRow.
     */
/*
    @Test
    public void testHasColumnChanged()
    {
        System.out.println("hasColumnChanged");
        String column = "";
        TableRow instance = null;
        boolean expResult = false;
        boolean result = instance.hasColumnChanged(column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of hasColumnChangedCanonicalized method, of class TableRow.
     */
/*
    @Test
    public void testHasColumnChangedCanonicalized()
    {
        System.out.println("hasColumnChangedCanonicalized");
        String column = "";
        TableRow instance = null;
        boolean expResult = false;
        boolean result = instance.hasColumnChangedCanonicalized(column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of isColumnNull method, of class TableRow.
     */
/*
    @Test
    public void testIsColumnNull()
    {
        System.out.println("isColumnNull");
        String column = "";
        TableRow instance = null;
        boolean expResult = false;
        boolean result = instance.isColumnNull(column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of isColumnNullCanonicalized method, of class TableRow.
     */
/*
    @Test
    public void testIsColumnNullCanonicalized()
    {
        System.out.println("isColumnNullCanonicalized");
        String column = "";
        TableRow instance = null;
        boolean expResult = false;
        boolean result = instance.isColumnNullCanonicalized(column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getIntColumn method, of class TableRow.
     */
/*
    @Test
    public void testGetIntColumn()
    {
        System.out.println("getIntColumn");
        String column = "";
        TableRow instance = null;
        int expResult = 0;
        int result = instance.getIntColumn(column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getLongColumn method, of class TableRow.
     */
/*
    @Test
    public void testGetLongColumn()
    {
        System.out.println("getLongColumn");
        String column = "";
        TableRow instance = null;
        long expResult = 0L;
        long result = instance.getLongColumn(column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getNumericColumn method, of class TableRow.
     */
    @Test
    public void testGetNumericColumn()
    {
        System.out.println("getNumericColumn");
        String column = "fifth";
        List columns = new ArrayList<>();
        columns.add(column);
        TableRow instance = new TableRow("row", columns);
        instance.setColumn(column, BigDecimal.ONE);
        BigDecimal expResult = BigDecimal.ONE;
        BigDecimal result = instance.getNumericColumn(column);
        assertEquals(expResult, result);
    }

    /**
     * Test of getDoubleColumn method, of class TableRow.
     */
/*
    @Test
    public void testGetDoubleColumn()
    {
        System.out.println("getDoubleColumn");
        String column = "";
        TableRow instance = null;
        double expResult = 0.0;
        double result = instance.getDoubleColumn(column);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getStringColumn method, of class TableRow.
     */
/*
    @Test
    public void testGetStringColumn()
    {
        System.out.println("getStringColumn");
        String column = "";
        TableRow instance = null;
        String expResult = "";
        String result = instance.getStringColumn(column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getBooleanColumn method, of class TableRow.
     */
/*
    @Test
    public void testGetBooleanColumn()
    {
        System.out.println("getBooleanColumn");
        String column = "";
        TableRow instance = null;
        boolean expResult = false;
        boolean result = instance.getBooleanColumn(column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getDateColumn method, of class TableRow.
     */
/*
    @Test
    public void testGetDateColumn()
    {
        System.out.println("getDateColumn");
        String column = "";
        TableRow instance = null;
        Date expResult = null;
        Date result = instance.getDateColumn(column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setColumnNull method, of class TableRow.
     */
/*
    @Test
    public void testSetColumnNull()
    {
        System.out.println("setColumnNull");
        String column = "";
        TableRow instance = null;
        instance.setColumnNull(column);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setColumn method, of class TableRow.
     */
/*
    @Test
    public void testSetColumn_String_boolean()
    {
        System.out.println("setColumn");
        String column = "";
        boolean b = false;
        TableRow instance = null;
        instance.setColumn(column, b);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setColumn method, of class TableRow.
     */
/*
    @Test
    public void testSetColumn_String_String()
    {
        System.out.println("setColumn");
        String column = "";
        String s = "";
        TableRow instance = null;
        instance.setColumn(column, s);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setColumn method, of class TableRow.
     */
/*
    @Test
    public void testSetColumn_String_int()
    {
        System.out.println("setColumn");
        String column = "";
        int i = 0;
        TableRow instance = null;
        instance.setColumn(column, i);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setColumn method, of class TableRow.
     */
/*
    @Test
    public void testSetColumn_String_long()
    {
        System.out.println("setColumn");
        String column = "";
        long l = 0L;
        TableRow instance = null;
        instance.setColumn(column, l);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setColumn method, of class TableRow.
     */
    @Test
    public void testSetColumn_String_BigDecimal()
    {
        System.out.println("setColumn");
        String column = "Corinthian";
        List<String> columns = new ArrayList<>();
        columns.add(column);
        BigDecimal bd = BigDecimal.ONE;
        TableRow instance = new TableRow("row", columns);
        instance.setColumn(column, bd);
        BigDecimal result = instance.getNumericColumn(column);
        assertEquals("Should give back the same value set", bd, result);
    }

    /**
     * Test of setColumn method, of class TableRow.
     */
/*
    @Test
    public void testSetColumn_String_double()
    {
        System.out.println("setColumn");
        String column = "";
        double d = 0.0;
        TableRow instance = null;
        instance.setColumn(column, d);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setColumn method, of class TableRow.
     */
/*
    @Test
    public void testSetColumn_String_Date()
    {
        System.out.println("setColumn");
        String column = "";
        Date d = null;
        TableRow instance = null;
        instance.setColumn(column, d);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of toString method, of class TableRow.
     */
/*
    @Test
    public void testToString()
    {
        System.out.println("toString");
        TableRow instance = null;
        String expResult = "";
        String result = instance.toString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of hashCode method, of class TableRow.
     */
/*
    @Test
    public void testHashCode()
    {
        System.out.println("hashCode");
        TableRow instance = null;
        int expResult = 0;
        int result = instance.hashCode();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of equals method, of class TableRow.
     */
/*
    @Test
    public void testEquals()
    {
        System.out.println("equals");
        Object obj = null;
        TableRow instance = null;
        boolean expResult = false;
        boolean result = instance.equals(obj);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of resetChanged method, of class TableRow.
     */
/*
    @Test
    public void testResetChanged()
    {
        System.out.println("resetChanged");
        TableRow instance = null;
        instance.resetChanged();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
}
