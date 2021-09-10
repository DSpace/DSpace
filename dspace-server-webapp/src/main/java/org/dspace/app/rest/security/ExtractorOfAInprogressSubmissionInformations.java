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
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.WorkflowItemRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
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

    public Integer getAInprogressSubmissionID(@Nullable HttpServletRequest request, Integer versionHistoryId) {
        Context context = getContext(request);
        if (Objects.nonNull(versionHistoryId)) {
            try {
                VersionHistory versionHistory = versionHistoryService.find(context, versionHistoryId);
                if (Objects.nonNull(versionHistory)) {
                    Version oldestVersion = versionHistoryService.getLatestVersion(context, versionHistory);
                    WorkflowItem workflowItem = workflowItemService.findByItem(context, oldestVersion.getItem());
                    WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, oldestVersion.getItem());
                    if (Objects.nonNull(workspaceItem)) {
                        return workspaceItem.getID();
                    }
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

    public String getAInprogressSubmissionTarget(@Nullable HttpServletRequest request, Integer versionHistoryId) {
        Context context = getContext(request);
        if (Objects.nonNull(versionHistoryId)) {
            try {
                VersionHistory versionHistory = versionHistoryService.find(context, versionHistoryId);
                if (Objects.nonNull(versionHistory)) {
                    Version oldestVersion = versionHistoryService.getLatestVersion(context, versionHistory);
                    WorkflowItem workflowItem = workflowItemService.findByItem(context, oldestVersion.getItem());
                    WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, oldestVersion.getItem());
                    if (Objects.nonNull(workspaceItem)) {
                        return WorkspaceItemRest.NAME;
                    }
                    if (Objects.nonNull(workflowItem)) {
                        return WorkflowItemRest.NAME;
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return StringUtils.EMPTY;
    }

    private Context getContext(HttpServletRequest request) {
        return Objects.nonNull(request) ? ContextUtil.obtainContext(request) : null;
    }

}