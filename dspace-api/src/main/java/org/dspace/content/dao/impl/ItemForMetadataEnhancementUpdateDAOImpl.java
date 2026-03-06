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
 * Hibernate implementation of the Database Access Object interface class for
 * the ItemForMetadataEnhancementUpdate object. This class is responsible for
 * all database calls for the ItemForMetadataEnhancementUpdate object and is
 * autowired by spring This class should never be accessed directly.
 */
public class ItemForMetadataEnhancementUpdateDAOImpl implements ItemForMetadataEnhancementUpdateDAO {
    @Autowired
    ConfigurationService configurationService;

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

    @Override
    public int saveAffectedItemsForUpdate(Context context, UUID uuid) {
        try {
            Session session = getHibernateSession();
            MetadataSchemaService schemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
            MetadataSchema schema = schemaService.find(context, "cris");
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
     * The Hibernate Session used in the current thread
     *
     * @return the current Session.
     * @throws SQLException
     */
    private Session getHibernateSession() throws SQLException {
        DBConnection dbConnection = new DSpace().getServiceManager().getServiceByName(null, DBConnection.class);
        return ((Session) dbConnection.getSession());
    }

    public UUID ConvertByteArrayToUUID(byte[] bytea) {
        long mostSigBits = 0;
        long leastSigBits = 0;
        for (int i = 0; i < 8; i++) {
            mostSigBits = (mostSigBits << 8) | (bytea[i] & 0xff);
            leastSigBits = (leastSigBits << 8) | (bytea[i + 8] & 0xff);
        }

        UUID uuid = new UUID(mostSigBits, leastSigBits);
        return uuid;
    }
}