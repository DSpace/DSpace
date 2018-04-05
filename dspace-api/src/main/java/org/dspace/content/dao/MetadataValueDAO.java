/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Database Access Object interface class for the MetadataValue object.
 * The implementation of this class is responsible for all database calls for the MetadataValue object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MetadataValueDAO extends GenericDAO<MetadataValue> {

    public List<MetadataValue> findByField(Context context, MetadataField fieldId) throws SQLException;

    public Iterator<MetadataValue> findByValueLike(Context context, String value) throws SQLException;

    public void deleteByMetadataField(Context context, MetadataField metadataField) throws SQLException;

    public MetadataValue getMinimum(Context context, int metadataFieldId)
            throws SQLException;

    int countRows(Context context) throws SQLException;

}
