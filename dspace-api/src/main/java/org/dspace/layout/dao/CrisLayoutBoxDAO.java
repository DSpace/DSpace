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
     * Find all boxes of a specific tab
     * @param context The relevant DSpace Context
     * @param tabId Id of tab container
     * @return List of CrisLayoutBox {@link CrisLayoutBox}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisLayoutBox> findByTabId(Context context, Integer tabId) throws SQLException;

    /**
     * Find all boxes of a specific tab
     * @param context The relevant DSpace Context
     * @param tabId Id of tab container
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of CrisLayoutBox {@link CrisLayoutBox}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisLayoutBox> findByTabId(
            Context context, Integer tabId, Integer limit, Integer offset) throws SQLException;

    /**
     * Returns the total number of boxes contained in the tab identified by its id
     * @param context The relevant DSpace Context
     * @param tabId Id of tab container
     * @return total boxes number in tab
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countTotalBoxesInTab(Context context, Integer tabId) throws SQLException;

    /**
     * Returns total number of the boxes that are available for the specified entity type {@link EntityType}
     * @param context The relevant DSpace Context
     * @param entityType entity type label {@link EntityType}
     * @return total boxes number of the entity type
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long countTotalEntityBoxes(Context context, String entityType) throws SQLException;

    /**
     * Returns the boxes that are available for the specified entity type
     * @param context The relevant DSpace Context
     * @param entityType entity type label {@link EntityType}
     * @param tabId Id of tab container
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of CrisLayoutBox {@link CrisLayoutBox}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisLayoutBox> findByEntityType
        (Context context, String entityType, Integer tabId, Integer limit, Integer offset) throws SQLException;

    /**
     * Returns the total number of metadata field
     * @param context The relevant DSpace Context
     * @param boxId id of the box
     * @return the total metadata number of the box
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long totalMetadatafield(Context context, Integer boxId) throws SQLException;

    /**
     * Find all tabs associated at an specific item
     * @param context The relevant DSpace Context
     * @param boxId id of the box
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of MetadataField {@link MetadataField}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<MetadataField> getMetadataField(Context context, Integer boxId, Integer limit, Integer offset)
            throws SQLException;

    /**
     * find a box by its shortname
     * @param context The relevant DSpace Context
     * @param entityTypeId The entity type id
     * @param shortname of the box to search
     * @return CrisLayoutBox if present, null otherwise
     * @throws SQLException An exception that provides information on a database errors.
     */
    public CrisLayoutBox findByShortname(Context context, Integer entityTypeId, String shortname) throws SQLException;
}
