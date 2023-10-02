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

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.GenericDAO;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the MetadataValue object.
 * The implementation of this class is responsible for all database calls for the MetadataValue object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MetadataValueDAO extends GenericDAO<MetadataValue> {

    public List<MetadataValue> findByField(Session session, MetadataField fieldId) throws SQLException;

    public Iterator<MetadataValue> findItemValuesByFieldAndValue(Session session,
                                                                 MetadataField metadataField, String value)
            throws SQLException;

    public Iterator<MetadataValue> findByValueLike(Session session, String value) throws SQLException;

    public void deleteByMetadataField(Session session, MetadataField metadataField) throws SQLException;

    public MetadataValue getMinimum(Session session, int metadataFieldId)
        throws SQLException;

    int countRows(Session session) throws SQLException;

}
