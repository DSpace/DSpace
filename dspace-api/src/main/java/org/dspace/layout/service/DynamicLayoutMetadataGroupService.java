/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service;


import java.sql.SQLException;
import java.util.List;

import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutField;
import org.dspace.layout.DynamicMetadataGroup;
import org.dspace.service.DSpaceCRUDService;


/**
 * Interface of service to manage Fields component of layout group
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.com)
 *
 */
public interface DynamicLayoutMetadataGroupService extends DSpaceCRUDService<DynamicMetadataGroup> {
    /**
     * This method stores in the database a DynamicMetadataGroup {@link DynamicMetadataGroup} instance.
     * @param context The relevant DSpace Context
     * @param nestedField a DynamicMetadataGroup instance {@link DynamicMetadataGroup}
     * @return the stored DynamicMetadataGroup instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    public DynamicMetadataGroup create(Context context, DynamicMetadataGroup nestedField) throws SQLException;
    /**
     * Create and store in the database a new DynamicMetadataGroup {@Link DynamicMetadataGroup} instance
     * with required field
     * @param context The relevant DSpace Context
     * @param mf MetadataField {@link MetadataField}
     * @param priority this attribute is used for define the position of the nested field on its field
     * @return the stored DynamicMetadataGroup instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    public DynamicMetadataGroup create(Context context, MetadataField mf,
                                    DynamicLayoutField cf,  Integer priority) throws SQLException;
    /**
     * Returns the total number of nested field that are available for specific field
     * @param context The relevant DSpace Context
     * @param field_id id of the field {@link DynamicLayoutField}
     * @return the total nested fields number of field
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countNestedFieldInCrisField(Context context, Integer field_id) throws SQLException;
    /**
     * Returns the nested field that are available for specific field
     * @param context The relevant DSpace Context
     * @param field_id id of the field {@link DynamicLayoutField}
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of DynamicMetadataGroup {@link DynamicMetadataGroup}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<DynamicMetadataGroup> findNestedFieldByFieldId(
            Context context, Integer field_id, Integer limit, Integer offset) throws SQLException;

    public List<DynamicMetadataGroup> findNestedFieldByFieldId(
            Context context, Integer field_id) throws SQLException;
}
