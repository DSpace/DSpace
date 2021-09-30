/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.sql.SQLException;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.AInprogressSubmissionRest;
import org.dspace.app.rest.model.VersionHistoryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the Repository that retrieve the most recent version in the history
 * that could live eventually in the workspace or workflow.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(VersionHistoryRest.CATEGORY + "." + VersionHistoryRest.NAME + "." + VersionHistoryRest.DRAFT_VERSION)
public class VersionHistoryDraftVersionLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    private VersionHistoryService versionHistoryService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @SuppressWarnings("rawtypes")
    @Autowired(required = true)
    private WorkflowItemService workflowItemService;

    @PreAuthorize("@versioningSecurity.isEnableVersioning() && " +
                  "hasPermission(@extractorOf.getAInprogressSubmissionID(#request, #versionHistoryId), " +
                  "@extractorOf.getAInprogressSubmissionTarget(#request, #versionHistoryId), 'READ')")
    public AInprogressSubmissionRest getDraftVersion(@Nullable HttpServletRequest request,
                                                               Integer versionHistoryId,
                                                     @Nullable Pageable optionalPageable,
                                                               Projection projection) throws SQLException {
        Context context = obtainContext();
        if (Objects.isNull(versionHistoryId) || versionHistoryId < 0) {
            throw new DSpaceBadRequestException("Provided id is not correct!");
        }
        VersionHistory versionHistory = versionHistoryService.find(context, versionHistoryId);
        if (Objects.isNull(versionHistory)) {
            throw new ResourceNotFoundException("No such version found");
        }
        Version oldestVersion = versionHistoryService.getLatestVersion(context, versionHistory);

        if (Objects.nonNull(oldestVersion) && Objects.nonNull(oldestVersion.getItem())) {
            WorkflowItem workflowItem = workflowItemService.findByItem(context, oldestVersion.getItem());
            WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, oldestVersion.getItem());
            if (Objects.nonNull(workflowItem)) {
                return converter.toRest(workflowItem, projection);
            }
            if (Objects.nonNull(workspaceItem)) {
                return converter.toRest(workspaceItem, projection);
            }
        }
        return null;
    }

}