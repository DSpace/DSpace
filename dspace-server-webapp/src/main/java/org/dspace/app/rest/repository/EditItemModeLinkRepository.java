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
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.EditItemModeRest;
import org.dspace.app.rest.model.EditItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component(EditItemRest.CATEGORY + "." + EditItemRest.NAME_PLURAL + "." + EditItemRest.MODE)
public class EditItemModeLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private EditItemModeService eimService;

    @PreAuthorize("permitAll")
    public Page<EditItemModeRest> getModes(
            @Nullable HttpServletRequest request, String id,
            @Nullable Pageable pageable, Projection projection) {
        Context context = obtainContext();
        String[] values = id.split(":");
        List<EditItemMode> modes = null;
        try {
            modes = eimService.findModes(context, UUID.fromString(values[0]));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (modes == null) {
            return null;
        }
        return converter.toRestPage(modes, pageable, modes.size(), utils.obtainProjection());
    }

}
