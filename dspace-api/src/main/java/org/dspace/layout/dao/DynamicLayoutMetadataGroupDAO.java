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
import org.dspace.layout.DynamicLayoutField;
import org.dspace.layout.DynamicMetadataGroup;


/**
 * Database Access Object interface class for the DynamicMetadataGroup object {@link org.dspace.layout.DynamicMetadataGroup}.
 * The implementation of this class is responsible for all database calls for the DynamicLayoutField
 * object and is autowired by spring
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.com)
 *
 */
public interface DynamicLayoutMetadataGroupDAO extends GenericDAO<DynamicMetadataGroup> {
    /**
     * Returns the nsted field that are available for specific field
     * @param context The relevant DSpace Context
     * @param fieldId id of field {@link DynamicLayoutField}
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of DynamicMetadataGroup {@link DynamicMetadataGroup}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<DynamicMetadataGroup> findByFieldId(Context context, Integer fieldId,
                                                 @Nullable Integer limit, @Nullable Integer offset) throws SQLException;
    /**
     * Returns the total number of nested  fields that are available for specific field
     * @param context The relevant DSpace Context
     * @param field_id id of the box {@link DynamicLayoutField}
     * @return the total nested fields number of field
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countByFieldId(Context context, Integer field_id) throws SQLException;
}
