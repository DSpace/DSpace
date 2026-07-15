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
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.layout.DynamicLayoutTab;


/**
 * Database Access Object interface class for the DynamicLayoutTab object {@link DynamicLayoutTab}.
 * The implementation of this class is responsible for all database calls for the DynamicLayoutTab
 * object and is autowired by spring
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public interface DynamicLayoutTabDAO extends GenericDAO<DynamicLayoutTab> {

    /**
     * Find the tab with the given id and fetch all the rows, cells and boxes
     * contained therein.
     *
     * @param  context The relevant DSpace Context
     * @param  id      the tab id
     * @return         the tab, if any
     * @throws SQLException An exception that provides information on a database errors.
     */
    public DynamicLayoutTab findAndEagerlyFetchBoxes(Context context, Integer id) throws SQLException;

    /**
     * Returns the total number of tabs {@link DynamicLayoutTab} in the database
     * @param context The relevant DSpace Context
     * @return total number of tabs in the database (Long)
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countTotal(Context context) throws SQLException;

    /**
     * Returns the total number of tabs with a specific entity type
     * @param context The relevant DSpace Context
     * @param entityType entity type label {@link EntityType}
     * @return total number of tabs
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countByEntityType(Context context, String entityType) throws SQLException;

    /**
     * Returns all tabs in database filtered by entity type {@link EntityType}
     * @param context The relevant DSpace Context
     * @param entityType entity type label {@link EntityType}
     * @return List of DynamicLayoutTab {@link DynamicLayoutTab}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<DynamicLayoutTab> findByEntityTypeAndEagerlyFetchBoxes(Context context,
        String entityType) throws SQLException;

    /**
     * Returns all tabs in database filtered by entity type {@link EntityType}
     * @param context The relevant DSpace Context
     * @param entityType entity type label {@link EntityType}
     * @param customFilter specialized entity type label {@link DynamicLayoutTab#getCustomFilter()}
     * @return List of DynamicLayoutTab {@link DynamicLayoutTab}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<DynamicLayoutTab> findByEntityTypeAndEagerlyFetchBoxes(Context context,
        String entityType, String customFilter) throws SQLException;

    /**
     * Returns all tabs in database filtered by entity type {@link EntityType}
     * @param context The relevant DSpace Context
     * @param entityType entity type label {@link EntityType}
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of DynamicLayoutTab {@link DynamicLayoutTab}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<DynamicLayoutTab> findByEntityTypeAndEagerlyFetchBoxes(
        Context context, String entityType, String customFilter, Integer limit, Integer offset) throws SQLException;

    /**
     * Returns the total number of metadata field associated at tab
     * @param context The relevant DSpace Context
     * @param tabId Id of Tab {@link DynamicLayoutTab}
     * @return the total number of metadata associated at tab
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long totalMetadatafield(Context context, Integer tabId) throws SQLException;

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

}
