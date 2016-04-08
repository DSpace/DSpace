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
import org.dspace.content.MetadataSchema;
import org.dspace.identifier.DOI;
import org.dspace.identifier.EZIDIdentifierProvider;
import org.dspace.identifier.IdentifierProvider;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Flyway migration to fix incorrect use of {@code dc.identifier} for holding
 * DSpace-generated EZID DOIs.  That field is reserved for user-provided
 * identifiers.  Earlier versions of EZID support incorrectly used that field
 * to hold DOIs that it generates.
 *
 * @author mwood
 */
public class V6_0_2016_04_01__DS_2199_Move_Identifiers
    implements JdbcMigration, MigrationChecksumProvider
{
    private static final String SCHEMA_TABLE = "MetadataSchemaRegistry";

    private static final String OLD_SCHEMA = MetadataSchema.DC_SCHEMA;
    private static final String OLD_ELEMENT = "identifier";
    private static final String OLD_QUALIFIER = null;
    private static final String OLD_LANG = null;

    private static final String NEW_SCHEMA = IdentifierProvider.URI_METADATA_SCHEMA;
    private static final String NEW_ELEMENT = IdentifierProvider.URI_METADATA_ELEMENT;
    private static final String NEW_QUALIFIER = IdentifierProvider.URI_METADATA_QUALIFIER;
    private static final String NEW_LANG = null;

    private static final Logger LOG
            = LoggerFactory.getLogger(V6_0_2016_04_01__DS_2199_Move_Identifiers.class);

    private final Checksum checksum = new CRC32();

    @Override
    public void migrate(Connection cnctn)
            throws Exception
    {
        /*
         * Bail out if there is no metadata schema registry or it is empty.
         * If there are no registered schemae,
         * then we are doing a fresh install or in a test,
         * and there can be no data to migrate.
         */
        if (!DatabaseUtils.tableExists(cnctn, SCHEMA_TABLE))
        {
            LOG.info("MetadataSchemaRegistry table does not exist, so there is nothing to migrate.");
            return;
        }

        // Find the field IDs for old and new fields
        final int old_field_id = getMetadataFieldId(cnctn,
                OLD_SCHEMA, OLD_ELEMENT, OLD_QUALIFIER);
        final int new_field_id = getMetadataFieldId(cnctn,
                NEW_SCHEMA, NEW_ELEMENT, NEW_QUALIFIER);
        if (old_field_id < 0 || new_field_id < 0)
        {
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

        final PreparedStatement select = cnctn.prepareStatement(
                "SELECT * FROM metadatavalue"
                        + " WHERE metadata_field_id = ?"
                        + " AND text_lang = ?"
                        + " AND text_value LIKE ?");

        select.setInt(0, old_field_id);
        select.setString(1, OLD_LANG);
        select.setString(2, prefix);

        final ResultSet rs = select.executeQuery();

        final int pos_field_id = rs.findColumn("metadata_field_id");
        final int pos_text_value = rs.findColumn("text_value");
        final int pos_text_lang = rs.findColumn("text_lang");

        try {
            while (rs.next())
            {
                String oldIdentifier = "unknown";
                try {
                    oldIdentifier = rs.getString(pos_text_value);
                    String newIdentifier = doiService.DOIToExternalForm(oldIdentifier);
                    LOG.debug("Moving {} (as {}).", oldIdentifier, newIdentifier);

                    rs.updateInt(pos_field_id, new_field_id);
                    rs.updateString(pos_text_lang, NEW_LANG);
                    rs.updateString(pos_text_value, newIdentifier);
                    rs.updateRow();

                    checksum.update(newIdentifier.getBytes(), 0, newIdentifier.length());
                } catch (SQLException e) {
                    LOG.error("Skipped {}:  {}", oldIdentifier, e.getMessage());
                }
            }
        } finally {
            select.close();
        }
    }

    @Override
    public Integer getChecksum()
    {
        return (int)(checksum.getValue() & 0Xffffffff);
    }

    /**
     * Look up a metadata field ID given schema, element and qualifier.
     *
     * @param cnctn
     * @param schema
     * @param element
     * @param qualifier
     * @return the field ID, or -1 if not found.
     * @throws SQLException on a database error.
     */
    private int getMetadataFieldId(Connection cnctn, String schema,
            String element, String qualifier)
            throws SQLException
    {
        PreparedStatement select;
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
        if (null == qualifier)
        {
            select = cnctn.prepareStatement(SELECT_FIELD_ID_NULL_QUALIFIER);
        }
        else
        {
            select = cnctn.prepareStatement(SELECT_FIELD_ID_NONNULL_QUALIFIER);
            select.setString(3, qualifier);
        }
        select.setString(1, schema);
        select.setString(2, element);
        final ResultSet rs = select.executeQuery();
        if (rs.next())
        {
            int id = rs.getInt(1);
            select.close();
            return id;
        }
        else
        {
            return -1;
        }
    }
}
