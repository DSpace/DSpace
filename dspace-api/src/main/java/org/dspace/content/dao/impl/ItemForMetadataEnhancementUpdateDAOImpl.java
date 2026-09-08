/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.content.MetadataSchema;
import org.dspace.content.dao.ItemForMetadataEnhancementUpdateDAO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.core.DBConnection;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemForMetadataEnhancementUpdateDAO} using native SQL queries.
 * 
 * @see ItemForMetadataEnhancementUpdateDAO
 * @see org.dspace.content.enhancer.consumer.ItemEnhancerConsumer
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class ItemForMetadataEnhancementUpdateDAOImpl implements ItemForMetadataEnhancementUpdateDAO {
    @Autowired
    ConfigurationService configurationService;

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation:</strong> Uses a direct SQL DELETE statement for immediate
     * removal without the overhead of entity loading and Hibernate state management.</p>
     */
    @Override
    public void removeItemForUpdate(Context context, UUID itemToRemove) {
        try {
            Session session = getHibernateSession();
            String sql = "DELETE FROM itemupdate_metadata_enhancement WHERE uuid = :uuid";
            NativeQuery<?> query = session.createNativeQuery(sql);
            query.setParameter("uuid", itemToRemove);
            query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * <strong>Implementation:</strong> Performs an atomic SELECT and DELETE operation.
     * Uses ORDER BY date_queued ASC to process items in first-in-first-out (FIFO) order
     * for fair queue processing.
     * </p>
     */
    @Override
    public UUID pollItemToUpdate(Context context) {
        try {
            Session session = getHibernateSession();
            String sql = "SELECT cast(uuid as varchar) FROM itemupdate_metadata_enhancement"
                    + " ORDER BY date_queued ASC LIMIT 1";
            NativeQuery<?> query = session.createNativeQuery(sql);
            Object uuidObj = query.uniqueResult();
            if (uuidObj != null) {
                UUID uuid;
                if (uuidObj instanceof String) {
                    uuid = (UUID) UUID.fromString((String) uuidObj);
                } else {
                    throw new RuntimeException("Unexpected result type from the database " + uuidObj);
                }
                removeItemForUpdate(context, uuid);
                return uuid;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Query Logic:</strong> Searches for all items containing {@code dspace.virtualsource.*}
     * metadata fields where the first 36 characters (UUID portion) match the updated entity's UUID.
     * This covers both direct references and qualified references like "uuid::qualifier".</p>
     */
    @Override
    public int saveAffectedItemsForUpdate(Context context, UUID uuid) {
        try {
            Session session = getHibernateSession();
            MetadataSchemaService schemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
            MetadataSchema schema = schemaService.find(context, "dspace");
            String sqlInsertOrUpdate;
            if ("org.h2.Driver".equals(configurationService.getProperty("db.driver"))) {
                // H2 doesn't support the INSERT OR UPDATE statement so let's do in two steps
                // update queued date for records already in the queue
                String sqlUpdate = "UPDATE itemupdate_metadata_enhancement iue " +
                    "SET date_queued = CURRENT_TIMESTAMP " +
                    "WHERE EXISTS ( " +
                    "    SELECT 1 " +
                    "    FROM metadatavalue mv " +
                    "    JOIN metadatafieldregistry mfr ON mv.metadata_field_id = mfr.metadata_field_id " +
                    "    WHERE mv.dspace_object_id = iue.uuid " +
                    "    AND mfr.metadata_schema_id = :schema " +
                    "    AND mfr.element = 'virtualsource' " +
                    "    AND SUBSTRING(mv.text_value,1,36) = :uuid " +
                    ")";
                String sqlInsert =
                        "INSERT INTO itemupdate_metadata_enhancement (uuid, date_queued) " +
                            "SELECT DISTINCT mv.dspace_object_id, CURRENT_TIMESTAMP " +
                            "FROM metadatavalue mv " +
                            "JOIN metadatafieldregistry mfr ON mv.metadata_field_id = mfr.metadata_field_id " +
                            "LEFT JOIN itemupdate_metadata_enhancement iue ON mv.dspace_object_id = iue.uuid " +
                            "WHERE mfr.metadata_schema_id = :schema " +
                            "AND mfr.element = 'virtualsource' " +
                            "AND SUBSTRING(mv.text_value,1,36) = :uuid " +
                            "AND iue.uuid IS NULL";
                NativeQuery<?> queryUpdate = session.createNativeQuery(sqlUpdate);
                queryUpdate.setParameter("uuid", uuid.toString());
                queryUpdate.setParameter("schema", schema.getID());
                queryUpdate.executeUpdate();
                NativeQuery<?> queryInsert = session.createNativeQuery(sqlInsert);
                queryInsert.setParameter("uuid", uuid.toString());
                queryInsert.setParameter("schema", schema.getID());
                return queryInsert.executeUpdate();
            } else {
                sqlInsertOrUpdate = "INSERT INTO itemupdate_metadata_enhancement (uuid, date_queued)" +
                    "SELECT DISTINCT mv.dspace_object_id, CURRENT_TIMESTAMP " +
                    "FROM metadatavalue mv " +
                    "JOIN metadatafieldregistry mfr ON mv.metadata_field_id = mfr.metadata_field_id " +
                    "WHERE mfr.metadata_schema_id = :schema " +
                    "AND mfr.element = 'virtualsource' " +
                    "AND SUBSTRING(mv.text_value,1,36) = :uuid " +
                    "ON CONFLICT (uuid) DO UPDATE " +
                    "SET date_queued = EXCLUDED.date_queued";
                NativeQuery<?> queryInsertOrUpdate = session.createNativeQuery(sqlInsertOrUpdate);
                queryInsertOrUpdate.setParameter("uuid", uuid.toString());
                queryInsertOrUpdate.setParameter("schema", schema.getID());
                return queryInsertOrUpdate.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the current Hibernate session for executing native SQL queries.
     * 
     * <p>This method accesses the DSpace service manager to obtain the current
     * database session, bypassing the normal Hibernate entity management for
     * direct SQL execution.</p>
     *
     * @return the current Hibernate Session for the thread
     * @throws SQLException if the database session cannot be obtained
     */
    private Session getHibernateSession() throws SQLException {
        DBConnection dbConnection = new DSpace().getServiceManager().getServiceByName(null, DBConnection.class);
        return ((Session) dbConnection.getSession());
    }
}