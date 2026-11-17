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

import org.dspace.content.dao.ItemForMetadataEnhancementUpdateDAO;
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

    /**
     * Resolves the relationships that touch the changed item through the two single column indexes,
     * then joins the metadata values on the resulting relationship ids.
     */
    private static final String RELATIONSHIP_ENDPOINT_JOIN =
            "JOIN ( " +
                "    SELECT id FROM relationship WHERE left_id = :uuid " +
                "    UNION " +
                "    SELECT id FROM relationship WHERE right_id = :uuid " +
                ") r ON mv.relationship_id = r.id ";

    /**
     * Excludes the changed item itself from the queue. An item that is both endpoints of the same
     * relationship is not queued from its own recalculation.
     */
    private static final String SELF_EXCLUSION = "mv.dspace_object_id <> :uuid ";

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
     * <p>Searches for all items owning relationship-backed metadata whose relationship has the
     * updated entity as one of its endpoints. Derived metadata is keyed by
     * {@code relationship_id}, so the fan-out follows the indexed relationship join.</p>
     */
    @Override
    public int saveAffectedItemsForUpdate(Context context, UUID uuid) {
        try {
            Session session = getHibernateSession();
            if ("org.h2.Driver".equals(configurationService.getProperty("db.driver"))) {
                // H2 doesn't support the INSERT OR UPDATE statement so let's do in two steps
                // update queued date for records already in the queue
                String sqlUpdate = "UPDATE itemupdate_metadata_enhancement iue " +
                    "SET date_queued = CURRENT_TIMESTAMP " +
                    "WHERE EXISTS ( " +
                    "    SELECT 1 " +
                    "    FROM metadatavalue mv " +
                    "    " + RELATIONSHIP_ENDPOINT_JOIN +
                    "    WHERE mv.dspace_object_id = iue.uuid " +
                    "    AND " + SELF_EXCLUSION +
                    ")";
                String sqlInsert =
                        "INSERT INTO itemupdate_metadata_enhancement (uuid, date_queued) " +
                            "SELECT DISTINCT mv.dspace_object_id, CURRENT_TIMESTAMP " +
                            "FROM metadatavalue mv " +
                            RELATIONSHIP_ENDPOINT_JOIN +
                            "LEFT JOIN itemupdate_metadata_enhancement iue ON mv.dspace_object_id = iue.uuid " +
                            "WHERE " + SELF_EXCLUSION +
                            "AND iue.uuid IS NULL";
                NativeQuery<?> queryUpdate = session.createNativeQuery(sqlUpdate);
                queryUpdate.setParameter("uuid", uuid);
                queryUpdate.executeUpdate();
                NativeQuery<?> queryInsert = session.createNativeQuery(sqlInsert);
                queryInsert.setParameter("uuid", uuid);
                return queryInsert.executeUpdate();
            } else {
                String sqlInsertOrUpdate = "INSERT INTO itemupdate_metadata_enhancement (uuid, date_queued) " +
                    "SELECT DISTINCT mv.dspace_object_id, CURRENT_TIMESTAMP " +
                    "FROM metadatavalue mv " +
                    RELATIONSHIP_ENDPOINT_JOIN +
                    "WHERE " + SELF_EXCLUSION +
                    "ON CONFLICT (uuid) DO UPDATE " +
                    "SET date_queued = EXCLUDED.date_queued";
                NativeQuery<?> queryInsertOrUpdate = session.createNativeQuery(sqlInsertOrUpdate);
                queryInsertOrUpdate.setParameter("uuid", uuid);
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