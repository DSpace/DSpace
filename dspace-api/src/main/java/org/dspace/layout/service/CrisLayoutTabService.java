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

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.service.DSpaceCRUDService;

/**
 * 
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
public interface CrisLayoutTabService extends DSpaceCRUDService<CrisLayoutTab> {

    /**
     * Create a new tab object
     * 
     * @param context the dspace application context
     * @param tab
     * @return a CrisLayoutTab instance of the object created
     * @throws SQLException
     */
    public CrisLayoutTab create(Context context, CrisLayoutTab tab) throws SQLException, AuthorizeException;

    /**
     * Create a new tab with EntityType and priority (both properties are required)
     * @param context the dspace application context
     * @param eType EntityType of new Tab
     * @param priority Priority of new Tab
     * @return a CrisLayoutTab instance
     * @throws SQLException
     */
    public CrisLayoutTab create(Context context, EntityType eType, Integer priority)
            throws SQLException, AuthorizeException;

    /**
     * Find all tab in the repository
     * @param context DSpace application context
     * @param limit max number of result
     * @param offset first element
     * @return
     * @throws SQLException
     */
    public List<CrisLayoutTab> findAll(Context context, Integer limit, Integer offset) throws SQLException;

    /**
     * Returns the total number of tabs in the repository
     * @param context DSpace application context
     * @return
     * @throws SQLException
     */
    public Long countTotal(Context context) throws SQLException;

    /**
     * Returns all tab in repository filtered by entity type label
     * @param context DSpace application context
     * @param entityType label of entity type
     * @return
     * @throws SQLException
     */
    public List<CrisLayoutTab> findByEntityType(Context context, String entityType) throws SQLException;

    /**
     * Returns all tab in repository filtered by entity type label with pagination
     * @param context DSpace application context
     * @param entityType entity type label
     * @param limit max number of result (null for no limit result)
     * @param offset first element
     * @return
     * @throws SQLException
     */
    public List<CrisLayoutTab> findByEntityType(Context context, String entityType, Integer limit, Integer offset)
            throws SQLException;

    /**
     * Returns the number of tabs for a specific entity type
     * @param context DSpace application context
     * @param entityType entity type label
     * @return
     * @throws SQLException
     */
    public Long countByEntityType(Context context, String entityType) throws SQLException;

    /**
     * Returns all metadata field associated at tab
     * @param context
     * @param tabId
     * @param limit
     * @param offset
     * @return
     * @throws SQLException
     */
    public List<MetadataField> getMetadataField(Context context, Integer tabId, Integer limit, Integer offset)
            throws SQLException;

    /**
     * Returns the total number of metadata field
     * @param context
     * @param tabId
     * @return
     * @throws SQLException
     */
    public Long totalMetadataField(Context context, Integer tabId) throws SQLException;

}
