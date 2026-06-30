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
     * @param minor this attribute is used to flag box that should be ignored in the determination
     *              of the tab visualization
     * @return the stored CrisLayoutBox instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    public CrisLayoutBox create(Context context, EntityType eType, String boxType, boolean collapsed, boolean minor)
        throws SQLException, AuthorizeException;

    /**
     * Returns the boxes that are available for the specified entity type
     * @param context The relevant DSpace Context
     * @param entityType entity type label {@link EntityType}
     * @param limit how many results return
     * @param offset the position of the first result to return
     * @return List of CrisLayoutBox {@link CrisLayoutBox}
     * @throws SQLException An exception that provides information on a database errors.
     */
    public List<CrisLayoutBox> findByEntityType(Context context, String entityType, Integer limit, Integer offset)
        throws SQLException;

    /**
     * Check if the box has content to show
     *
     * @param context The relevant DSpace Context
     * @param box     CrisLayoutBox instance
     * @param item    the box's item
     * @return true if the box has content to show, false otherwise
     */
    public boolean hasContent(Context context, CrisLayoutBox box, Item item);

    /**
     * Establishes wether or not, current user is enabled to have access to layout data
     * contained in a layout box for a given Item.
     *
     * @param context current Context
     * @param box     layout box
     * @param item    item to whom metadata contained in the box belong to
     * @return true if access has to be granded, false otherwise
     */
    public boolean hasAccess(Context context, CrisLayoutBox box, Item item);

    /**
     * Retrieve the configuration details of a specific box. By default the
     * configuration object is just a wrapper of box object as all the details are
     * currently stored inside the box object itself
     *
     * @param box     the CrisLayoutBox
     * @return the configuration details
     */
    public CrisLayoutBoxConfiguration getConfiguration(CrisLayoutBox box);
    /**
     * Retrieve the configuration details of a specific box. By default the
     * configuration object is just a wrapper of box object as all the details are
     * currently stored inside the box object itself
     *
     * @param context the dspace context
     * @param entity    entity type
     * @return type type of the box
     */
    public List<CrisLayoutBox> findByEntityAndType(Context context, String entity, String type);
}
