/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.EntityTypeRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.EntityType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller will handle all the incoming calls on the /api/core/entitytypes/label/<:entity-type-label> endpoint
 * where the entity-type-label parameter can be filled in to match a specific entityType by label
 * There's always at most one entity type per label.
 * <p>
 * It responds with:
 * <p>
 * The single entity type if there's a match
 * 404 if the entity type doesn't exist
 *
 * @author Maria Verdonck (Atmire) on 2019-12-13
 */
@RestController
@RequestMapping("/api/" + EntityTypeRest.CATEGORY + "/" + EntityTypeRest.NAME_PLURAL)
public class EntityTypeLabelRestController {

    protected final EntityTypeService entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();

    @Autowired
    protected ConverterService converter;

    @Autowired
    protected Utils utils;

    @GetMapping("/label/{entity-type-label}")
    public EntityTypeRest get(HttpServletRequest request, HttpServletResponse response,
                              @PathVariable("entity-type-label") String label) {
        Context context = ContextUtil.obtainContext(request);
        try {
            EntityType entityType = this.entityTypeService.findByEntityType(context, label);
            if (entityType == null) {
                throw new ResourceNotFoundException("There was no entityType found with label: " + label);
            }
            return converter.toRest(entityType, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
