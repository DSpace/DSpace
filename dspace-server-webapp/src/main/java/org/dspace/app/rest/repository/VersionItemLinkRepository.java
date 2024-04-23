/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This Repository takes care of the retrieval of the {@link org.dspace.content.Item} objects
 * for a given {@link Version}
 */
@Component(VersionRest.CATEGORY + "." + VersionRest.PLURAL_NAME + "." + VersionRest.ITEM)
public class VersionItemLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private VersioningService versioningService;

    /**
     * This method will return the ItemRest object constructed from the Item object which is found in the Version
     * that will be found through the versionId parameter
     * @param request           The current request
     * @param versionId         The ID for the Version to be used
     * @param optionalPageable  The pageable if present
     * @param projection        The current Projection
     * @return                  The ItemRest object that is relevant for the Version
     * @throws SQLException     If something goes wrong
     */
    public ItemRest getVersionItem(@Nullable HttpServletRequest request,
                                   Integer versionId,
                                   @Nullable Pageable optionalPageable,
                                   Projection projection) throws SQLException {


        Context context = obtainContext();
        Version version = versioningService.getVersion(context, versionId);
        if (version == null) {
            throw new ResourceNotFoundException("The version with ID: " + versionId + " couldn't be found");
        }
        Item item = version.getItem();
        if (item == null) {
            return null;
        }
        return converter.toRest(item, projection);
    }
}
