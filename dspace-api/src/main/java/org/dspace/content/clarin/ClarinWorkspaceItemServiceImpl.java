/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.content.service.clarin.ClarinWorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service interface class for the WorkspaceItem object created for Clarin-Dspace import.
 * Contains methods needed to import bitstream when dspace5 migrating to dspace7.
 * The implementation of this class is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author Michaela Paurikova(michaela.paurikova at dataquest.sk)
 */
public class ClarinWorkspaceItemServiceImpl implements ClarinWorkspaceItemService {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(
            ClarinWorkspaceItemServiceImpl.class);
    @Autowired
    private WorkspaceItemService workspaceItemService;
    @Autowired
    private WorkspaceItemDAO workspaceItemDAO;

    @Override
    public WorkspaceItem create(Context context, Collection collection, boolean multipleTitles, boolean publishedBefore,
                                boolean multipleFiles, Integer stageReached, Integer pageReached,
                                boolean template) throws AuthorizeException, SQLException {

        //create empty workspace item with item
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        //set workspace item values based on input values
        workspaceItem.setPublishedBefore(publishedBefore);
        workspaceItem.setMultipleFiles(multipleFiles);
        workspaceItem.setMultipleTitles(multipleTitles);
        workspaceItem.setPageReached(pageReached);
        workspaceItem.setStageReached(stageReached);
        return workspaceItem;
    }

    @Override
    public WorkspaceItem find(Context context, UUID uuid) throws SQLException {
        //find workspace item by its UUID
        WorkspaceItem workspaceItem = workspaceItemDAO.findByID(context, WorkspaceItem.class, uuid);

        //create log if the workspace item is not found
        if (log.isDebugEnabled()) {
            if (Objects.nonNull(workspaceItem)) {
                log.debug(LogHelper.getHeader(context, "find_workspace_item",
                        "not_found,workspace_item_uuid=" + uuid));
            } else {
                log.debug(LogHelper.getHeader(context, "find_workspace_item",
                        "workspace_item_uuid=" + uuid));
            }
        }
        return workspaceItem;
    }
}