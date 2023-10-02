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

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.hibernate.Session;

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
     * @param session current request's database context.
     * @param valueId database key id of value
     * @return recalled metadata value
     * @throws IOException  if IO error
     * @throws SQLException if database error
     */
    public MetadataValue find(Session session, int valueId)
        throws IOException, SQLException;


    /**
     * Retrieves the metadata values for a given field from the database.
     *
     * @param session       current request's database context.
     * @param metadataField metadata field whose values to look for
     * @return a collection of metadata values
     * @throws IOException  if IO error
     * @throws SQLException if database error
     */
    public List<MetadataValue> findByField(Session session, MetadataField metadataField)
        throws IOException, SQLException;

    /**
     * Retrieves matching MetadataValues for a given field and value.
     *
     * @param session current request's database context.
     * @param metadataField The field that must match
     * @param value The value that must match
     * @return the matching MetadataValues
     * @throws SQLException if database error
     */
    public Iterator<MetadataValue> findByFieldAndValue(Session session, MetadataField metadataField, String value)
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

    public Iterator<MetadataValue> findByValueLike(Session session, String value) throws SQLException;

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
}
