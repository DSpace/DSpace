/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Insert a 'dc.idendifier.uri' metadata record for each Community and Collection in the database.
 * The value is calculated concatenating the canonicalPrefix extracted from the configuration
 * (default is "http://hdl.handle.net/) and the object's handle suffix stored inside the handle table.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class V7_0_2020_10_31__CollectionCommunity_Metadata_Handle extends BaseJavaMigration {
    // Size of migration script run
    protected Integer migration_file_size = -1;

    @Override
    public void migrate(Context context) throws Exception {

        HandleService handleService = DSpaceServicesFactory
                .getInstance().getServiceManager().getServicesByType(HandleService.class).get(0);

        final String prefix = handleService.getCanonicalPrefix();

        // Step 1 Preparation (creation of the tmp_handleprefix containing one record with the prefix)
        String createTmpTable = "CREATE TABLE tmp_handleprefix (handleprefix text NOT NULL);";
        DatabaseUtils.executeSql(context.getConnection(), createTmpTable);

        // Step 2 Preparation (insertion of the prefix value into tmp_handleprefix)
        String insertPrefixSql = "INSERT INTO tmp_handleprefix (handleprefix) VALUES (?)";
        try {
            new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true))
                .update(insertPrefixSql, new PreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps) throws SQLException {
                        ps.setString(1, prefix);
                    }
                });
        } catch (DataAccessException dae) {
            throw new SQLException("Flyway executeSql() error occurred", dae);
        }

        // Step 3 Populate (sql script execution)
        String dbtype = DatabaseUtils.getDbType(context.getConnection());
        String sqlMigrationPath = "org/dspace/storage/rdbms/sqlmigration/metadata/" + dbtype + "/";
        String dataMigrateSQL = MigrationUtils.getResourceAsString(
                sqlMigrationPath + "V7.0_2020.10.31__CollectionCommunity_Metadata_Handle.sql");
        DatabaseUtils.executeSql(context.getConnection(), dataMigrateSQL);

        migration_file_size = dataMigrateSQL.length();

    }

    @Override
    public Integer getChecksum() {
        return migration_file_size;
    }
}
