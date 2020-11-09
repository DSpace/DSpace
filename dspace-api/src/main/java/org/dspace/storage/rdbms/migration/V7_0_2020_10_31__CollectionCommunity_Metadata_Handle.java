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
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * This class automatically adding rptype to the resource policy created with a migration into XML-based Configurable
 * Workflow system
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

        final String SQL_INSERT = "insert into metadatavalue "
                + " (metadata_field_id, text_value, text_lang, place, authority, confidence, dspace_object_id) "

                + " select distinct T1.metadata_field_id as metadata_field_id, concat(?, h.handle) as text_value, "
                + "  null as text_lang, 0 as place, null as authority, -1 as confidence, c.uuid as dspace_object_id  "

                + "  from %s c "
                + "  left outer join handle h on h.resource_id = c.uuid "
                + "  left outer join metadatavalue mv on mv.dspace_object_id = c.uuid "
                + "  left outer join metadatafieldregistry mfr on mv.metadata_field_id = mfr.metadata_field_id "
                + "  left outer join metadataschemaregistry msr on mfr.metadata_schema_id = msr.metadata_schema_id "

                + "  cross join (select mfr.metadata_field_id as metadata_field_id from metadatafieldregistry mfr "
                + "     left outer join metadataschemaregistry msr on mfr.metadata_schema_id = msr.metadata_schema_id "
                + "     where msr.short_id = 'dc' "
                + "      and mfr.element = 'identifier' "
                + "      and mfr.qualifier = 'uri') T1 "
                + ";";

        final String COLLECTIONS_SQL_INDEX = String.format(SQL_INSERT, "collection");
        final String COMMUNITIES_SQL_INDEX = String.format(SQL_INSERT, "community");

        try {
            new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true))
                .update(COLLECTIONS_SQL_INDEX, new PreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps) throws SQLException {
                        ps.setString(1, prefix);
                    }
                });

            new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true))
                .update(COMMUNITIES_SQL_INDEX, new PreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps) throws SQLException {
                        ps.setString(1, prefix);
                    }
                });
        } catch (DataAccessException dae) {
            throw new SQLException("Flyway executeSql() error occurred", dae);
        }

        migration_file_size = COLLECTIONS_SQL_INDEX.length() + COMMUNITIES_SQL_INDEX.length();

    }

    @Override
    public Integer getChecksum() {
        return migration_file_size;
    }
}
