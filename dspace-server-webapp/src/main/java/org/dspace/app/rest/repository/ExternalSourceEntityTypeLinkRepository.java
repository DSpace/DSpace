/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.EntityTypeRest;
import org.dspace.app.rest.model.ExternalSourceRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.EntityType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Context;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Link repository for "EntityTypes" supported of an individual ExternalDataProvider.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(ExternalSourceRest.CATEGORY + "." + ExternalSourceRest.NAME + "." + ExternalSourceRest.ENTITY_TYPES)
public class ExternalSourceEntityTypeLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private ExternalDataService externalDataService;

    public Page<EntityTypeRest> getSupportedEntityTypes(@Nullable HttpServletRequest request,
                                                                  String externalSourceName,
                                                        @Nullable Pageable pageable,
                                                                  Projection projection) {
        Context context = ContextUtil.obtainContext(request);
        List<EntityType> entityTypes = Collections.emptyList();
        AbstractExternalDataProvider externalDataProvider = (AbstractExternalDataProvider)
                                     externalDataService.getExternalDataProvider(externalSourceName);
        if (Objects.isNull(externalDataProvider)) {
            throw new ResourceNotFoundException("No such ExternalDataProvider: " + externalSourceName);
        }
        int total = 0;
        List<String> supportedEntityTypes = externalDataProvider.getSupportedEntityTypes();
        try {
            if (CollectionUtils.isNotEmpty(supportedEntityTypes)) {
                entityTypes = entityTypeService.getEntityTypesByNames(context, supportedEntityTypes,
                                                            Math.toIntExact(pageable.getPageSize()),
                                                            Math.toIntExact(pageable.getOffset()));
                total = entityTypeService.countEntityTypesByNames(context, supportedEntityTypes);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(entityTypes, pageable, total, utils.obtainProjection());
    }

}