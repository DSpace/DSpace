/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.VersionHistoryRest;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the Repository that takes care of the retrieval of the {@link Version} objects for a given
 * {@link VersionHistory}
 */
@Component(VersionHistoryRest.CATEGORY + "." + VersionHistoryRest.NAME + "." + VersionHistoryRest.VERSIONS)
public class VersionsLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private VersionHistoryService versionHistoryService;

    @Autowired
    private VersioningService versioningService;

    /**
     * This method will return a page of VersionRest objects found through the VersionHistory object that is resolved
     * from the versionHistoryId parameter
     * @param request           The current request
     * @param versionHistoryId  The ID for the VersionHistory to be used
     * @param optionalPageable  The pageable if present
     * @param projection        The current Projection
     * @return                  The page containing relevant VersionRest objects
     * @throws SQLException     If something goes wrong
     */
    @PreAuthorize("@versioningSecurity.isEnableVersioning() && " +
                  "hasPermission(#versionHistoryId, 'VERSIONHISTORY', 'READ')")
    public Page<VersionRest> getVersions(@Nullable HttpServletRequest request,
                                               Integer versionHistoryId,
                                               @Nullable Pageable optionalPageable,
                                               Projection projection) throws SQLException {

        Context context = obtainContext();
        int total = 0;
        VersionHistory versionHistory = versionHistoryService.find(context, versionHistoryId);
        if (versionHistory == null) {
            throw new ResourceNotFoundException("The versionHistory with ID: " + versionHistoryId +
                                                    " couldn't be found");
        }
        Pageable pageable = optionalPageable != null ? optionalPageable : PageRequest.of(0, 20);
        List<Version> versions = versioningService.getVersionsByHistoryWithItems(context, versionHistory,
                                                   Math.toIntExact(pageable.getOffset()),
                                                   Math.toIntExact(pageable.getPageSize()));
        total = versioningService.countVersionsByHistoryWithItem(context, versionHistory);
        return converter.toRestPage(versions, pageable, total, projection);
    }
}
