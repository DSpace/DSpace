/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.dao;

import java.sql.SQLException;
import java.util.List;
import javax.annotation.Nullable;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisMetadataGroup;


/**
 * Database Access Object interface class for the CrisMetadataGroup object {@link org.dspace.layout.CrisMetadataGroup}.
 * The implementation of this class is responsible for all database calls for the CrisLayoutField
 * object and is autowired by spring
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.com)
 *
 */
public interface CrisLayoutMetadataGroupDAO extends GenericDAO<CrisMetadataGroup> {
    /**
     * Returns the nsted field that are available for specific field
     * @param context The relevant DSpace Context
     * @param fieldId id of field {@link CrisLayoutField}
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of CrisMetadataGroup {@link CrisMetadataGroup}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisMetadataGroup> findByFieldId(Context context, Integer fieldId,
                                                 @Nullable Integer limit, @Nullable Integer offset) throws SQLException;
    /**
     * Returns the total number of nested  fields that are available for specific field
     * @param context The relevant DSpace Context
     * @param field_id id of the box {@link CrisLayoutField}
     * @return the total nested fields number of field
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countByFieldId(Context context, Integer field_id) throws SQLException;
}
