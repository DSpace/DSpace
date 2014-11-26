/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import org.dspace.AbstractUnitTest;
import org.dspace.core.ConfigurationManager;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mhwood
 */
public class DatabaseManagerTest
        extends AbstractUnitTest
{
    // Type of database that is being used in Unit Testing. See "setUpClass()" below.
    // This variable is used to determine expected values for some Database specific unit tests below.
    private static String dbtype = "";
    
    public DatabaseManagerTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
        // Based on configured db.url, determine type of Database that is running Unit Tests
        String dburl = ConfigurationManager.getProperty("db.url");
        if(dburl.contains(":postgresql:"))
            dbtype = DatabaseManager.DBMS_POSTGRES;
        else if(dburl.contains(":oracle:"))
            dbtype = DatabaseManager.DBMS_ORACLE;
        else if(dburl.contains(":h2:"))
            dbtype = DatabaseManager.DBMS_H2;
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
     * Test of isOracle method, of class DatabaseManager.
     */
    @Test
    public void testIsOracle()
    {
        System.out.println("isOracle");
        
        // Expected result = true for all DBMSes except PostgreSQL
        boolean expResult = true;
        if(dbtype.equals(DatabaseManager.DBMS_POSTGRES))
            expResult=false;
        
        boolean result = DatabaseManager.isOracle();
        assertEquals("isOracle is true for Oracle-like DBMSs", expResult, result);
    }

    /**
     * Test of setConstraintDeferred method, of class DatabaseManager.
     */
/*
    @Test
    public void testSetConstraintDeferred() throws Exception
    {
        System.out.println("setConstraintDeferred");
        Context context = null;
        String constraintName = "";
        DatabaseManager.setConstraintDeferred(context, constraintName);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setConstraintImmediate method, of class DatabaseManager.
     */
/*
    @Test
    public void testSetConstraintImmediate() throws Exception
    {
        System.out.println("setConstraintImmediate");
        Context context = null;
        String constraintName = "";
        DatabaseManager.setConstraintImmediate(context, constraintName);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of queryTable method, of class DatabaseManager.
     */
/*
    @Test
    public void testQueryTable() throws Exception
    {
        System.out.println("queryTable");
        Context context = null;
        String table = "";
        String query = "";
        Object[] parameters = null;
        TableRowIterator expResult = null;
        TableRowIterator result = DatabaseManager.queryTable(context, table,
                query, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of query method, of class DatabaseManager.
     */
/*
    @Test
    public void testQuery() throws Exception
    {
        System.out.println("query");
        Context context = null;
        String query = "";
        Object[] parameters = null;
        TableRowIterator expResult = null;
        TableRowIterator result = DatabaseManager.query(context, query,
                parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of querySingle method, of class DatabaseManager.
     */
/*
    @Test
    public void testQuerySingle() throws Exception
    {
        System.out.println("querySingle");
        Context context = null;
        String query = "";
        Object[] parameters = null;
        TableRow expResult = null;
        TableRow result = DatabaseManager.querySingle(context, query, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of querySingleTable method, of class DatabaseManager.
     */
/*
    @Test
    public void testQuerySingleTable() throws Exception
    {
        System.out.println("querySingleTable");
        Context context = null;
        String table = "";
        String query = "";
        Object[] parameters = null;
        TableRow expResult = null;
        TableRow result = DatabaseManager.querySingleTable(context, table, query,
                parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
    /**
     * Test of updateQuery method, of class DatabaseManager.
     */
/*
    @Test
    public void testUpdateQuery() throws Exception
    {
        System.out.println("updateQuery");
        Context context = null;
        String query = "";
        Object[] parameters = null;
        int expResult = 0;
        int result = DatabaseManager.updateQuery(context, query, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of create method, of class DatabaseManager.
     */
/*
    @Test
    public void testCreate() throws Exception
    {
        System.out.println("create");
        Context context = null;
        String table = "";
        TableRow expResult = null;
        TableRow result = DatabaseManager.create(context, table);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of find method, of class DatabaseManager.
     */
/*
    @Test
    public void testFind() throws Exception
    {
        System.out.println("find");
        Context context = null;
        String table = "";
        int id = 0;
        TableRow expResult = null;
        TableRow result = DatabaseManager.find(context, table, id);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of findByUnique method, of class DatabaseManager.
     */
/*
    @Test
    public void testFindByUnique() throws Exception
    {
        System.out.println("findByUnique");
        Context context = null;
        String table = "";
        String column = "";
        Object value = null;
        TableRow expResult = null;
        TableRow result = DatabaseManager.findByUnique(context, table, column,
                value);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of delete method, of class DatabaseManager.
     */
/*
    @Test
    public void testDelete_3args() throws Exception
    {
        System.out.println("delete");
        Context context = null;
        String table = "";
        int id = 0;
        int expResult = 0;
        int result = DatabaseManager.delete(context, table, id);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of deleteByValue method, of class DatabaseManager.
     */
/*
    @Test
    public void testDeleteByValue() throws Exception
    {
        System.out.println("deleteByValue");
        Context context = null;
        String table = "";
        String column = "";
        Object value = null;
        int expResult = 0;
        int result = DatabaseManager.deleteByValue(context, table, column, value);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getConnection method, of class DatabaseManager.
     */
/*
    @Test
    public void testGetConnection() throws Exception
    {
        System.out.println("getConnection");
        Connection expResult = null;
        Connection result = DatabaseManager.getConnection();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getDataSource method, of class DatabaseManager.
     */
/*
    @Test
    public void testGetDataSource()
    {
        System.out.println("getDataSource");
        DataSource expResult = null;
        DataSource result = DatabaseManager.getDataSource();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of freeConnection method, of class DatabaseManager.
     */
/*
    @Test
    public void testFreeConnection()
    {
        System.out.println("freeConnection");
        Connection c = null;
        DatabaseManager.freeConnection(c);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of row method, of class DatabaseManager.
     */
/*
    @Test
    public void testRow() throws Exception
    {
        System.out.println("row");
        String table = "";
        TableRow expResult = null;
        TableRow result = DatabaseManager.row(table);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of insert method, of class DatabaseManager.
     */
/*
    @Test
    public void testInsert() throws Exception
    {
        System.out.println("insert");
        Context context = null;
        TableRow row = null;
        DatabaseManager.insert(context, row);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of update method, of class DatabaseManager.
     */
/*
    @Test
    public void testUpdate() throws Exception
    {
        System.out.println("update");
        Context context = null;
        TableRow row = null;
        int expResult = 0;
        int result = DatabaseManager.update(context, row);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of delete method, of class DatabaseManager.
     */
/*
    @Test
    public void testDelete_Context_TableRow() throws Exception
    {
        System.out.println("delete");
        Context context = null;
        TableRow row = null;
        int expResult = 0;
        int result = DatabaseManager.delete(context, row);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getColumnInfo method, of class DatabaseManager.
     */
/*
    @Test
    public void testGetColumnInfo_String() throws Exception
    {
        System.out.println("getColumnInfo");
        String table = "";
        Collection<ColumnInfo> expResult = null;
        Collection<ColumnInfo> result = DatabaseManager.getColumnInfo(table);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getColumnInfo method, of class DatabaseManager.
     */
/*
    @Test
    public void testGetColumnInfo_String_String() throws Exception
    {
        System.out.println("getColumnInfo");
        String table = "";
        String column = "";
        ColumnInfo expResult = null;
        ColumnInfo result = DatabaseManager.getColumnInfo(table, column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getColumnNames method, of class DatabaseManager.
     */
/*
    @Test
    public void testGetColumnNames_String() throws Exception
    {
        System.out.println("getColumnNames");
        String table = "";
        List<String> expResult = null;
        List<String> result = DatabaseManager.getColumnNames(table);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getColumnNames method, of class DatabaseManager.
     */
/*
    @Test
    public void testGetColumnNames_ResultSetMetaData() throws Exception
    {
        System.out.println("getColumnNames");
        ResultSetMetaData meta = null;
        List<String> expResult = null;
        List<String> result = DatabaseManager.getColumnNames(meta);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of canonicalize method, of class DatabaseManager.
     */
/*
    @Test
    public void testCanonicalize()
    {
        System.out.println("canonicalize");
        String table = "";
        String expResult = "";
        String result = DatabaseManager.canonicalize(table);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of process method, of class DatabaseManager.
     */
/*
    @Test
    public void testProcess_ResultSet_String() throws Exception
    {
        System.out.println("process");
        ResultSet results = null;
        String table = "";
        TableRow expResult = null;
        TableRow result = DatabaseManager.process(results, table);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of process method, of class DatabaseManager.
     */
/*
    @Test
    public void testProcess_3args() throws Exception
    {
        System.out.println("process");
        ResultSet results = null;
        String table = "";
        List<String> pColumnNames = null;
        TableRow expResult = null;
        TableRow result = DatabaseManager.process(results, table, pColumnNames);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getPrimaryKeyColumn method, of class DatabaseManager.
     */
/*
    @Test
    public void testGetPrimaryKeyColumn_TableRow() throws Exception
    {
        System.out.println("getPrimaryKeyColumn");
        TableRow row = null;
        String expResult = "";
        String result = DatabaseManager.getPrimaryKeyColumn(row);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getPrimaryKeyColumn method, of class DatabaseManager.
     */
/*
    @Test
    public void testGetPrimaryKeyColumn_String() throws Exception
    {
        System.out.println("getPrimaryKeyColumn");
        String table = "";
        String expResult = "";
        String result = DatabaseManager.getPrimaryKeyColumn(table);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getPrimaryKeyColumnInfo method, of class DatabaseManager.
     */
/*
    @Test
    public void testGetPrimaryKeyColumnInfo() throws Exception
    {
        System.out.println("getPrimaryKeyColumnInfo");
        String table = "";
        ColumnInfo expResult = null;
        ColumnInfo result = DatabaseManager.getPrimaryKeyColumnInfo(table);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of shutdown method, of class DatabaseManager.
     */
/*
    @Test
    public void testShutdown() throws Exception
    {
        System.out.println("shutdown");
        DatabaseManager.shutdown();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getDbName method, of class DatabaseManager.
     */
    @Test
    public void testGetDbName()
    {
        System.out.println("getDbName");
        String expResult = null;
        // Based on the type of DB, the expected result is different
        if (dbtype.equals(DatabaseManager.DBMS_H2))
            expResult = "H2";
        else if (dbtype.equals(DatabaseManager.DBMS_POSTGRES))
            expResult = "PostgreSQL";
        else if (dbtype.equals(DatabaseManager.DBMS_ORACLE))
            expResult = "Oracle";
        
        String result = DatabaseManager.getDbName();
        assertEquals("Database name names the configured database driver",
                expResult, result);
    }

    /**
     * Test of getDbKeyword method, of class DatabaseManager.
     */
    @Test
    public void testGetDbKeyword()
    {
        System.out.println("getDbKeyword");
        // Expected result is what is configured in dbtype (see setUpClass())
        String expResult = dbtype;
        String result = DatabaseManager.getDbKeyword();
        assertEquals("Database 'keyword' names the configured DBMS", expResult, result);
    }

    /**
     * Test of loadParameters method, of class DatabaseManager.
     */
/*
    @Test
    public void testLoadParameters() throws Exception
    {
        System.out.println("loadParameters");
        PreparedStatement statement = null;
        Object[] parameters = null;
        DatabaseManager.loadParameters(statement, parameters);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of main method, of class DatabaseManager.
     */
/*
    @Test
    public void testMain()
    {
        System.out.println("main");
        String[] args = null;
        DatabaseManager.main(args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of applyOffsetAndLimit method, of class DatabaseManager.
     */
/*
    @Test
    public void testApplyOffsetAndLimit()
    {
        System.out.println("applyOffsetAndLimit");
        StringBuffer query = null;
        List<Serializable> params = null;
        int offset = 0;
        int limit = 0;
        DatabaseManager.applyOffsetAndLimit(query, params, offset, limit);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
}
