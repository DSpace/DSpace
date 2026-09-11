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
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutTab;
import org.dspace.service.DSpaceCRUDService;

/**
 * Interface of service to manage Tabs component of layout
 * 
 * The tab {@link DynamicLayoutTab} is a generic container used for configure the visualization
 * of the DSpace Objects data in the client. The configuration provides a hierarchical structure
 * made of tabs, boxes and fields
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public interface DynamicLayoutTabService extends DSpaceCRUDService<DynamicLayoutTab> {

    /**
     * This method stores in the database a DynamicLayoutTab {@link DynamicLayoutTab} instance.
     * @param context The relevant DSpace Context
     * @param tab DynamicLayoutTab instance to store in the database
     * @return stored DynamicLayoutTab instance
     * @throws SQLException An exception that provides information on a database errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    public DynamicLayoutTab create(Context context, DynamicLayoutTab tab) throws SQLException, AuthorizeException;

    /**
     * This method creates and store in the database a new DynamicLayoutTab instance with EntityType
     * and priority (both properties are required)
     * @param context The relevant DSpace Context
     * @param eType EntityType of new Tab {@link EntityType}
     * @param priority Priority of new Tab
     * @return stored DynamicLayoutTab instance {@link DynamicLayoutTab}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public DynamicLayoutTab create(Context context, EntityType eType, Integer priority)
            throws SQLException, AuthorizeException;

    /**
     * Find the tab with the given id and fetch all the rows, cells and boxes
     * contained therein.
     *
     * @param  context The relevant DSpace Context
     * @param  id      the tab id
     * @return         the tab, if any
     * @throws SQLException An exception that provides information on a database errors.
     */
    public DynamicLayoutTab findAndEagerlyFetch(Context context, Integer id) throws SQLException;

    /**
     * Find all DynamicLayoutTab {@link DynamicLayoutTab} in the database
     * @param context The relevant DSpace Context
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of DynamicLayoutTab {@link DynamicLayoutTab}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<DynamicLayoutTab> findAll(Context context, Integer limit, Integer offset) throws SQLException;

    /**
     * Find all DynamicLayoutTab {@link DynamicLayoutTab} in the database
     * @param context The relevant DSpace Context
     * @return List of DynamicLayoutTab {@link DynamicLayoutTab}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<DynamicLayoutTab> findAll(Context context) throws SQLException;

    /**
     * Returns the total number of tabs {@link DynamicLayoutTab} in the database
     * @param context The relevant DSpace Context
     * @return number of tab in the database (Long)
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countTotal(Context context) throws SQLException;

    /**
     * Returns all tabs in the database filtered by entity type {@link EntityType}
     * @param context The relevant DSpace Context
     * @param entityType label of entity type {@link EntityType}
     * @param customFilter label of specialized entity type {@link DynamicLayoutTab#getCustomFilter()}
     * @return List of DynamicLayoutTab {@link DynamicLayoutTab}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<DynamicLayoutTab> findByEntityType(Context context, String entityType, String customFilter)
        throws SQLException;

    /**
     * Returns all tabs in database filtered by entity type {@link EntityType}
     * @param context The relevant DSpace Context
     * @param entityType entity type label
     * @param customFilter label of specialized entity type {@link DynamicLayoutTab#getCustomFilter()}
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of DynamicLayoutTab {@link DynamicLayoutTab}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<DynamicLayoutTab> findByEntityType(Context context, String entityType, String customFilter,
                                                Integer limit, Integer offset) throws SQLException;

    /**
     * Returns the total number of tabs with a specific entity type
     * @param context The relevant DSpace Context
     * @param entityType entity type label
     * @return List of DynamicLayoutTab {@link DynamicLayoutTab}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countByEntityType(Context context, String entityType) throws SQLException;

    /**
     * Returns all metadata field {@link MetadataField} associated at tab
     * @param context The relevant DSpace Context
     * @param tabId Id of Tab {@link DynamicLayoutTab}
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of MetadataField {@link MetadataField}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<MetadataField> getMetadataField(Context context, Integer tabId, Integer limit, Integer offset)
            throws SQLException;

    /**
     * Returns the total number of metadata field associated at tab
     * @param context The relevant DSpace Context
     * @param tabId Id of Tab {@link DynamicLayoutTab}
     * @return the total number of metadata associated at tab
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long totalMetadataField(Context context, Integer tabId) throws SQLException;

    /**
     * Find all tabs associated at an specific item. The tabs are sorted by priority ascending.
     * @param context The relevant DSpace Context
     * @param itemUuid String that represents UUID of the item {@link Item}
     * @return List of DynamicLayoutTab {@link DynamicLayoutTab}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<DynamicLayoutTab> findByItem(Context context, String itemUuid) throws SQLException;

    /**
     * Returns whether access.
     */
    public boolean hasAccess(Context context, DynamicLayoutTab tab, Item item);
}
