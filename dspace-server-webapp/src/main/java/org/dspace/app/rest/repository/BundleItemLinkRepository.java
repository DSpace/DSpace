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

import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "item" subresource of an individual bundle.
 */
@Component(BundleRest.CATEGORY + "." + BundleRest.NAME + "." + BundleRest.ITEM)
public class BundleItemLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    BundleService bundleService;

    /**
     * Get the item where the provided bundle resides in
     */
    @PreAuthorize("hasPermission(#bundleId, 'BUNDLE', 'READ')")
    public ItemRest getItem(@Nullable HttpServletRequest request,
                            UUID bundleId,
                            @Nullable Pageable optionalPageable,
                            Projection projection) {
        try {
            Context context = obtainContext();
            Bundle bundle = bundleService.find(context, bundleId);
            if (bundle == null) {
                throw new ResourceNotFoundException("No such bundle: " + bundleId);
            }
            Item item = bundle.getItems().get(0);
            if (item == null) {
                return null;
            }
            return converter.toRest(item, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
