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
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.service.DSpaceCRUDService;


/**
 * Interface of service to manage Fields component of layout group
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.com)
 *
 */
public interface CrisLayoutMetadataGroupService extends DSpaceCRUDService<CrisMetadataGroup> {
    /**
     * This method stores in the database a CrisMetadataGroup {@link CrisMetadataGroup} instance.
     * @param context The relevant DSpace Context
     * @param nestedField a CrisMetadataGroup instance {@link CrisMetadataGroup}
     * @return the stored CrisMetadataGroup instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    public CrisMetadataGroup create(Context context, CrisMetadataGroup nestedField) throws SQLException;
    /**
     * Create and store in the database a new CrisMetadataGroup {@Link CrisMetadataGroup} instance
     * with required field
     * @param context The relevant DSpace Context
     * @param mf MetadataField {@link MetadataField}
     * @param priority this attribute is used for define the position of the nested field on its field
     * @return the stored CrisMetadataGroup instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    public CrisMetadataGroup create(Context context, MetadataField mf,
                                    CrisLayoutField cf,  Integer priority) throws SQLException;
    /**
     * Returns the total number of nested field that are available for specific field
     * @param context The relevant DSpace Context
     * @param field_id id of the field {@link CrisLayoutField}
     * @return the total nested fields number of field
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countNestedFieldInCrisField(Context context, Integer field_id) throws SQLException;
    /**
     * Returns the nested field that are available for specific field
     * @param context The relevant DSpace Context
     * @param field_id id of the field {@link CrisLayoutField}
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of CrisMetadataGroup {@link CrisMetadataGroup}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisMetadataGroup> findNestedFieldByFieldId(
            Context context, Integer field_id, Integer limit, Integer offset) throws SQLException;

    public List<CrisMetadataGroup> findNestedFieldByFieldId(
            Context context, Integer field_id) throws SQLException;
}
