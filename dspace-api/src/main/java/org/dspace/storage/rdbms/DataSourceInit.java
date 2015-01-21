/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.commons.dbcp.*;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import java.util.*;
import org.apache.commons.lang.StringUtils;

public class DataSourceInit {
    private static final Logger log = Logger.getLogger(DataSourceInit.class);

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
                    null, // validation query (none until we know DBMS brand)
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

            // Set the proper validation query by DBMS brand.
            // For reference see: http://stackoverflow.com/a/10684260/3750035
            Connection connection = dataSource.getConnection();
            String productNameLC = connection.getMetaData().getDatabaseProductName()
                    .toLowerCase();
            if (productNameLC.contains("oracle"))
            {
                poolableConnectionFactory.setValidationQuery("SELECT 1 FROM DUAL");
            }
            else
            {
                poolableConnectionFactory.setValidationQuery("SELECT 1");
    
                String dbSchema = ConfigurationManager.getProperty("db.schema");
                if (StringUtils.isBlank(dbSchema) != true)
                {
                    List initSql = Arrays.asList("set search_path to ".concat(dbSchema));
                    poolableConnectionFactory.setConnectionInitSql(initSql);
                }

            }
            connection.close();

            poolableConnectionFactory.getPool().clear();

            // Ready to use
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
