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

import org.dspace.content.EntityType;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object Interface class for the EntityType object
 * The implementation of this class is responsible for all database calls for the EntityType object and is autowired by
 * spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 */
public interface EntityTypeDAO extends GenericDAO<EntityType> {

    /**
     * This method returns the EntityType object that has the given entityType String
     * as label
     * @param context       The relevant DSpace context
     * @param entityType    The entityType String that will be matched on to find
     *                      the correct EntityType
     * @return              The EntityType object that has the entityType String as label
     * @throws SQLException If something goes wrong
     */
    public EntityType findByEntityType(Context context, String entityType) throws SQLException;

    /**
     * 
     * @param context        DSpace context object
     * @param names          List of Entity type names that you want to retrieve
     * @param limit          paging limit
     * @param offset         the position of the first result to return
     * @return
     * @throws SQLException  if database error
     */
    public List<EntityType> getEntityTypesByNames(Context context, List<String> names, Integer limit, Integer offset)
           throws SQLException;

    /**
     * 
     * @param context          DSpace context object
     * @param names            List of Entity type names that you want to retrieve
     * @return
     * @throws SQLException    If database error
     */
    public int countEntityTypesByNames(Context context, List<String> names) throws SQLException;

}
