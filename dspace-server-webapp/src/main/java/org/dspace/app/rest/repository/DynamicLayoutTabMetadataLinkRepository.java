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

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.DynamicLayoutTabRest;
import org.dspace.app.rest.model.MetadataFieldRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.layout.factory.DynamicLayoutServiceFactory;
import org.dspace.layout.service.DynamicLayoutTabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for the security metadata subresource of a specific tab
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component(DynamicLayoutTabRest.CATEGORY + "." + DynamicLayoutTabRest.NAME_PLURAL + "."
        + DynamicLayoutTabRest.SECURITY_METADATA)
public class DynamicLayoutTabMetadataLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private DynamicLayoutServiceFactory serviceFactory;

    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<MetadataFieldRest> getSecurityMetadata(
            @Nullable HttpServletRequest request,
            Integer tabId,
            @Nullable Pageable pageable,
            Projection projection) {
        Context context = obtainContext();
        DynamicLayoutTabService service = serviceFactory.getTabService();
        List<MetadataField> metadata = null;
        Long totalRow = null;

        Integer limit = null;
        Integer offset = null;
        if ( pageable != null ) {
            limit = pageable.getPageSize();
            offset = pageable.getPageNumber() * pageable.getPageSize();
        }
        try {
            totalRow = service.totalMetadataField(context, tabId);
            metadata = service.getMetadataField(
                context,
                tabId,
                limit,
                offset);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (metadata == null) {
            return null;
        }
        return converter.toRestPage(metadata, pageable, totalRow, utils.obtainProjection());
    }
}
