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

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.SuggestionTargetRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "target" subresource of an suggestion target.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(SuggestionTargetRest.CATEGORY + "." + SuggestionTargetRest.PLURAL_NAME + "." + SuggestionTargetRest.TARGET)
public class SuggestionTargetTargetLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private ItemService itemService;

    /**
     * Returns the item related to the suggestion target with the given id.
     *
     * @param request    the http servlet request
     * @param id         the suggestion target UUID
     * @param pageable   the optional pageable
     * @param projection the projection object
     * @return the target item rest representation
     */
    @PreAuthorize("hasPermission(#id, 'SUGGESTIONTARGET', 'READ')")
    public ItemRest getTarget(@Nullable HttpServletRequest request, String id,
        @Nullable Pageable pageable, Projection projection) {
        String source = id.split(":")[0];
        UUID uuid = UUID.fromString(id.split(":")[1]);
        if (StringUtils.isBlank(source) || uuid == null) {
            throw new ResourceNotFoundException("No such item related to a suggestion target with UUID: " + id);
        }
        try {
            Context context = obtainContext();
            Item profile = itemService.find(context, uuid);
            if (profile == null) {
                throw new ResourceNotFoundException("No such item related to a suggestion target with UUID: " + id);
            }

            return converter.toRest(profile, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}
