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

import org.dspace.content.EntityType;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.service.DSpaceCRUDService;

public interface CrisLayoutBoxService extends DSpaceCRUDService<CrisLayoutBox> {

    /**
     * Create a new Box
     * @param context DSpace application context
     * @param box
     * @return
     * @throws SQLException
     */
    public CrisLayoutBox create(Context context, CrisLayoutBox box) throws SQLException;

    /**
     * Create a new Box with required field
     * @param context DSpace application context
     * @param eType EntiType
     * @param collapsed
     * @param priority
     * @param minor
     * @return
     * @throws SQLException
     */
    public CrisLayoutBox create(
            Context context,
            EntityType eType,
            boolean collapsed,
            int priority,
            boolean minor) throws SQLException;

    /**
     * Find all boxes by tab id
     * @param context DSpace application context
     * @param tabId Id of tab container
     * @return
     * @throws SQLException
     */
    public List<CrisLayoutBox> findByTabId(Context context, Integer tabId) throws SQLException;

    /**
     * Find all boxes by tab id
     * @param context
     * @param tabId
     * @param limit
     * @param offeset
     * @return
     * @throws SQLException
     */
    public List<CrisLayoutBox> findByTabId(Context context, Integer tabId, Integer limit, Integer offset)
            throws SQLException;

    /**
     * Returns the total number of boxes contained in the tab identified by its id
     * @param context
     * @param tabId
     * @return
     * @throws SQLException
     */
    public Long countTotalBoxesInTab(Context context, Integer tabId) throws SQLException;

}
