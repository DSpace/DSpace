/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.WorkflowItemRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Methods of this class are used on PreAuthorize annotations
 * to convert input parameters.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(value = "extractorOf")
public class ExtractorOfAInprogressSubmissionInformations {

    @SuppressWarnings("rawtypes")
    @Autowired(required = true)
    private WorkflowItemService workflowItemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private VersionHistoryService versionHistoryService;

    @Autowired
    private VersioningService versionService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RequestService requestService;

    /**
     * This method is used on security checks, given a versionHistory id,
     * it searches the latest version and checks if there is
     * a Workspace/Workflow item in progress submission,
     * if yes return the id of this one, otherwise returns null.
     * 
     * @param request              The current request
     * @param versionHistoryId     VersionHistoryId
     * @return
     */
    public Integer getAInprogressSubmissionID(@Nullable HttpServletRequest request, Integer versionHistoryId) {
        Context context = getContext(request);
        if (Objects.nonNull(versionHistoryId)) {
            try {
                VersionHistory versionHistory = versionHistoryService.find(context, versionHistoryId);
                if (Objects.nonNull(versionHistory)) {
                    Version oldestVersion = versionHistoryService.getLatestVersion(context, versionHistory);
                    WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, oldestVersion.getItem());
                    if (Objects.nonNull(workspaceItem)) {
                        return workspaceItem.getID();
                    }
                    WorkflowItem workflowItem = workflowItemService.findByItem(context, oldestVersion.getItem());
                    if (Objects.nonNull(workflowItem)) {
                        return workflowItem.getID();
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * This method is used on security checks, given a versionHistory id,
     * it searches the latest version and checks if there is a Workspace/Workflow item in progress submission,
     * if yes return the rest name - 'workspaceitem' or 'workflowitem', otherwise it returns the empty string.
     * 
     * @param request             The current request
     * @param versionHistoryId    VersionHistoryId
     * @return
     */
    public String getAInprogressSubmissionTarget(@Nullable HttpServletRequest request, Integer versionHistoryId) {
        Context context = getContext(request);
        if (Objects.nonNull(versionHistoryId)) {
            try {
                VersionHistory versionHistory = versionHistoryService.find(context, versionHistoryId);
                if (Objects.nonNull(versionHistory)) {
                    Version oldestVersion = versionHistoryService.getLatestVersion(context, versionHistory);
                    if (Objects.nonNull(workspaceItemService.findByItem(context, oldestVersion.getItem()))) {
                        return WorkspaceItemRest.NAME;
                    }
                    if (Objects.nonNull(workflowItemService.findByItem(context, oldestVersion.getItem()))) {
                        return WorkflowItemRest.NAME;
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * This method is used on security checks, given an item UUID,
     * it searches the relative version, if version exist return its id, otherwise returns null.
     * 
     * @param request     The current request
     * @param uuid        Item uuid
     * @return
     */
    public Integer getVersionIdByItemUUID(@Nullable HttpServletRequest request, UUID uuid) {
        Context context = getContext(request);
        if (Objects.nonNull(uuid)) {
            try {
                Item item = itemService.find(context, uuid);
                if (Objects.nonNull(item)) {
                    Version version = versionService.getVersion(context, item);
                    return Objects.nonNull(version) ? version.getID() : null;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return null;
    }

    private Context getContext(HttpServletRequest request) {
        HttpServletRequest currentRequest = requestService.getCurrentRequest().getHttpServletRequest();
        return Objects.nonNull(request) ? ContextUtil.obtainContext(request)
                                        : ContextUtil.obtainContext(currentRequest);
    }

}