/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

/**
 * Value object for the Database configuration, can be used to get the database configuration parameters.
 * The config parameters are retrieved by the implementation of the org.dspace.core.DBConnection object.
 * This class should never be used to store configuration, it is just used to export & can be used for display purposes
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

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public String getDatabaseDriver() {
        return databaseDriver;
    }

    public void setDatabaseDriver(String databaseDriver) {
        this.databaseDriver = databaseDriver;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
}
