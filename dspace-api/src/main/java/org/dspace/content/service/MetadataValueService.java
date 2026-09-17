/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

/**
 * Service interface class for the MetadataValue object.
 * The implementation of this class is responsible for all business logic calls for the MetadataValue object and is
 * autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MetadataValueService {

    /**
     * Creates a new metadata value.
     *
     * @param context       DSpace context object
     * @param dso           DSpaceObject
     * @param metadataField metadata field
     * @return new MetadataValue
     * @throws SQLException if database error
     */
    public MetadataValue create(Context context, DSpaceObject dso, MetadataField metadataField) throws SQLException;

    /**
     * Retrieves the metadata value from the database.
     *
     * @param context dspace context
     * @param valueId database key id of value
     * @return recalled metadata value
     * @throws IOException  if IO error
     * @throws SQLException if database error
     */
    public MetadataValue find(Context context, int valueId)
        throws IOException, SQLException;


    /**
     * Retrieves the metadata values for a given field from the database.
     *
     * @param context       dspace context
     * @param metadataField metadata field whose values to look for
     * @return a collection of metadata values
     * @throws IOException  if IO error
     * @throws SQLException if database error
     */
    public List<MetadataValue> findByField(Context context, MetadataField metadataField)
        throws IOException, SQLException;

    /**
     * Retrieves matching MetadataValues for a given field and value.
     *
     * @param context dspace context
     * @param metadataField The field that must match
     * @param value The value that must match
     * @return the matching MetadataValues
     * @throws SQLException if database error
     */
    public Iterator<MetadataValue> findByFieldAndValue(Context context, MetadataField metadataField, String value)
            throws SQLException;

    /**
     * Update the metadata value in the database.
     *
     * @param context       dspace context
     * @param metadataValue metadata value
     * @throws SQLException if database error
     */
    public void update(Context context, MetadataValue metadataValue) throws SQLException;

    public void update(Context context, MetadataValue metadataValue, boolean modifyParentObject)
        throws SQLException, AuthorizeException;

    /**
     * Delete the metadata field.
     *
     * @param context       dspace context
     * @param metadataValue metadata value
     * @throws SQLException if database error
     */
    public void delete(Context context, MetadataValue metadataValue) throws SQLException;

    public Iterator<MetadataValue> findByValueLike(Context context, String value) throws SQLException;

    public void deleteByMetadataField(Context context, MetadataField metadataField) throws SQLException;

    /**
     * Get the minimum value of a given metadata field across all objects.
     *
     * @param context         dspace context
     * @param metadataFieldId unique identifier of the interesting field.
     * @return the minimum value of the metadata field
     * @throws SQLException if database error
     */
    public MetadataValue getMinimum(Context context, int metadataFieldId)
        throws SQLException;

    int countTotal(Context context) throws SQLException;

    /**
     * Count the metadata values that belong to the given metadata field.
     *
     * @param context       dspace context
     * @param metadataField the metadata field
     * @return the number of metadata values for the field
     * @throws SQLException if database error
     */
    long countByField(Context context, MetadataField metadataField) throws SQLException;

    /**
     * Keyset page over the distinct DSpaceObject ids that have at least one value of the given
     * metadata field, ordered by ascending id and starting strictly after {@code afterUuid}.
     *
     * @param context       dspace context
     * @param metadataField the metadata field
     * @param afterUuid     return only object ids greater than this value; {@code null} starts from the beginning
     * @param limit         the maximum number of object ids to return
     * @return an ascending, gap/duplicate-free page of DSpaceObject ids
     * @throws SQLException if database error
     */
    List<UUID> findObjectIdsByField(Context context, MetadataField metadataField, UUID afterUuid, int limit)
        throws SQLException;

    /**
     * Find all metadata values of the given field that belong to any of the given DSpaceObjects,
     * ordered by object id then place.
     *
     * @param context       dspace context
     * @param metadataField the metadata field
     * @param objectIds     the DSpaceObject ids to restrict to
     * @return the matching metadata values
     * @throws SQLException if database error
     */
    List<MetadataValue> findByFieldAndObjects(Context context, MetadataField metadataField, List<UUID> objectIds)
        throws SQLException;

    /**
     * Bulk-delete the metadata values with the given ids in a single statement that bypasses the
     * Hibernate cascade/orphan-removal machinery.
     *
     * @param context dspace context
     * @param ids     the metadata value ids to delete
     * @return the number of rows deleted
     * @throws SQLException if database error
     */
    int deleteByIds(Context context, List<Integer> ids) throws SQLException;
}
