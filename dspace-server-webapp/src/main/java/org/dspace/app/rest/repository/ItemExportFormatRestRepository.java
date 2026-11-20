/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.apache.commons.lang3.EnumUtils.isValidEnum;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.ItemExportFormatRest;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.integration.crosswalks.service.ItemExportFormat;
import org.dspace.content.integration.crosswalks.service.ItemExportFormatService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;



/**
 * This is the repository responsible to manage ItemExportFormat Rest object
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
@Component(ItemExportFormatRest.CATEGORY + "." + ItemExportFormatRest.PLURAL_NAME)
public class ItemExportFormatRestRepository extends DSpaceRestRepository<ItemExportFormatRest, String> {

    public static Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemExportFormatRestRepository.class);

    @Autowired
    public ItemExportFormatService itemExportFormatService;

    @Override
    @PreAuthorize("permitAll()")
    public ItemExportFormatRest findOne(Context context, String id) {

        ItemExportFormat ief = this.itemExportFormatService.get(context, id);

        if (ief == null) {
            return null;
        }

        return converter.toRest(ief, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("permitAll()")
    public Page<ItemExportFormatRest> findAll(Context context, Pageable pageable) {

        List<ItemExportFormat> formats = this.itemExportFormatService.getAll(context);

        return converter.toRestPage(formats, pageable, utils.obtainProjection());
    }

    @Override
    public Class<ItemExportFormatRest> getDomainClass() {
        return ItemExportFormatRest.class;
    }

    @SearchRestMethod(name = "byEntityTypeAndMolteplicity")
    @PreAuthorize("permitAll()")
    public Page<ItemExportFormatRest> byEntityTypeAndMolteplicity(
            @Parameter(value = "entityTypeId") String entityTypeId,
            @Parameter(value = "molteplicity") String molteplicity,
            Pageable pageable) {

        List<ItemExportFormat> formats = this.itemExportFormatService
            .byEntityTypeAndMolteplicity(obtainContext(), entityTypeId, getCrosswalkMode(molteplicity));

        return converter.toRestPage(formats, pageable, utils.obtainProjection());
    }

    private CrosswalkMode getCrosswalkMode(String molteplicity) {
        if (StringUtils.isBlank(molteplicity)) {
            return CrosswalkMode.SINGLE_AND_MULTIPLE;
        }

        if (!isValidEnum(CrosswalkMode.class, molteplicity.toUpperCase())) {
            throw new DSpaceBadRequestException("The given molteplicity is unknown.");
        }

        return CrosswalkMode.valueOf(molteplicity.toUpperCase());

    }

}
