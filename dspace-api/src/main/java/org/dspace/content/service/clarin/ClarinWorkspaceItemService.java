/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service.clarin;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;

/**
 * Service interface class for the WorkspaceItem object created for Clarin-Dspace import.
 * Contains methods needed to import bitstream when dspace5 migrating to dspace7.
 * The implementation of this class is autowired by spring.
 *
 * @author Michaela Paurikova(michaela.paurikova at dataquest.sk)
 */
public interface ClarinWorkspaceItemService {

    /**
     * Create a new empty workspace item.
     * Set workspace item attributes by its input values.
     * @param context         context
     * @param collection      Collection being submitted to
     * @param multipleTitles  contains multiple titles
     * @param publishedBefore published before
     * @param multipleFiles   contains multiple files
     * @param stageReached    stage reached
     * @param pageReached     page reached
     * @param template        if <code>true</code>, the workspace item starts as a copy
     *                        of the collection's template item
     * @return created workspace item
     * @throws AuthorizeException if authorization error
     * @throws SQLException       if database error
     */
    public WorkspaceItem create(Context context, Collection collection,
                                boolean multipleTitles, boolean publishedBefore,
                                boolean multipleFiles, Integer stageReached,
                                Integer pageReached, boolean template)
            throws AuthorizeException, SQLException;

    /***
     * Find workspace item by its UUID.
     * @param context context
     * @param uuid    workspace item UUID
     * @return found workspace item
     * @throws SQLException if database error
     */
    public WorkspaceItem find(Context context, UUID uuid) throws SQLException;
}
