/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.core.Context;
import org.dspace.service.DSpaceCRUDService;

/**
 * This Service is used to access the data for EntityTypes through the DAO objects
 */
public interface EntityTypeService extends DSpaceCRUDService<EntityType> {

    /**
     * Retrieves the EntityType that has the entityType String parameter as label
     * @param context       The relevant DSpace context
     * @param entityType    The String label that has to match
     * @return              The EntityType that has a String
     * @throws SQLException If something goes wrong
     */
    public EntityType findByEntityType(Context context,String entityType) throws SQLException;

    /**
     * Retrieves all the EntityType objects currently in the system
     * @param context   The relevant DSpace context
     * @return          A list of all EntityType objects
     * @throws SQLException If something goes wrong
     */
    public List<EntityType> findAll(Context context) throws SQLException;

    /**
     * Retrieves all the EntityType objects currently in the system
     * @param context   The relevant DSpace context
     * @param limit     paging limit
     * @param offset    paging offset
     * @return          A list of all EntityType objects
     * @throws SQLException If something goes wrong
     */
    List<EntityType> findAll(Context context, Integer limit, Integer offset) throws SQLException;

    /**
     * This method creates an EntityType object in the database with the given entityTypeString as it's label
     * @param context           The relevant DSpace context
     * @param entityTypeString  The label for the newly created EntityType
     * @return                  The newly created EntityType
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something geos wrong with authorizations
     */
    public EntityType create(Context context, String entityTypeString) throws SQLException, AuthorizeException;

    /**
     * Retrieves all entity types related to the collections on which the current user can deposit
     * 
     * @param context                     DSpace context object
     * @return
     * @throws SQLException               If database error
     * @throws SolrServerException        If there is a problem in communicating with Solr
     * @throws IOException                If IO error
     */
    public List<String> getSubmitAuthorizedTypes(Context context) throws SQLException, SolrServerException, IOException;

    /**
     * 
     * @param context          DSpace context object
     * @param names            List of Entity type names that you want to retrieve
     * @param limit            paging limit
     * @param offset           the position of the first result to return
     * @return
     * @throws SQLException    if database error
     */
    public List<EntityType> getEntityTypesByNames(Context context, List<String> names,Integer limit, Integer offset)
           throws SQLException;

    /**
     * 
     * @param context          DSpace context object
     * @param names            List of Entity type names that you want to retrieve
     * @return
     * @throws SQLException    if database error
     */
    public int countEntityTypesByNames(Context context, List<String> names) throws SQLException;

    /**
     * Initializes the EntityType names, and marks them "permanent".
     * 
     * @param context                 DSpace context object
     * @throws SQLException           Database exception
     * @throws AuthorizeException     Authorization error
     */
    public void initDefaultEntityTypeNames(Context context) throws SQLException, AuthorizeException;

}
