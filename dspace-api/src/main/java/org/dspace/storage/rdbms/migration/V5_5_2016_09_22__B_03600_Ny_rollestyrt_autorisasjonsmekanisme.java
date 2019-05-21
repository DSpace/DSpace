package org.dspace.storage.rdbms.migration;

import org.apache.log4j.Logger;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class V5_5_2016_09_22__B_03600_Ny_rollestyrt_autorisasjonsmekanisme implements JdbcMigration, MigrationChecksumProvider {

    /** log4j category */
    private static final Logger log = Logger.getLogger(V5_5_2016_09_22__B_03600_Ny_rollestyrt_autorisasjonsmekanisme.class);

    /* The checksum to report for this migration (when successful) */
    private int checksum = -1;

    private final Map<String, Integer> resourceCache = new HashMap<>();


    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement dropBibkodeColumn = connection.createStatement()){
            dropBibkodeColumn.execute("CREATE OR REPLACE FUNCTION remove_bibkode() RETURNS VOID AS " +
                    "$$ " +
                    "BEGIN " +
                    "ALTER TABLE eperson DROP COLUMN bibkode; " +
                    "EXCEPTION WHEN UNDEFINED_COLUMN THEN RAISE NOTICE 'bibkode column does not exist in eperson table'; " +
                    "END; " +
                    "$$ LANGUAGE plpgsql VOLATILE; " +
                    "SELECT remove_bibkode(); " +
                    "DROP FUNCTION remove_bibkode();");
        }
    }

    @Override
    public Integer getChecksum() {
        return checksum;
    }
}
