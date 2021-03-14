/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.VersionHistoryRest;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is the Repository that takes care of the retrieval of the {@link VersionHistory} object
 * for a given {@link Version}
 */
@Component(VersionRest.CATEGORY + "." + VersionRest.NAME + "." + VersionRest.VERSION_HISTORY)
public class VersionHistoryLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private VersioningService versioningService;

    /**
     * This method will retrieve the VersionHistoryRest object from the Version that is found by the associated
     * versionId parameter
     * @param request           The current request
     * @param versionId         The ID for the Version object
     * @param optionalPageable  The pageable if present
     * @param projection        The current Projection
     * @return                  The VersionHistoryRest object that is constructed from the VersionHistory object that
     *                          is linked to the Version found by the versionId parameter
     * @throws SQLException     If something goes wrong
     */
    public VersionHistoryRest getVersionHistory(@Nullable HttpServletRequest request,
                                                Integer versionId,
                                                @Nullable Pageable optionalPageable,
                                                Projection projection) throws SQLException {

        Context context = obtainContext();
        Version version = versioningService.getVersion(context, versionId);
        if (version == null) {
            throw new ResourceNotFoundException("The version with ID: " + versionId + " couldn't be found");
        }
        VersionHistory versionHistory = version.getVersionHistory();
        if (versionHistory == null) {
            return null;
        }
        return converter.toRest(versionHistory, projection);
    }
}
