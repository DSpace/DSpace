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
import org.dspace.app.rest.model.CrisLayoutTabRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.factory.CrisLayoutServiceFactory;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for the boxes subresource of a specific tab
 * 
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
@Component(CrisLayoutTabRest.CATEGORY + "." + CrisLayoutTabRest.NAME + "." + CrisLayoutTabRest.BOXES)
public class CrisLayoutTabBoxLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private CrisLayoutServiceFactory serviceFactory;

    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<CrisLayoutBoxRest> getBoxes(
            @Nullable HttpServletRequest request,
            Integer tabId,
            @Nullable Pageable optionalPageable,
            Projection projection) {
        Context context = obtainContext();
        CrisLayoutBoxService service = serviceFactory.getBoxService();
        List<CrisLayoutBox> boxes = null;
        Long totalRow = null;

        Integer limit = null;
        Integer offset = null;
        if ( optionalPageable != null ) {
            limit = optionalPageable.getPageSize();
            offset = optionalPageable.getPageNumber() * optionalPageable.getPageSize();
        }
        try {
            totalRow = service.countTotalBoxesInTab(context, tabId);
            boxes = service.findByTabId(context, tabId, limit, offset);
        } catch ( SQLException e ) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if ( boxes == null ) {
            return null;
        }
        return converter.toRestPage(boxes, optionalPageable, totalRow, utils.obtainProjection());
    }

}
