/*
 * SimplePool.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.storage.rdbms;


import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import org.apache.log4j.Category;

import org.dspace.core.ConfigurationManager;

/**
 * Simple connection pool
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class SimplePool implements java.sql.Driver
{
    /**
     * Log4j category
     */
    private static Category log = Category.getInstance(SimplePool.class);

    /**
     * Constructor
     */
    public SimplePool (String driverClass)
        throws SQLException
    {
        registerDriver(driverClass);
    }

    /**
     * Constructor
     */
    public SimplePool ()
        throws SQLException
    {
        this(DRIVER_CLASS);
    }

    /**
     * Register the driver with the DriverManager
     */
    private void registerDriver(String classname)
        throws SQLException
    {
        if (driver != null)
            return;

        try
        {
            Class driverClass = Class.forName(classname);

            for (Enumeration enum = DriverManager.getDrivers();
                enum.hasMoreElements();)
            {
                Driver d = (Driver) enum.nextElement();

                if (driverClass.isInstance(d))
                {
                    driver = d;
                    return;
                }
            }

            driver = (Driver) driverClass.newInstance();
            DriverManager.registerDriver(driver);
        }
        catch (Exception e)
        {
            throw new SQLException("Exception while registering driver: " + e);
        }
    }

    /**
     * Get a JDBC SQL connection
     */
    public Connection connect(String s, Properties p)
        throws SQLException
    {
        return connectInternal(s, p, 0);
    }

    /**
     * Get a JDBC SQL connection
     */
    public synchronized Connection connectInternal(String s, Properties p, int count)
        throws SQLException
    {
        // Check the pool first
        for (Iterator iterator = pool.iterator(); iterator.hasNext();)
        {
            PooledConnection pc = (PooledConnection) iterator.next();

            // Found one
            if (pc.isAvailable())
            {
                if (log.isDebugEnabled())
                    log.debug("Using connection " + pc.getId());

                    //  FIXME This method blocks!
                    //         if (pc.isClosed())
                    //         {
                    //           if (log.isInfoEnabled())
                    //             log.info("Reopening connection " + pc.getId());
                    //
                    //           Connection c = driver.connect(DRIVER_URL, p);
                    //           pc.setPhysicalConnection(c);
                    //         }

                pc.reuse();
                return pc;
            }
        }

        // Create a new one
        if (pool.size() < max_connections)
        {
            Connection c = driver.connect(DRIVER_URL, p);
            // Make the wrapper
            PooledConnection pc = new PooledConnection(c);

            // Add it to the pool
            pool.add(pc);
            if (log.isDebugEnabled())
                log.debug("Creating new connection " + pc.getId());
            return pc;
        }
        // Up to maximum, block
        else
        {
            log.warn("No connections available (" + (count + 1) + " tries)");

            try
            {
                // Explicit gc-ing is usually a no-no, but here it makes
                // sense; if there's a Connection out there waiting to
                // be garbage collected and closed, this may be the nudge
                // it needs.
                System.gc();
                wait(WAIT_TIME);
            }
            catch (InterruptedException ie)
            {}

            if (count >= MAX_ATTEMPTS)
                throw new SQLException("Unable to obtain an SQL Connection after " + count + " attempts");

            return connectInternal(s, p, count + 1);
        }
    }

    // Simple info class
    static class SimplePoolInfo
    {

        /**
         * Constructor
         */
        public SimplePoolInfo (int size, int free, int max)
        {
            this.size = size;
            this.free = free;
            this.used = size - free;
            this.max = max;
        }

        // Accessors
        public int getSize()
        {
            return size;
        }

        public int getFree()
        {
            return free;
        }

        public int getUsed()
        {
            return used;
        }

        public int getMaximumSize()
        {
            return max;
        }

        // Variables
        private int size;
        private int free;
        private int used;
        private int max;
    }

    /**
     * Set the maximum size for the pool.
     *
     * This setting is NOT persistent.
     *
     * Note that this method does not close any open connections.
     * So, if you have a max of 10 connections and you resize to
     * 15, you now have 5 extra connections; but if you have 10
     * connections and you set the size to 5, you may still have
     * 10 connections in use.
     */
    public static synchronized void setPoolMaximumSize(int size)
    {
        max_connections = size;
    }

    /**
     * Return info about the Pool
     */
    public static synchronized SimplePoolInfo getInfo()
    {
        int free = 0;

        for (Iterator iterator = pool.iterator(); iterator.hasNext();)
        {
            PooledConnection pc = (PooledConnection) iterator.next();

            if (pc.isAvailable())
                free++;
        }

        return new SimplePoolInfo(pool.size(), free, max_connections);
    }

    public boolean acceptsURL(String s)
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Called acceptsURL with " + s);

        return s.startsWith("jdbc:simplepool:");
    }

    public DriverPropertyInfo[] getPropertyInfo(String s, Properties p)
        throws SQLException
    {
        return driver.getPropertyInfo(s, p);
    }

    public int getMajorVersion()
    {
        return 0;
    }

    public int getMinorVersion()
    {
        return 1;
    }

    public boolean jdbcCompliant()
    {
        // We are as compliant as the underlying driver
        return driver.jdbcCompliant();
    }

    /**
     * The pooled connections
     */
    private static List pool = new ArrayList();

    /**
     * The underlying driver
     */
    private Driver driver;

    /**
     * Config settings
     */
    private static String DRIVER_CLASS =
        ConfigurationManager.getProperty("simplepool.driver");
    private static String DRIVER_URL =
        ConfigurationManager.getProperty("simplepool.url");
    private static String MAX_CONNECTIONS =
        ConfigurationManager.getProperty("simplepool.max.connections");
    // Default
    private static int max_connections = 15;

    // Time to wait, in milliseconds
    // If a connection is not available, the request will wait for
    // this length of time before trying again
    private static long WAIT_TIME = 1000;

    /**
     * After this many connection attempts, an SQLException will be thrown
     */
    private static int MAX_ATTEMPTS = 5;

    static
    {
        try
        {
            if (MAX_CONNECTIONS != null)
            {
                int tmp = Integer.parseInt(MAX_CONNECTIONS);

                max_connections = tmp;
            }
        }
        catch (NumberFormatException nfe)
        {
            nfe.printStackTrace();
        }
    }
}


// Connection wrapper
class PooledConnection implements java.sql.Connection
{
    private boolean free = true;
    private int id;
    private static int count = 0;
    private Connection physicalConnection = null;
    private PooledCallableStatement statement = null;
    private boolean closed = false;

    private static Category log = Category.getInstance(PooledConnection.class);

    /**
     * Constructor
     */
    public PooledConnection (Connection c)
    {
        this.physicalConnection = c;
        this.id = count++;
        this.free = false;

        if (log.isDebugEnabled())
            log.debug("Creating connection " + id);
    }

    ////////////////////////////////////////
    // Other methods
    ////////////////////////////////////////

    public void finalize()
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Called finalize on connection " + id);

        close();
    }

    public boolean isAvailable()
    {
        return isFree() && (!isBeingUsed());
    }

    public boolean isFree()
    {
        return free;
    }

    public void setFree(boolean value)
    {
        free = value;
        closed = value;
    }

    public int getId()
    {
        return id;
    }

    /**
     * Get the value of physicalConnection.
     * @return Value of physicalConnection.
     */
    public Connection getPhysicalConnection()
    {
        return physicalConnection;
    }

    /**
     * Set the value of physicalConnection.
     * @param v  Value to assign to physicalConnection.
     */
    public void setPhysicalConnection(Connection  v)
    {
        this.physicalConnection = v;
    }

    public boolean isBeingUsed()
    {
        return statement == null ? false : statement.isBeingUsed();
    }

    public void reuse()
    {
        closed = false;
        free = false;
    }

    public void validate()
    {
        if (closed)
            if (log.isDebugEnabled())
                log.debug("Alert: connection " + id + " using closed connection");
    }

    ////////////////////////////////////////
    // Delegated methods
    ////////////////////////////////////////

    public Statement createStatement()
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Connection " + id + ": called createStatement");

        validate();

        Statement s = physicalConnection.createStatement();

        statement = new PooledCallableStatement(physicalConnection, s, id);
        return statement;
    }

    public PreparedStatement prepareStatement(String sql)
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Connection " + id + ": called prepareStatement with sql \"" + sql + "\"");

        validate();

        PreparedStatement s = physicalConnection.prepareStatement(sql);

        statement = new PooledCallableStatement(physicalConnection, s, id);
        return statement;
    }

    public CallableStatement prepareCall(String sql)
        throws SQLException
    {
        validate();

        CallableStatement s = physicalConnection.prepareCall(sql);

        statement = new PooledCallableStatement(physicalConnection, s, id);
        return statement;
    }

    public String nativeSQL(String s)
        throws SQLException
    {
        validate();
        return physicalConnection.nativeSQL(s);
    }

    public void setAutoCommit(boolean b)
        throws SQLException
    {
        validate();
        physicalConnection.setAutoCommit(b);
    }

    public boolean getAutoCommit()
        throws SQLException
    {
        validate();
        return physicalConnection.getAutoCommit();
    }

    public void commit()
        throws SQLException
    {
        validate();
        physicalConnection.commit();
    }

    public void rollback()
        throws SQLException
    {
        validate();
        physicalConnection.rollback();
    }

    public void close()
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Connection " + id + ": closed");

            // Close the statement
        if (statement != null)
            statement.close();

        statement = null;
        free = true;
        closed = true;
    }

    public boolean isClosed()
        throws SQLException
    {
        return physicalConnection.isClosed();
    }

    public DatabaseMetaData getMetaData()
        throws SQLException
    {
        validate();
        return physicalConnection.getMetaData();
    }

    public void setReadOnly(boolean b)
        throws SQLException
    {
        validate();
        physicalConnection.setReadOnly(b);
    }

    public boolean isReadOnly()
        throws SQLException
    {
        validate();
        return physicalConnection.isReadOnly();
    }

    public void setCatalog(String s)
        throws SQLException
    {
        validate();
        physicalConnection.setCatalog(s);
    }

    public String getCatalog()
        throws SQLException
    {
        validate();
        return physicalConnection.getCatalog();
    }

    public void setTransactionIsolation(int i)
        throws SQLException
    {
        validate();

        physicalConnection.setTransactionIsolation(i);
    }

    public int getTransactionIsolation()
        throws SQLException
    {
        validate();
        return physicalConnection.getTransactionIsolation();
    }

    public SQLWarning getWarnings()
        throws SQLException
    {
        validate();
        return physicalConnection.getWarnings();
    }

    public void clearWarnings()
        throws SQLException
    {
        validate();
        physicalConnection.clearWarnings();
    }

    public Statement createStatement(int i, int j)
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Connection " + id + " createStatement");

        validate();
        return physicalConnection.createStatement(i, j);
    }

    public PreparedStatement prepareStatement(String sql, int i, int j)
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Connection " + id + " prepareStatement with sql \"" + sql + "\"");

        validate();

        return physicalConnection.prepareStatement(sql, i, j);
    }

    public CallableStatement prepareCall(String sql, int i, int j)
        throws SQLException
    {
        validate();
        return physicalConnection.prepareCall(sql, i, j);
    }

    public java.util.Map getTypeMap()
        throws SQLException
    {
        validate();
        return physicalConnection.getTypeMap();
    }

    public void setTypeMap(java.util.Map map)
        throws SQLException
    {
        validate();
        physicalConnection.setTypeMap(map);
    }
}


class PooledCallableStatement implements java.sql.CallableStatement
{
    private Connection connection;
    private Statement statement;
    private PooledResultSet results;
    private int id;
    private boolean closed = false;

    private static Category log = Category.getInstance(PooledCallableStatement.class);

    /**
     * Constructor
     */
    public PooledCallableStatement (Connection c, Statement s, int id)
    {
        this.connection = c;
        this.statement = s;
        this.id = id;
        this.closed = false;
    }

    ////////////////////////////////////////
    // Non-interface methods
    ////////////////////////////////////////

    public void finalize()
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Called finalize on statement with connection " + id);

        close();
    }

    public boolean isBeingUsed()
    {
        return results == null ? false : results.isBeingUsed();
    }

    public void validate()
    {
        if (closed)
            if (log.isDebugEnabled())
                log.debug("Alert: connection " + id + " using closed statement");
    }

    ////////////////////////////////////////
    // Delegated methods
    ////////////////////////////////////////

    public void registerOutParameter(int i, int j)
        throws SQLException
    {
        validate();
        ((CallableStatement) statement).registerOutParameter(i, j);
    }

    public void registerOutParameter(int i, int j, int k)
        throws SQLException
    {
        validate();
        ((CallableStatement) statement).registerOutParameter(i, j, k);
    }

    public boolean wasNull()
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).wasNull();
    }

    public String getString(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getString(i);
    }

    public boolean getBoolean(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getBoolean(i);
    }

    public byte getByte(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getByte(i);
    }

    public short getShort(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getShort(i);
    }

    public int getInt(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getInt(i);
    }

    public long getLong(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getLong(i);
    }

    public float getFloat(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getFloat(i);
    }

    public double getDouble(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getDouble(i);
    }

    public BigDecimal getBigDecimal(int i, int j)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getBigDecimal(i, j);
    }

    public byte[] getBytes(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getBytes(i);
    }

    public java.sql.Date getDate(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getDate(i);
    }

    public Time getTime(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getTime(i);
    }

    public Timestamp getTimestamp(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getTimestamp(i);
    }

    public Object getObject(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getObject(i);
    }

    public BigDecimal getBigDecimal(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getBigDecimal(i);
    }

    public Object getObject(int i, java.util.Map map)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getObject(i, map);
    }

    public Ref getRef(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getRef(i);
    }

    public Blob getBlob(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getBlob(i);
    }

    public Clob getClob(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getClob(i);
    }

    public Array getArray(int i)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getArray(i);
    }

    public java.sql.Date getDate(int i, Calendar calendar)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getDate(i, calendar);
    }

    public Time getTime(int i, Calendar calendar)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getTime(i, calendar);
    }

    public Timestamp getTimestamp(int i, Calendar calendar)
        throws SQLException
    {
        validate();
        return ((CallableStatement) statement).getTimestamp(i, calendar);
    }

    public void registerOutParameter(int i, int j, String s)
        throws SQLException
    {
        validate();
        ((CallableStatement) statement).registerOutParameter(i, j, s);
    }

    ////////////////////////////////////////
    // PreparedStatement methods
    ////////////////////////////////////////
    public ResultSet executeQuery()
        throws SQLException
    {
        validate();
        ResultSet r = ((PreparedStatement) statement).executeQuery();

        results = new PooledResultSet(connection, statement, r, id);
        return results;
    }

    public int executeUpdate()
        throws SQLException
    {
        validate();
        return ((PreparedStatement) statement).executeUpdate();
    }

    public void setNull(int i, int j)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setNull(i, j);
    }

    public void setBoolean(int i, boolean b)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setBoolean(i, b);
    }

    public void setByte(int i, byte b)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setByte(i, b);
    }

    public void setShort(int i, short s)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setShort(i, s);
    }

    public void setInt(int i, int j)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setInt(i, j);
    }

    public void setLong(int i, long l)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setLong(i, l);
    }

    public void setFloat(int i, float f)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setFloat(i, f);
    }

    public void setDouble(int i, double d)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setDouble(i, d);
    }

    public void setBigDecimal(int i, BigDecimal bd)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setBigDecimal(i, bd);
    }

    public void setString(int i, String s)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setString(i, s);
    }

    public void setBytes(int i, byte[] b)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setBytes(i, b);
    }

    public void setDate(int i, java.sql.Date d)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setDate(i, d);
    }

    public void setTime(int i, Time t)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setTime(i, t);
    }

    public void setTimestamp(int i, Timestamp t)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setTimestamp(i, t);
    }

    public void setAsciiStream(int i, InputStream stream, int j)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setAsciiStream(i, stream, j);
    }

    public void setUnicodeStream(int i, InputStream stream, int j)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setUnicodeStream(i, stream, j);
    }

    public void setBinaryStream(int i, InputStream stream, int j)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setBinaryStream(i, stream, j);
    }

    public void clearParameters()
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).clearParameters();
    }

    public void setObject(int i, Object obj, int j, int k)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setObject(i, obj, j, k);
    }

    public void setObject(int i, Object obj, int j)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setObject(i, obj, j);
    }

    public void setObject(int i, Object obj)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setObject(i, obj);
    }

    public boolean execute()
        throws SQLException
    {
        validate();
        return ((PreparedStatement) statement).execute();
    }

    public void addBatch()
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).addBatch();
    }

    public void setCharacterStream(int i, Reader reader, int j)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setCharacterStream(i, reader, j);
    }

    public void setRef(int i, Ref r)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setRef(i, r);
    }

    public void setBlob(int i, Blob b)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setBlob(i, b);
    }

    public void setClob(int i, Clob c)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setClob(i, c);
    }

    public void setArray(int i, Array a)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setArray(i, a);
    }

    public ResultSetMetaData getMetaData()
        throws SQLException
    {
        validate();
        return ((PreparedStatement) statement).getMetaData();
    }

    public void setDate(int i, java.sql.Date d, Calendar calendar)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setDate(i, d, calendar);
    }

    public void setTime(int i, Time t, Calendar calendar)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setTime(i, t, calendar);
    }

    public void setTimestamp(int i, Timestamp t, Calendar calendar)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setTimestamp(i, t, calendar);
    }

    public void setNull(int i, int j, String s)
        throws SQLException
    {
        validate();
        ((PreparedStatement) statement).setNull(i, j, s);
    }

    ////////////////////////////////////////
    // Statement methods
    ////////////////////////////////////////
    public ResultSet executeQuery(String sql)
        throws SQLException
    {
        validate();
        if (log.isDebugEnabled())
            log.debug("Connection " + id + ": executing query \"" + sql + "\"");

        ResultSet r = statement.executeQuery(sql);

        results = new PooledResultSet(connection, statement, r, id);
        return results;
    }

    public int executeUpdate(String sql)
        throws SQLException
    {
        validate();
        if (log.isDebugEnabled())
            log.debug("Connection " + id + ": executing update \"" + sql + "\"");

        return statement.executeUpdate(sql);
    }

    public void close()
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Connection " + id + ": closing statement");

        if (statement != null)
            statement.close();
        statement = null;
        connection = null;
        if (results != null)
            results.close();
        results = null;
        closed = true;
    }

    public int getMaxFieldSize()
        throws SQLException
    {
        validate();
        return statement.getMaxFieldSize();
    }

    public void setMaxFieldSize(int i)
        throws SQLException
    {
        validate();
        statement.setMaxFieldSize(i);
    }

    public int getMaxRows()
        throws SQLException
    {
        validate();
        return statement.getMaxRows();
    }

    public void setMaxRows(int i)
        throws SQLException
    {
        validate();
        statement.setMaxRows(i);
    }

    public void setEscapeProcessing(boolean b)
        throws SQLException
    {
        validate();
        statement.setEscapeProcessing(b);
    }

    public int getQueryTimeout()
        throws SQLException
    {
        validate();
        return statement.getQueryTimeout();
    }

    public void setQueryTimeout(int i)
        throws SQLException
    {
        validate();
        statement.setQueryTimeout(i);
    }

    public void cancel()
        throws SQLException
    {
        validate();
        statement.cancel();
    }

    public SQLWarning getWarnings()
        throws SQLException
    {
        validate();
        return statement.getWarnings();
    }

    public void clearWarnings()
        throws SQLException
    {
        validate();
        statement.clearWarnings();
    }

    public void setCursorName(String c)
        throws SQLException
    {
        validate();
        statement.setCursorName(c);
    }

    public boolean execute(String sql)
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Connection " + id + ": called execute with sql \"" + sql + "\"");

        validate();

        return statement.execute(sql);
    }

    public ResultSet getResultSet()
        throws SQLException
    {
        validate();

        return results;
    }

    public int getUpdateCount()
        throws SQLException
    {
        validate();
        return statement.getUpdateCount();
    }

    public boolean getMoreResults()
        throws SQLException
    {
        validate();
        return statement.getMoreResults();
    }

    public void setFetchDirection(int i)
        throws SQLException
    {
        validate();
        statement.setFetchDirection(i);
    }

    public int getFetchDirection()
        throws SQLException
    {
        validate();
        return statement.getFetchDirection();
    }

    public void setFetchSize(int i)
        throws SQLException
    {
        validate();
        statement.setFetchSize(i);
    }

    public int getFetchSize()
        throws SQLException
    {
        validate();
        return statement.getFetchSize();
    }

    public int getResultSetConcurrency()
        throws SQLException
    {
        validate();
        return statement.getResultSetConcurrency();
    }

    public int getResultSetType()
        throws SQLException
    {
        validate();
        return statement.getResultSetType();
    }

    public void addBatch(String b)
        throws SQLException
    {
        validate();
        statement.addBatch(b);
    }

    public void clearBatch()
        throws SQLException
    {
        validate();
        statement.clearBatch();
    }

    public int[] executeBatch()
        throws SQLException
    {
        validate();
        return statement.executeBatch();
    }

    public Connection getConnection()
        throws SQLException
    {
        validate();
        return connection;
    }
}


// ResultSet wrapper
class PooledResultSet implements java.sql.ResultSet
{
    private static Category log = Category.getInstance(PooledResultSet.class);
    private java.sql.ResultSet  delegate;
    private java.sql.Statement  s;
    private java.sql.Connection c;
    private int id;
    private boolean closed = false;

    public PooledResultSet(Connection c, Statement s, java.sql.ResultSet r, int id)
    {
        this.c = c;
        this.s = s;
        this.id = id;
        this.delegate = r;
    }

    public void finalize()
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Called finalize on resultset with connection " + id);

        close();
    }

    ////////////////////////////////////////
    // Non-interface methods
    ////////////////////////////////////////

    /**
     * True if the ResultSet is in use
     */
    public boolean isBeingUsed()
    {
        return delegate != null;
    }

    public void validate()
    {
        if (closed)
            if (log.isDebugEnabled())
                log.debug("Alert: connection " + id + " using closed resultset");
    }

    ////////////////////////////////////////
    // Delegated methods
    ////////////////////////////////////////

    public java.sql.Timestamp getTimestamp
    (java.lang.String p1, java.util.Calendar p2)
        throws SQLException
    {
        validate();

        return delegate.getTimestamp(p1, p2);
    }

    public java.sql.Timestamp getTimestamp
    (int p1, java.util.Calendar p2)
        throws SQLException
    {
        validate();
        return delegate.getTimestamp(p1, p2);
    }

    public java.sql.Time getTime(String p1, java.util.Calendar p2)
        throws SQLException
    {
        validate();
        return delegate.getTime(p1, p2);
    }

    public java.sql.Time getTime(int p1, java.util.Calendar p2)
        throws SQLException
    {
        validate();
        return delegate.getTime(p1, p2);
    }

    public java.sql.Date getDate(String p1, java.util.Calendar p2)
        throws SQLException
    {
        validate();
        return delegate.getDate(p1, p2);
    }

    public java.sql.Date getDate(int p1, java.util.Calendar p2)
        throws SQLException
    {
        validate();
        return delegate.getDate(p1, p2);
    }

    public java.sql.Array getArray(String p1)
        throws SQLException
    {
        validate();
        return delegate.getArray(p1);
    }

    public java.sql.Clob getClob(String p1)
        throws SQLException
    {
        validate();
        return delegate.getClob(p1);
    }

    public java.sql.Blob getBlob(String p1)
        throws SQLException
    {
        validate();
        return delegate.getBlob(p1);
    }

    public java.sql.Ref getRef(String p1)
        throws SQLException
    {
        validate();
        return delegate.getRef(p1);
    }

    public Object getObject(String p1, java.util.Map p2)
        throws SQLException
    {
        validate();
        return delegate.getObject(p1, p2);
    }

    public java.sql.Array getArray(int p1)
        throws SQLException
    {
        validate();
        return delegate.getArray(p1);
    }

    public java.sql.Clob getClob(int p1)
        throws SQLException
    {
        validate();
        return delegate.getClob(p1);
    }

    public java.sql.Blob getBlob(int p1)
        throws SQLException
    {
        validate();
        return delegate.getBlob(p1);
    }

    public java.sql.Ref getRef(int p1)
        throws SQLException
    {
        validate();
        return delegate.getRef(p1);
    }

    public Object getObject(int p1, java.util.Map p2)
        throws SQLException
    {
        validate();
        return delegate.getObject(p1, p2);
    }

    public java.sql.Statement getStatement()
        throws SQLException
    {
        validate();
        return s;
    }

    public void moveToCurrentRow()
        throws SQLException
    {
        validate();
        delegate.moveToCurrentRow();
    }

    public void moveToInsertRow()
        throws SQLException
    {
        validate();
        delegate.moveToInsertRow();
    }

    public void cancelRowUpdates()
        throws SQLException
    {
        validate();
        delegate.cancelRowUpdates();
    }

    public void refreshRow()
        throws SQLException
    {
        validate();
        delegate.refreshRow();
    }

    public void deleteRow()
        throws SQLException
    {
        validate();
        delegate.deleteRow();
    }

    public void updateRow()
        throws SQLException
    {
        validate();
        delegate.updateRow();
    }

    public void insertRow()
        throws SQLException
    {
        validate();
        delegate.insertRow();
    }

    public void updateObject(String p1, Object p2)
        throws SQLException
    {
        validate();
        delegate.updateObject(p1, p2);
    }

    public void updateObject(String p1, Object p2, int p3)
        throws SQLException
    {
        validate();
        delegate.updateObject(p1, p2, p3);
    }

    public void updateCharacterStream(String p1, java.io.Reader p2, int p3)
        throws SQLException
    {
        validate();
        delegate.updateCharacterStream(p1, p2, p3);
    }

    public void updateBinaryStream(String p1, java.io.InputStream p2, int p3)
        throws SQLException
    {
        validate();
        delegate.updateBinaryStream(p1, p2, p3);
    }

    public void updateAsciiStream(String p1, java.io.InputStream p2, int p3)
        throws SQLException
    {
        validate();
        delegate.updateAsciiStream(p1, p2, p3);
    }

    public void updateTimestamp(String p1, java.sql.Timestamp p2)
        throws SQLException
    {
        validate();
        delegate.updateTimestamp(p1, p2);
    }

    public void updateTime(String p1, java.sql.Time p2)
        throws SQLException
    {
        validate();
        delegate.updateTime(p1, p2);
    }

    public void updateDate(String p1, java.sql.Date p2)
        throws SQLException
    {
        validate();
        delegate.updateDate(p1, p2);
    }

    public void updateBytes(String p1, byte[] p2)
        throws SQLException
    {
        validate();
        delegate.updateBytes(p1, p2);
    }

    public void updateString(String p1, String p2)
        throws SQLException
    {
        validate();
        delegate.updateString(p1, p2);
    }

    public void updateBigDecimal(String p1, java.math.BigDecimal p2)
        throws SQLException
    {
        validate();
        delegate.updateBigDecimal(p1, p2);
    }

    public void updateDouble(String p1, double p2)
        throws SQLException
    {
        validate();
        delegate.updateDouble(p1, p2);
    }

    public void updateFloat(String p1, float p2)
        throws SQLException
    {
        validate();
        delegate.updateFloat(p1, p2);
    }

    public void updateLong(String p1, long p2)
        throws SQLException
    {
        validate();
        delegate.updateLong(p1, p2);
    }

    public void updateInt(String p1, int p2)
        throws SQLException
    {
        validate();
        delegate.updateInt(p1, p2);
    }

    public void updateShort(String p1, short p2)
        throws SQLException
    {
        validate();
        delegate.updateShort(p1, p2);
    }

    public void updateByte(String p1, byte p2)
        throws SQLException
    {
        validate();
        delegate.updateByte(p1, p2);
    }

    public void updateBoolean(String p1, boolean p2)
        throws SQLException
    {
        validate();
        delegate.updateBoolean(p1, p2);
    }

    public void updateNull(String p1)
        throws SQLException
    {
        validate();
        delegate.updateNull(p1);
    }

    public void updateObject(int p1, Object p2)
        throws SQLException
    {
        validate();
        delegate.updateObject(p1, p2);
    }

    public void updateObject(int p1, Object p2, int p3)
        throws SQLException
    {
        validate();
        delegate.updateObject(p1, p2, p3);
    }

    public void updateCharacterStream(int p1, java.io.Reader p2, int p3)
        throws SQLException
    {
        validate();
        delegate.updateCharacterStream(p1, p2, p3);
    }

    public void updateBinaryStream(int p1, java.io.InputStream p2, int p3)
        throws SQLException
    {
        validate();
        delegate.updateBinaryStream(p1, p2, p3);
    }

    public void updateAsciiStream(int p1, java.io.InputStream p2, int p3)
        throws SQLException
    {
        validate();
        delegate.updateAsciiStream(p1, p2, p3);
    }

    public void updateTimestamp(int p1, java.sql.Timestamp p2)
        throws SQLException
    {
        validate();
        delegate.updateTimestamp(p1, p2);
    }

    public void updateTime(int p1, java.sql.Time p2)
        throws SQLException
    {
        validate();
        delegate.updateTime(p1, p2);
    }

    public void updateDate(int p1, java.sql.Date p2)
        throws SQLException
    {
        validate();
        delegate.updateDate(p1, p2);
    }

    public void updateBytes(int p1, byte[] p2)
        throws SQLException
    {
        validate();
        delegate.updateBytes(p1, p2);
    }

    public void updateString(int p1, String p2)
        throws SQLException
    {
        validate();
        delegate.updateString(p1, p2);
    }

    public void updateBigDecimal(int p1, java.math.BigDecimal p2)
        throws SQLException
    {
        validate();
        delegate.updateBigDecimal(p1, p2);
    }

    public void updateDouble(int p1, double p2)
        throws SQLException
    {
        validate();
        delegate.updateDouble(p1, p2);
    }

    public void updateFloat(int p1, float p2)
        throws SQLException
    {
        validate();
        delegate.updateFloat(p1, p2);
    }

    public void updateLong(int p1, long p2)
        throws SQLException
    {
        validate();
        delegate.updateLong(p1, p2);
    }

    public void updateInt(int p1, int p2)
        throws SQLException
    {
        validate();
        delegate.updateInt(p1, p2);
    }

    public void updateShort(int p1, short p2)
        throws SQLException
    {
        validate();
        delegate.updateShort(p1, p2);
    }

    public void updateByte(int p1, byte p2)
        throws SQLException
    {
        validate();
        delegate.updateByte(p1, p2);
    }

    public void updateBoolean(int p1, boolean p2)
        throws SQLException
    {
        validate();
        delegate.updateBoolean(p1, p2);
    }

    public void updateNull(int p1)
        throws SQLException
    {
        validate();
        delegate.updateNull(p1);
    }

    public boolean rowDeleted()
        throws SQLException
    {
        validate();
        return delegate.rowDeleted();
    }

    public boolean rowInserted()
        throws SQLException
    {
        validate();
        return delegate.rowInserted();
    }

    public boolean rowUpdated()
        throws SQLException
    {
        validate();
        return delegate.rowUpdated();
    }

    public int getConcurrency()
        throws SQLException
    {
        validate();
        return delegate.getConcurrency();
    }

    public int getType()
        throws SQLException
    {
        validate();
        return delegate.getType();
    }

    public int getFetchSize()
        throws SQLException
    {
        validate();
        return delegate.getFetchSize();
    }

    public void setFetchSize(int p1)
        throws SQLException
    {
        validate();
        delegate.setFetchSize(p1);
    }

    public int getFetchDirection()
        throws SQLException
    {
        validate();
        return delegate.getFetchDirection();
    }

    public void setFetchDirection(int p1)
        throws SQLException
    {
        validate();
        delegate.setFetchDirection(p1);
    }

    public boolean previous()
        throws SQLException
    {
        validate();
        return delegate.previous();
    }

    public boolean relative(int p1)
        throws SQLException
    {
        validate();
        return delegate.relative(p1);
    }

    public boolean absolute(int p1)
        throws SQLException
    {
        validate();
        return delegate.absolute(p1);
    }

    public int getRow()
        throws SQLException
    {
        validate();
        return delegate.getRow();
    }

    public boolean last()
        throws SQLException
    {
        validate();
        return delegate.last();
    }

    public boolean first()
        throws SQLException
    {
        validate();
        return delegate.first();
    }

    public void afterLast()
        throws SQLException
    {
        validate();
        delegate.afterLast();
    }

    public void beforeFirst()
        throws SQLException
    {
        validate();
        delegate.beforeFirst();
    }

    public boolean isLast()
        throws SQLException
    {
        validate();
        return delegate.isLast();
    }

    public boolean isFirst()
        throws SQLException
    {
        validate();
        return delegate.isFirst();
    }

    public boolean isAfterLast()
        throws SQLException
    {
        validate();
        return delegate.isAfterLast();
    }

    public boolean isBeforeFirst()
        throws SQLException
    {
        validate();
        return delegate.isBeforeFirst();
    }

    public java.math.BigDecimal getBigDecimal(String p1)
        throws SQLException
    {
        validate();
        return delegate.getBigDecimal(p1);
    }

    public java.math.BigDecimal getBigDecimal(int p1)
        throws SQLException
    {
        validate();
        return delegate.getBigDecimal(p1);
    }

    public java.io.Reader getCharacterStream(String p1)
        throws SQLException
    {
        validate();
        return delegate.getCharacterStream(p1);
    }

    public java.io.Reader getCharacterStream(int p1)
        throws SQLException
    {
        validate();
        return delegate.getCharacterStream(p1);
    }

    public int findColumn(String p1)
        throws SQLException
    {
        validate();
        return delegate.findColumn(p1);
    }

    public Object getObject(String p1)
        throws SQLException
    {
        validate();
        return delegate.getObject(p1);
    }

    public Object getObject(int p1)
        throws SQLException
    {
        validate();
        return delegate.getObject(p1);
    }

    public java.sql.ResultSetMetaData getMetaData()
        throws SQLException
    {
        validate();
        return delegate.getMetaData();
    }

    public String getCursorName()
        throws SQLException
    {
        validate();
        return delegate.getCursorName();
    }

    public void clearWarnings()
        throws SQLException
    {
        validate();
        delegate.clearWarnings();
    }

    public java.sql.SQLWarning getWarnings()
        throws SQLException
    {
        validate();
        return delegate.getWarnings();
    }

    public java.io.InputStream getBinaryStream(String p1)
        throws SQLException
    {
        validate();
        return delegate.getBinaryStream(p1);
    }

    public java.io.InputStream getUnicodeStream(String p1)
        throws SQLException
    {
        validate();
        return delegate.getUnicodeStream(p1);
    }

    public java.io.InputStream getAsciiStream(String p1)
        throws SQLException
    {
        validate();
        return delegate.getAsciiStream(p1);
    }

    public java.sql.Timestamp getTimestamp(String p1)
        throws SQLException
    {
        validate();
        return delegate.getTimestamp(p1);
    }

    public java.sql.Time getTime(String p1)
        throws SQLException
    {
        validate();
        return delegate.getTime(p1);
    }

    public java.sql.Date getDate(String p1)
        throws SQLException
    {
        validate();
        return delegate.getDate(p1);
    }

    public byte[] getBytes(String p1)
        throws SQLException
    {
        validate();
        return delegate.getBytes(p1);
    }

    public java.math.BigDecimal getBigDecimal(String p1, int p2)
        throws SQLException
    {
        validate();
        return delegate.getBigDecimal(p1, p2);
    }

    public double getDouble(String p1)
        throws SQLException
    {
        validate();
        return delegate.getDouble(p1);
    }

    public float getFloat(String p1)
        throws SQLException
    {
        validate();
        return delegate.getFloat(p1);
    }

    public long getLong(String p1)
        throws SQLException
    {
        validate();
        return delegate.getLong(p1);
    }

    public int getInt(String p1)
        throws SQLException
    {
        validate();
        return delegate.getInt(p1);
    }

    public short getShort(String p1)
        throws SQLException
    {
        validate();
        return delegate.getShort(p1);
    }

    public byte getByte(String p1)
        throws SQLException
    {
        validate();
        return delegate.getByte(p1);
    }

    public boolean getBoolean(String p1)
        throws SQLException
    {
        validate();
        return delegate.getBoolean(p1);
    }

    public String getString(String p1)
        throws SQLException
    {
        validate();
        return delegate.getString(p1);
    }

    public java.io.InputStream getBinaryStream(int p1)
        throws SQLException
    {
        validate();
        return delegate.getBinaryStream(p1);
    }

    public java.io.InputStream getUnicodeStream(int p1)
        throws SQLException
    {
        validate();
        return delegate.getUnicodeStream(p1);
    }

    public java.io.InputStream getAsciiStream(int p1)
        throws SQLException
    {
        validate();
        return delegate.getAsciiStream(p1);
    }

    public java.sql.Timestamp getTimestamp(int p1)
        throws SQLException
    {
        validate();
        return delegate.getTimestamp(p1);
    }

    public java.sql.Time getTime(int p1)
        throws SQLException
    {
        validate();
        return delegate.getTime(p1);
    }

    public java.sql.Date getDate(int p1)
        throws SQLException
    {
        validate();
        return delegate.getDate(p1);
    }

    public byte[] getBytes(int p1)
        throws SQLException
    {
        validate();
        return delegate.getBytes(p1);
    }

    public java.math.BigDecimal getBigDecimal(int p1, int p2)
        throws SQLException
    {
        validate();
        return delegate.getBigDecimal(p1, p2);
    }

    public double getDouble(int p1)
        throws SQLException
    {
        validate();
        return delegate.getDouble(p1);
    }

    public float getFloat(int p1)
        throws SQLException
    {
        validate();
        return delegate.getFloat(p1);
    }

    public long getLong(int p1)
        throws SQLException
    {
        validate();
        return delegate.getLong(p1);
    }

    public int getInt(int p1)
        throws SQLException
    {
        validate();
        return delegate.getInt(p1);
    }

    public short getShort(int p1)
        throws SQLException
    {
        validate();
        return delegate.getShort(p1);
    }

    public byte getByte(int p1)
        throws SQLException
    {
        validate();
        return delegate.getByte(p1);
    }

    public boolean getBoolean(int p1)
        throws SQLException
    {
        validate();
        return delegate.getBoolean(p1);
    }

    public String getString(int p1)
        throws SQLException
    {
        validate();
        return delegate.getString(p1);
    }

    public boolean wasNull()
        throws SQLException
    {
        validate();
        return delegate.wasNull();
    }

    public void close()
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Connection " + id + ": closing resultset");

        if (delegate != null)
            delegate.close();

        delegate = null;
        s = null;
        c = null;
        closed = true;
    }

    public boolean next()
        throws SQLException
    {
        validate();
        return delegate.next();
    }
}
