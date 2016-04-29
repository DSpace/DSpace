/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

import java.sql.SQLException;
import java.util.List;

/**
 * Database Access Object interface class for the MetadataField object.
 * The implementation of this class is responsible for all database calls for the MetadataField object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MetadataFieldDAO extends GenericDAO<MetadataField> {

    public MetadataField find(Context context, int metadataFieldId, MetadataSchema metadataSchema, String element, String qualifier)
            throws SQLException;

    public MetadataField findByElement(Context context, MetadataSchema metadataSchema, String element, String qualifier)
            throws SQLException;

    public MetadataField findByElement(Context context, String metadataSchema, String element, String qualifier)
            throws SQLException;

    public List<MetadataField> findFieldsByElementNameUnqualified(Context context, String metadataSchema, String element) 
    		throws SQLException;
    
    public List<MetadataField> findAllInSchema(Context context, MetadataSchema metadataSchema)
            throws SQLException;

}
