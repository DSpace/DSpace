/*
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/constants/Constants.java $
 * 
 * $Revision: 3434 $
 * 
 * $Date: 2009-02-04 10:00:29 -0800 (Wed, 04 Feb 2009) $
 *
 * Copyright (c) 2008, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its 
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
