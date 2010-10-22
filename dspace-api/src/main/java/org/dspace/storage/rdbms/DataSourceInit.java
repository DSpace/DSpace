/*
 * DataSourceInit.java
 *
 * Version: $Revision: 5241 $
 *
 * Date: $Date: 2010-08-05 20:56:37 +0100 (Thu, 05 Aug 2010) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

import org.apache.commons.dbcp.*;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import javax.sql.DataSource;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSourceInit {
    private static Logger log = Logger.getLogger(DataSourceInit.class);

    private static DataSource dataSource = null;

    public static DataSource getDatasource() throws SQLException
    {
        if (dataSource != null)
        {
            return dataSource;
        }

        try
        {
            // Register basic JDBC driver
            Class driverClass = Class.forName(ConfigurationManager
                    .getProperty("db.driver"));
            Driver basicDriver = (Driver) driverClass.newInstance();
            DriverManager.registerDriver(basicDriver);

            // Read pool configuration parameter or use defaults
            // Note we check to see if property is null; getIntProperty returns
            // '0' if the property is not set OR if it is actually set to zero.
            // But 0 is a valid option...
            int maxConnections = ConfigurationManager
                    .getIntProperty("db.maxconnections");

            if (ConfigurationManager.getProperty("db.maxconnections") == null)
            {
                maxConnections = 30;
            }

            int maxWait = ConfigurationManager.getIntProperty("db.maxwait");

            if (ConfigurationManager.getProperty("db.maxwait") == null)
            {
                maxWait = 5000;
            }

            int maxIdle = ConfigurationManager.getIntProperty("db.maxidle");

            if (ConfigurationManager.getProperty("db.maxidle") == null)
            {
                maxIdle = -1;
            }

            boolean useStatementPool = ConfigurationManager.getBooleanProperty("db.statementpool",true);

            // Create object pool
            ObjectPool connectionPool = new GenericObjectPool(null, // PoolableObjectFactory
                    // - set below
                    maxConnections, // max connections
                    GenericObjectPool.WHEN_EXHAUSTED_BLOCK, maxWait, // don't
                                                                     // block
                    // more than 5
                    // seconds
                    maxIdle, // max idle connections (unlimited)
                    true, // validate when we borrow connections from pool
                    false // don't bother validation returned connections
            );

            // ConnectionFactory the pool will use to create connections.
            ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
                    ConfigurationManager.getProperty("db.url"),
                    ConfigurationManager.getProperty("db.username"),
                    ConfigurationManager.getProperty("db.password"));

            //
            // Now we'll create the PoolableConnectionFactory, which wraps
            // the "real" Connections created by the ConnectionFactory with
            // the classes that implement the pooling functionality.
            //
            String validationQuery = "SELECT 1";

            // Oracle has a slightly different validation query
            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
            {
                validationQuery = "SELECT 1 FROM DUAL";
            }

            GenericKeyedObjectPoolFactory statementFactory = null;
            if (useStatementPool)
            {
                // The statement Pool is used to pool prepared statements.
                GenericKeyedObjectPool.Config statementFactoryConfig = new GenericKeyedObjectPool.Config();
                // Just grow the pool size when needed.
                //
                // This means we will never block when attempting to
                // create a query. The problem is unclosed statements,
                // they can never be reused. So if we place a maximum
                // cap on them, then we might reach a condition where
                // a page can only be viewed X number of times. The
                // downside of GROW_WHEN_EXHAUSTED is that this may
                // allow a memory leak to exist. Both options are bad,
                // but I'd prefer a memory leak over a failure.
                //
                // FIXME: Perhaps this decision should be derived from config parameters?
                statementFactoryConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;

                statementFactory = new GenericKeyedObjectPoolFactory(null,statementFactoryConfig);
            }

            PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
                    connectionFactory, connectionPool, statementFactory,
                    validationQuery, // validation query
                    false, // read only is not default for now
                    false); // Autocommit defaults to none

            //
            // Finally, we create the PoolingDataSource itself...
            //
            PoolingDataSource poolingDataSource = new PoolingDataSource();

            //
            // ...and register our pool with it.
            //
            poolingDataSource.setPool(connectionPool);

            dataSource = poolingDataSource;
            return poolingDataSource;
        }
        catch (Exception e)
        {
            // Need to be able to catch other exceptions. Pretend they are
            // SQLExceptions, but do log
            log.warn("Exception initializing DB pool", e);
            throw new SQLException(e.toString(), e);
        }
    }
}
