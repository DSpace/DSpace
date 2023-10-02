/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.GenericDAO;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the MetadataField object.
 * The implementation of this class is responsible for all database calls for the MetadataField object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MetadataFieldDAO extends GenericDAO<MetadataField> {

    public MetadataField find(Session session, int metadataFieldId, MetadataSchema metadataSchema, String element,
                              String qualifier)
        throws SQLException;

    public MetadataField findByElement(Session session, MetadataSchema metadataSchema, String element, String qualifier)
        throws SQLException;

    public MetadataField findByElement(Session session, String metadataSchema, String element, String qualifier)
        throws SQLException;

    public List<MetadataField> findFieldsByElementNameUnqualified(Session session, String metadataSchema,
                                                                  String element)
        throws SQLException;

    public List<MetadataField> findAllInSchema(Session session, MetadataSchema metadataSchema)
        throws SQLException;

}
