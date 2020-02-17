/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is the Repository that will take care of fetching the Version for a given Item
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.NAME + "." + ItemRest.VERSION)
public class ItemVersionLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private ItemService itemService;

    public VersionRest getItemVersion(@Nullable HttpServletRequest request,
                                      UUID itemUuid,
                                      @Nullable Pageable optionalPageable,
                                      Projection projection) throws SQLException {


        Context context = obtainContext();
        Item item = itemService.find(context, itemUuid);
        if (item == null) {
            throw new ResourceNotFoundException("The Item for uuid: " + itemUuid + " couldn't be found");
        }
        Version version = versioningService.getVersion(context, item);
        if (version == null) {
            return null;
        }
        return converter.toRest(version, projection);
    }
}
