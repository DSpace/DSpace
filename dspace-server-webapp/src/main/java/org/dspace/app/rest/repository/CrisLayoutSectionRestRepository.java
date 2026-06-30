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
import org.dspace.app.rest.model.CrisLayoutSectionRest;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.layout.CrisLayoutSection;
import org.dspace.layout.service.CrisLayoutSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage {@link CrisLayoutSectionRest}
 * Rest object.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component(CrisLayoutSectionRest.CATEGORY + "." + CrisLayoutSectionRest.NAME_PLURAL)
public class CrisLayoutSectionRestRepository extends DSpaceRestRepository<CrisLayoutSectionRest, String> {

    @Autowired
    private CrisLayoutSectionService crisLayoutSectionService;

    @Override
    @PreAuthorize("permitAll()")
    public CrisLayoutSectionRest findOne(Context context, String id) {
        CrisLayoutSection layoutSection = crisLayoutSectionService.findOne(id);
        if (layoutSection == null) {
            return null;
        }
        return converter.toRest(layoutSection, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("permitAll()")
    public Page<CrisLayoutSectionRest> findAll(Context context, Pageable pageable) {
        List<CrisLayoutSection> layoutSections = crisLayoutSectionService.findAll();
        int total = crisLayoutSectionService.countTotal();
        return converter.toRestPage(layoutSections, pageable, total, utils.obtainProjection());
    }

    @SearchRestMethod(name = "visibleTopBarSections")
    @PreAuthorize("permitAll()")
    public Page<CrisLayoutSectionRest> searchVisibleTopBarSections(@Parameter(value = "query") String q,
        Pageable pageable) throws SearchServiceException {
        List<CrisLayoutSection> layoutSections = crisLayoutSectionService.findAllVisibleSectionsInTopBar();
        int total = crisLayoutSectionService.countVisibleSectionsInTopBar();
        return converter.toRestPage(layoutSections, pageable, total, utils.obtainProjection());
    }
    @Override
    public Class<CrisLayoutSectionRest> getDomainClass() {
        return CrisLayoutSectionRest.class;
    }

}
