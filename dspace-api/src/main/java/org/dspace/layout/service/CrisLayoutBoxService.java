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
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxConfiguration;
import org.dspace.service.DSpaceCRUDService;

/**
 * Interface of service to manage Boxes component of layout
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public interface CrisLayoutBoxService extends DSpaceCRUDService<CrisLayoutBox> {

    /**
     * This method stores in the database a CrisLayoutBox {@Link CrisLayoutBox} instance
     * @param context The relevant DSpace Context
     * @param box CrisLayoutBox instance to store in the database {@link CrisLayoutBox}
     * @return the stored CrisLayoutBox instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    public CrisLayoutBox create(Context context, CrisLayoutBox box) throws SQLException, AuthorizeException;

    /**
     * Create and store in the database a new CrisLayoutBox {@Link CrisLayoutBox} instance
     * with required field
     * @param context The relevant DSpace Context
     * @param eType EntiType of new Box {@link EntityType}
     * @param boxType the type of Box
     * @param collapsed this attribute define if the box is collapsed or not
     * @param priority this attribute is used for define the position of the box in its tab
     * @param minor this attribute is used to flag box that should be ignored in the determination
     *              of the tab visualization
     * @return the stored CrisLayoutBox instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    public CrisLayoutBox create(
            Context context,
            EntityType eType,
            String boxType,
            boolean collapsed,
            int priority,
            boolean minor) throws SQLException, AuthorizeException;

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
    public List<CrisLayoutBox> findByTabId(Context context, Integer tabId, Integer limit, Integer offset)
            throws SQLException;

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
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of CrisLayoutBox {@link CrisLayoutBox}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisLayoutBox> findEntityBoxes(
            Context context, String entityType, Integer limit, Integer offset) throws SQLException;

    /**
     * Returns all metadata field associated at box
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
     * Returns the total number of metadata field
     * @param context The relevant DSpace Context
     * @param boxId id of the box
     * @return the total metadata number of the box
     * @throws SQLException An exception that provides information on a database errors.
     */
    public Long totalMetadataField(Context context, Integer boxId) throws SQLException;

    /**
     * Find all tabs associated at an specific item
     * @param context The relevant DSpace Context
     * @param itemUuid String that represents UUID of item {@link item}
     * @param tabId Id of tab container
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of CrisLayoutBox {@link CrisLayoutBox}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisLayoutBox> findByItem(
            Context context, UUID itemUuid, Integer tabId) throws SQLException;

    /**
     * find a box by its shortname
     * @param context The relevant DSpace Context
     * @param entityType the label of the entityType
     * @param shortname of the box to search
     * @return CrisLayoutBox if present, null otherwise
     * @throws SQLException An exception that provides information on a database errors.
     */
    public CrisLayoutBox findByShortname(Context context, String entityType, String shortname) throws SQLException;

    /**
     * Check if the box has content to show
     * @param box CrisLayoutBox instance
     * @param values metadataValue of item
     * @return true if the box has contet to show, false otherwise
     */
    public boolean hasContent(CrisLayoutBox box, List<MetadataValue> values);

    /**
     * Retrieve the configuration details of a specific box. By default the
     * configuration object is just a wrapper of box object as all the details are
     * currently stored inside the box object itself
     * 
     * @param context the dspace context
     * @param box     the CrisLayoutBox
     * @return the configuration details
     */
    public CrisLayoutBoxConfiguration getConfiguration(Context context, CrisLayoutBox box);
}
