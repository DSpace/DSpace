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

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;

/**
 * Database Access Object interface class for the CrisLayoutField object {@link CrisLayoutField}.
 * The implementation of this class is responsible for all database calls for the CrisLayoutField
 * object and is autowired by spring
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public interface CrisLayoutFieldDAO extends GenericDAO<CrisLayoutField> {

    /**
     * Returns the field that are available for specific Box
     * @param context The relevant DSpace Context
     * @param boxId id of box {@link CrisLayoutBox}
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of CrisLayoutField {@link CrisLayoutField}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisLayoutField> findByBoxId
        (Context context, Integer boxId, Integer limit, Integer offset) throws SQLException;

    /**
     * Returns the total number of field that are available for specific Box
     * @param context The relevant DSpace Context
     * @param boxId id of the box {@link CrisLayoutBox}
     * @return the total fields number of box
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countByBoxId(Context context, Integer boxId) throws SQLException;

    /**
     * Returns the field that are available for specific Box
     * @param context The relevant DSpace Context
     * @param boxId id of the box {@link CrisLayoutBox}
     * @return List of CrisLayoutField {@link CrisLayoutField}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisLayoutField> findByBoxId(
            Context context, Integer boxId, Integer row) throws SQLException;
}
