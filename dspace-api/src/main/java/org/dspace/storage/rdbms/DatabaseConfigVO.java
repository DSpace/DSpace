/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

/**
 * Get database configuration parameters.  The parameter values are retrieved by
 * the configured implementation of the {@link DBConnection} interface.
 *
 * <p>This class <strong>cannot</strong> be used to <em>alter</em> configuration;
 * it is just used to export and can be used for display purposes
 *
 * @author kevinvandevelde at atmire.com
 */
public class DatabaseConfigVO {

    private String databaseUrl;

    private String databaseDriver;

    private String userName;

    private String schema;

    private int maxConnections;

    public DatabaseConfigVO()
    {

    }

    /**
     * Get the JDBC URL which identifies the DBMS instance and database.  This
     * is set in the DSpace configuration.
     * @return URL pointing to the configured database.
     */
    public String getDatabaseUrl() {
        return databaseUrl;
    }

    /**
     * DO NOT USE unless you are writing a DBConnection implementation.  This
     * method does not set the URL that will be used to connect to the database.
     * @param databaseUrl JDBC URL being used by the DBConnection for creating connections.
     */
    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    /**
     * Get the name of the DBMS driver, which should indicate what DBMS is in use.
     * @return the driver's notion of its "name".
     */
    public String getDatabaseDriver() {
        return databaseDriver;
    }

    /**
     * DO NOT USE unless you are writing an implementation of DBConnection.
     * This method does not select the DBMS driver.
     * @param databaseDriver the driver's name.
     */
    public void setDatabaseDriver(String databaseDriver) {
        this.databaseDriver = databaseDriver;
    }

    /**
     * Get the name of the database role used to authenticate connections to the DBMS.
     * @return DBMS user name, from DSpace configuration.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * DO NOT USE unless you are writing an implementation of DBConnection.
     * This method does not alter the user name.
     * @param userName the configured DBMS username.
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Get the name of the database schema.
     * @return name of the schema.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * DO NOT USE unless you are writing an implementation of DBConnection.
     * This method does not set the schema that will be used.
     * @param schema name of the database schema, from configuration.
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Get the maximum number of concurrent DBMS connections that will be opened (if possible).
     * @return configured maximum DBMS connection count.
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * DO NOT USE unless you are writing an implementation of DBConnection.
     * This method does not set the connection maximum.
     * @param maxConnections configured maximum number of concurrent DBMS connections.
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
}
