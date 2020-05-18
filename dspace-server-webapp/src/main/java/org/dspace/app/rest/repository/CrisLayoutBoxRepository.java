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

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage CrisLayoutBox Rest object
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component(CrisLayoutBoxRest.CATEGORY + "." + CrisLayoutBoxRest.NAME)
public class CrisLayoutBoxRepository extends DSpaceRestRepository<CrisLayoutBoxRest, Integer>
    implements ReloadableEntityObjectRepository<CrisLayoutBox, Integer> {

    @Autowired
    private CrisLayoutBoxService service;

    @Override
    public CrisLayoutBox findDomainObjectByPk(Context context, Integer id) throws SQLException {
        return service.find(context, id);
    }

    @Override
    public Class<Integer> getPKClass() {
        return Integer.class;
    }

    @Override
    public CrisLayoutBoxRest findOne(Context context, Integer id) {
        CrisLayoutBox box = null;
        try {
            box = service.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if ( box == null ) {
            return null;
        }
        return converter.toRest(box, utils.obtainProjection());
    }

    @SearchRestMethod(name = "findByEntityType")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<CrisLayoutBoxRest> findByEnityType(
            @Parameter(value = "type", required = true) String type, Pageable pageable) {
        Context context = obtainContext();
        List<CrisLayoutBox> boxList = null;
        Long totalRow = null;
        try {
            totalRow = service.countTotalEntityBoxes(context, type);
            boxList = service.findEntityBoxes(
                    context,
                    type,
                    pageable.getPageSize(),
                    pageable.getPageSize() * pageable.getPageNumber());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(boxList, pageable, totalRow, utils.obtainProjection());
    }

    @SearchRestMethod(name = "findByItem")
    public Page<CrisLayoutBoxRest> findByItem(
            @Parameter(value = "uuid", required = true) String itemUuid,
            @Parameter(value = "tab", required = true) Integer tabId,
            Pageable pageable) {
        Context context = obtainContext();
        List<CrisLayoutBox> boxList = null;
        Long totalRow = null;
        try {
            boxList = service.findByItem(
                    context,
                    UUID.fromString(itemUuid),
                    tabId);
            totalRow = Long.valueOf(boxList.size());
            int lastIndex = (pageable.getPageNumber() + 1) * pageable.getPageSize();
            boxList = boxList.subList(
                    pageable.getPageNumber() * pageable.getPageSize(),
                    (boxList.size() < lastIndex) ? boxList.size() : lastIndex );
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(boxList, pageable, totalRow, utils.obtainProjection());
    }

    @Override
    public Page<CrisLayoutBoxRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not Implemented!", "");
    }

    @Override
    public Class<CrisLayoutBoxRest> getDomainClass() {
        return CrisLayoutBoxRest.class;
    }

}
