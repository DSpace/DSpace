/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the MetadataValue object.
 * The implementation of this class is responsible for all database calls for the MetadataValue object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MetadataValueDAO extends GenericDAO<MetadataValue> {

    public List<MetadataValue> findByField(Context context, MetadataField fieldId) throws SQLException;

    public Iterator<MetadataValue> findItemValuesByFieldAndValue(Context context,
                                                                 MetadataField metadataField, String value)
            throws SQLException;

    public Iterator<MetadataValue> findByValueLike(Context context, String value) throws SQLException;

    public void deleteByMetadataField(Context context, MetadataField metadataField) throws SQLException;

    public MetadataValue getMinimum(Context context, int metadataFieldId)
        throws SQLException;

    int countRows(Context context) throws SQLException;

    /**
     * Count the metadata values that belong to the given metadata field.
     *
     * @param context       the DSpace context
     * @param metadataField the metadata field
     * @return the number of metadata values for the field
     * @throws SQLException if a database error occurs
     */
    long countByField(Context context, MetadataField metadataField) throws SQLException;

    /**
     * Keyset page over the distinct DSpaceObject ids that have at least one value of the given
     * metadata field, ordered by ascending id and starting strictly after {@code afterUuid}.
     *
     * @param context       the DSpace context
     * @param metadataField the metadata field
     * @param afterUuid     return only object ids greater than this value; {@code null} starts from the beginning
     * @param limit         the maximum number of object ids to return
     * @return an ascending, gap/duplicate-free page of DSpaceObject ids
     * @throws SQLException if a database error occurs
     */
    List<UUID> findObjectIdsByField(Context context, MetadataField metadataField, UUID afterUuid, int limit)
        throws SQLException;

    /**
     * Find all metadata values of the given field that belong to any of the given DSpaceObjects,
     * ordered by object id then place.
     *
     * @param context       the DSpace context
     * @param metadataField the metadata field
     * @param objectIds     the DSpaceObject ids to restrict to
     * @return the matching metadata values
     * @throws SQLException if a database error occurs
     */
    List<MetadataValue> findByFieldAndObjects(Context context, MetadataField metadataField, List<UUID> objectIds)
        throws SQLException;

    /**
     * Bulk-delete the metadata values with the given ids using a single statement that bypasses the
     * Hibernate cascade/orphan-removal machinery (so it is safe to delete a value without loading its
     * owning object).
     *
     * @param context the DSpace context
     * @param ids     the metadata value ids to delete
     * @return the number of rows deleted
     * @throws SQLException if a database error occurs
     */
    int deleteByIds(Context context, List<Integer> ids) throws SQLException;

}
