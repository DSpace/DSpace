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

import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.app.rest.model.MetadataFieldRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.layout.factory.CrisLayoutServiceFactory;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Link repository for the security metadata subresource of a specific box
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component(CrisLayoutBoxRest.CATEGORY + "." + CrisLayoutBoxRest.NAME + "." + CrisLayoutBoxRest.SECURITY_METADATA)
public class CrisLayoutBoxMetadataLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private CrisLayoutServiceFactory serviceFactory;

    public Page<MetadataFieldRest> getSecurityMetadata(
            @Nullable HttpServletRequest request,
            Integer boxId,
            @Nullable Pageable pageable,
            Projection projection) {
        Context context = obtainContext();
        CrisLayoutBoxService service = serviceFactory.getBoxService();
        List<MetadataField> metadata = null;
        Long totalRow = null;

        Integer limit = null;
        Integer offset = null;
        if ( pageable != null ) {
            limit = pageable.getPageSize();
            offset = pageable.getPageNumber() * pageable.getPageSize();
        }
        try {
            totalRow = service.totalMetadataField(context, boxId);
            metadata = service.getMetadataField(
                context,
                boxId,
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
