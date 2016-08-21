/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import org.dspace.handle.HandlePlugin;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This unifies the structured string we have in url.
 * We should reconsider the approach and use metadata tables or something.
 */
public class V5_6_2016_06_24__Handles_with_token_unify_structure
    implements JdbcMigration, MigrationChecksumProvider
{
    /* The checksum to report for this migration (when successful) */
    private int checksum = -1;

    /**
     * Actually migrate the existing database
     * @param connection
     */
    @Override
    public void migrate(Connection connection)
            throws SQLException
    {
        String query = "select * from handle where url like '" + HandlePlugin.magicBean + "%' ";
        PreparedStatement statement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        try {
            ResultSet results = statement.executeQuery();

            while (results.next()) {
                String url = results.getString("url");
                url = updateURL(url);
                results.updateString("url", url);
                results.updateRow();
            }
            //this doesn't help much but w/e. Change it if you touch the script!
            checksum = 1;
        } finally {
            statement.close();
        }
    }

    private String updateURL(String url) {
        String[] splits = url.split(HandlePlugin.magicBean);
        url = splits[splits.length - 1];
        String title = splits[1];
        String repository = splits[2];
        String submitdate = splits[3];
        String reportemail = splits[4];
        String datasetName = "";
        String datasetVersion = "";
        String query = "";
        String token = "";
        if(splits.length > 6) {
            datasetName = splits[5];
            datasetVersion = splits[6];
            query = splits[7];
        }
        if(splits.length > 9){
            token = splits[8];
        }
        String magicURL = "";
        for (String part : new String[]{title, repository, submitdate, reportemail, datasetName, datasetVersion, query, token, url}){
            magicURL += HandlePlugin.magicBean + part;
        }
        return magicURL;
    }

    /**
     * Return the checksum to be associated with this Migration
     * in the Flyway database table (schema_version).
     * @return checksum as an Integer
     */
    @Override
    public Integer getChecksum()
    {
        return checksum;
    }
}
