/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.constants;


/**
 * All core DSpace constants
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class Constants {

    /**
     * If this is set to true then DSpace kernel is run as if it is inside a unit test,
     * this means that nothing is persisted (things are run in-memory only) and 
     * caches and other speed optimizing parts are reduced,
     * this should NEVER be set in a production system
     */
    public static final String DSPACE_TESTING_MODE = "dspace.testing";
    /**
     * This is the name of the timer thread for all DSpace core timers
     */
    public static final String DSPACE_TIMER_NAME = "DSpaceTimer";

    /**
     * HSQLDB, DERBY, ORACLE, MYSQL, POSTGRES, DB2, MSSQL
     * Note that H2 uses HSQLDB key
     */
    public static final String DATABASE_TYPE_KEY = "jdbc.database.type";
    /**
     * Embedded drivers are:
     * org.h2.Driver
     * org.apache.derby.jdbc.EmbeddedDriver
     * org.hsqldb.jdbcDriver
     * Put your driver in the lib directory for your servlet container
     */
    public static final String DATABASE_DRIVER_KEY = "jdbc.driver.class";
    public static final String DATABASE_CONNECTION_KEY = "jdbc.connection.url";
    public static final String DATABASE_USERNAME_KEY = "jdbc.username";
    public static final String DATABASE_PASSWORD_KEY = "jdbc.password";
    /**
     * Set this to true to enable connection pooling, default true
     */
    public static final String DATABASE_CONN_POOLING = "jdbc.dataSource.pooling";
    
    public static final String DEFAULT_ENCODING = "UTF-8";

}
