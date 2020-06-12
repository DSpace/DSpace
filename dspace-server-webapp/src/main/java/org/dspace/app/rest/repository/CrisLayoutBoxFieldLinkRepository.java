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
import org.dspace.app.rest.model.CrisLayoutFieldRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.factory.CrisLayoutServiceFactory;
import org.dspace.layout.service.CrisLayoutFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Link repository for the fields subresource of a specific box
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component(CrisLayoutBoxRest.CATEGORY + "." + CrisLayoutBoxRest.NAME + "." + CrisLayoutBoxRest.FIELDS)
public class CrisLayoutBoxFieldLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private CrisLayoutServiceFactory serviceFactory;

    public Page<CrisLayoutFieldRest> getFields(
            @Nullable HttpServletRequest request,
            Integer boxId,
            @Nullable Pageable optionalPageable,
            Projection projection) {
        Context context = obtainContext();
        CrisLayoutFieldService service = serviceFactory.getFieldService();
        List<CrisLayoutField> fields = null;
        Long totalRow = null;

        Integer limit = null;
        Integer offset = null;
        if ( optionalPageable != null ) {
            limit = optionalPageable.getPageSize();
            offset = optionalPageable.getPageNumber() * optionalPageable.getPageSize();
        }
        try {
            totalRow = service.countFieldInBox(context, boxId);
            fields = service.findFieldByBoxId(context, boxId, limit, offset);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if ( fields == null ) {
            return null;
        }
        return converter.toRestPage(fields, optionalPageable, totalRow, utils.obtainProjection());
    }
}
