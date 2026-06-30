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
import org.dspace.service.DSpaceCRUDService;
/**
 * Interface of service to manage Fields component of layout
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public interface CrisLayoutFieldService extends DSpaceCRUDService<CrisLayoutField> {

    /**
     * This method stores in the database a CrisLayoutTab {@link CrisLayoutField} instance.
     * @param context The relevant DSpace Context
     * @param field a CrisLayoutField instance {@link CrisLayoutField}
     * @return the stored CrisLayoutField instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    public CrisLayoutField create(Context context, CrisLayoutField field) throws SQLException;

    /**
     * Create and store in the database a new CrisLayoutField {@Link CrisLayoutField} instance
     * with required field
     * @param context The relevant DSpace Context
     * @param mf MetadataField {@link MetadataField}
     * @param row this attribute is used for define the row of the field in its box
     * @param priority this attribute is used for define the position of the field in its box
     * @return the stored CrisLayoutField instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    public CrisLayoutField create(Context context, MetadataField mf, Integer row, Integer priority) throws SQLException;

    /**
     * Returns the total number of field that are available for specific Box
     * @param context The relevant DSpace Context
     * @param boxId id of the box {@link CrisLayoutBox}
     * @return the total fields number of box
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countFieldInBox(Context context, Integer boxId) throws SQLException;

    /**
     * Returns the field that are available for specific Box
     * @param context The relevant DSpace Context
     * @param boxId id of the box {@link CrisLayoutBox}
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of CrisLayoutField {@link CrisLayoutField}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisLayoutField> findFieldByBoxId(
            Context context, Integer boxId, Integer limit, Integer offset) throws SQLException;

    /**
     * Returns all field of a specific row in a Box
     * @param context The relevant DSpace Context
     * @param boxId id of the box {@link CrisLayoutBox}
     * @param row
     * @return
     * @throws SQLException
     */
    public List<CrisLayoutField> findFieldByBoxId(
            Context context, Integer boxId, Integer row) throws SQLException;
}
