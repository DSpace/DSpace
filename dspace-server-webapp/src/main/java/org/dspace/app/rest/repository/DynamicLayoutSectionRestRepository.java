/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.DynamicLayoutSectionRest;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.layout.DynamicLayoutSection;
import org.dspace.layout.service.DynamicLayoutSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage {@link DynamicLayoutSectionRest}
 * Rest object.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component(DynamicLayoutSectionRest.CATEGORY + "." + DynamicLayoutSectionRest.NAME_PLURAL)
public class DynamicLayoutSectionRestRepository extends DSpaceRestRepository<DynamicLayoutSectionRest, String> {

    @Autowired
    private DynamicLayoutSectionService dynamicLayoutSectionService;

    @Override
    @PreAuthorize("permitAll()")
    public DynamicLayoutSectionRest findOne(Context context, String id) {
        DynamicLayoutSection layoutSection = dynamicLayoutSectionService.findOne(id);
        if (layoutSection == null) {
            return null;
        }
        return converter.toRest(layoutSection, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("permitAll()")
    public Page<DynamicLayoutSectionRest> findAll(Context context, Pageable pageable) {
        List<DynamicLayoutSection> layoutSections = dynamicLayoutSectionService.findAll();
        int total = dynamicLayoutSectionService.countTotal();
        return converter.toRestPage(layoutSections, pageable, total, utils.obtainProjection());
    }

    /**
     * Returns the layout sections that are visible in the top bar.
     *
     * @param q an optional query used to filter the sections
     * @param pageable the pagination information
     * @return a page of visible top-bar sections
     * @throws SearchServiceException if the search fails
     */
    @SearchRestMethod(name = "visibleTopBarSections")
    @PreAuthorize("permitAll()")
    public Page<DynamicLayoutSectionRest> searchVisibleTopBarSections(@Parameter(value = "query") String q,
        Pageable pageable) throws SearchServiceException {
        List<DynamicLayoutSection> layoutSections = dynamicLayoutSectionService.findAllVisibleSectionsInTopBar();
        int total = dynamicLayoutSectionService.countVisibleSectionsInTopBar();
        return converter.toRestPage(layoutSections, pageable, total, utils.obtainProjection());
    }
    @Override
    public Class<DynamicLayoutSectionRest> getDomainClass() {
        return DynamicLayoutSectionRest.class;
    }

}
