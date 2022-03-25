/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.identifier.DOI;
import org.dspace.identifier.EZIDIdentifierProvider;
import org.dspace.identifier.IdentifierProvider;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * A Flyway migration to fix incorrect use of {@code dc.identifier} for holding
 * DSpace-generated EZID DOIs.  That field is reserved for user-provided
 * identifiers.  Earlier versions of EZID support incorrectly used that field
 * to hold DOIs that it generates.
 *
 * @author mwood
 */
public class V6_0_2016_04_01_0002__DS_2199_Move_Identifiers
        extends BaseJavaMigration {
    private static final String SCHEMA_TABLE = "MetadataSchemaRegistry";

    static final String OLD_SCHEMA = MetadataSchemaEnum.DC.getName();
    static final String OLD_ELEMENT = "identifier";
    static final String OLD_QUALIFIER = null;
    static final String OLD_LANG = null;

    static final String NEW_SCHEMA = IdentifierProvider.URI_METADATA_SCHEMA;
    static final String NEW_ELEMENT = IdentifierProvider.URI_METADATA_ELEMENT;
    static final String NEW_QUALIFIER = IdentifierProvider.URI_METADATA_QUALIFIER;
    static final String NEW_LANG = null;

    private static final Logger LOG = LogManager.getLogger();

    private final Checksum checksum = new CRC32();

    @Override
    public void migrate(Context context)
            throws Exception {
        /*
         * Bail out if there is no metadata schema registry or it is empty.
         * If there are no registered schemae,
         * then we are doing a fresh install or in a test,
         * and there can be no data to migrate.
         */
        Connection connection = context.getConnection();
        if (!DatabaseUtils.tableExists(connection, SCHEMA_TABLE)) {
            LOG.info("MetadataSchemaRegistry table does not exist, so there is nothing to migrate.");
            return;
        }

        // Find the field IDs for old and new fields
        final int old_field_id = getMetadataFieldId(connection,
                OLD_SCHEMA, OLD_ELEMENT, OLD_QUALIFIER);
        final int new_field_id = getMetadataFieldId(connection,
                NEW_SCHEMA, NEW_ELEMENT, NEW_QUALIFIER);
        if (old_field_id < 0 || new_field_id < 0) {
            LOG.info("Skipping because old ({}) or new ({}) field ID is undefined",
                    old_field_id, new_field_id);
            return;
        }

        // OK, the required fields exist, so we have work to do.
        final ConfigurationService cfg
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        final DOIService doiService
                = IdentifierServiceFactory.getInstance().getDOIService();

        final String prefix = DOI.SCHEME
                + cfg.getProperty(EZIDIdentifierProvider.CFG_SHOULDER)
                + '%';
        LOG.debug("Prefix = {}", prefix);

        String sql;
        if (null == OLD_LANG) {
            sql = "SELECT * FROM metadatavalue"
                    + " WHERE metadata_field_id = ?"
                    + " AND text_value LIKE ?"
                    + " AND text_lang IS NULL";
        } else {
            sql = "SELECT * FROM metadatavalue"
                    + " WHERE metadata_field_id = ?"
                    + " AND text_value LIKE ?"
                    + " AND text_lang = ?";
        }
        final PreparedStatement stmt = connection.prepareStatement(sql,
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

        stmt.setInt(1, old_field_id);
        stmt.setString(2, prefix);
        if (null != OLD_LANG) {
            stmt.setString(3, OLD_LANG);
        }

        final ResultSet rs = stmt.executeQuery();

        final int pos_field_id = rs.findColumn("metadata_field_id");
        final int pos_text_value = rs.findColumn("text_value");
        final int pos_text_lang = rs.findColumn("text_lang");

        try {
            while (rs.next()) {
                String oldIdentifier = "unknown";
                try {
                    oldIdentifier = rs.getString(pos_text_value);
                    String newIdentifier = doiService.DOIToExternalForm(oldIdentifier);
                    LOG.debug("Moving {} (as {}).", oldIdentifier, newIdentifier);

                    rs.updateInt(pos_field_id, new_field_id);
                    rs.updateString(pos_text_value, newIdentifier);
                    rs.updateString(pos_text_lang, NEW_LANG);
                    rs.updateRow();

                    checksum.update(newIdentifier.getBytes(), 0, newIdentifier.length());
                } catch (SQLException e) {
                    LOG.error("Skipped {}:  {}", oldIdentifier, e.getMessage());
                }
            }
        } finally {
            stmt.close();
        }
    }

    @Override
    public Integer getChecksum() {
        return (int)(checksum.getValue() & 0Xffffffff);
    }

    /**
     * Look up a metadata field ID given schema, element and qualifier.
     *
     * @param connection
     * @param schema
     * @param element
     * @param qualifier
     * @return the field ID, or -1 if not found.
     * @throws SQLException on a database error.
     */
    private int getMetadataFieldId(Connection connection, String schema,
            String element, String qualifier)
            throws SQLException {
        PreparedStatement stmt;
        final String SELECT_FIELD_ID_NULL_QUALIFIER
                = "SELECT f.metadata_field_id"
                + " FROM metadataschemaregistry s"
                + " JOIN metadatafieldregistry f ON s.metadata_schema_id = f.metadata_schema_id"
                + " WHERE s.short_id = ? AND f.element = ? AND f.qualifier IS NULL";
        final String SELECT_FIELD_ID_NONNULL_QUALIFIER
                = "SELECT f.metadata_field_id"
                + " FROM metadataschemaregistry s"
                + " JOIN metadatafieldregistry f ON s.metadata_schema_id = f.metadata_schema_id"
                + " WHERE s.short_id = ? AND f.element = ? AND f.qualifier = ?";
        if (null == qualifier) {
            stmt = connection.prepareStatement(SELECT_FIELD_ID_NULL_QUALIFIER);
        } else {
            stmt = connection.prepareStatement(SELECT_FIELD_ID_NONNULL_QUALIFIER);
            stmt.setString(3, qualifier);
        }
        stmt.setString(1, schema);
        stmt.setString(2, element);
        final ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            int id = rs.getInt(1);
            stmt.close();
            return id;
        } else {
            return -1;
        }
    }
}
