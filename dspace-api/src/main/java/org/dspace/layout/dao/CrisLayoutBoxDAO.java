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

import org.dspace.content.EntityType;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.layout.CrisLayoutBox;

/**
 * Database Access Object interface class for the CrisLayoutBox object {@link CrisLayoutBox}.
 * The implementation of this class is responsible for all database calls for the CrisLayoutBox
 * object and is autowired by spring
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public interface CrisLayoutBoxDAO extends GenericDAO<CrisLayoutBox> {

    /**
     * Returns the boxes that are available for the specified entity type
     * @param context The relevant DSpace Context
     * @param entityType entity type label {@link EntityType}
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of CrisLayoutBox {@link CrisLayoutBox}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisLayoutBox> findByEntityType(Context context, String entityType,
        Integer limit, Integer offset) throws SQLException;

    /**
     * Returns the boxes that are available for the specified entity type and with the given type
     * @param context The relevant DSpace Context
     * @param entity entity type
     * @param type type of the box
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisLayoutBox> findByEntityAndType(Context context, String entity, String type) throws SQLException;
}
