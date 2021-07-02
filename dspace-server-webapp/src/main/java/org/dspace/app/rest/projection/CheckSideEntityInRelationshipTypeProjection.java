/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import java.sql.SQLException;

import javax.servlet.ServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.RelationshipTypeRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Check if an entity type is on the left or right side of a relationship type.
 */
@Component
public class CheckSideEntityInRelationshipTypeProjection extends AbstractProjection {

    private static final Logger log = LogManager.getLogger(CheckSideEntityInRelationshipTypeProjection.class);

    public static final String PROJECTION_NAME = "CheckSideEntityInRelationshipType";
    public static final String PARAM_NAME = "checkSideEntityInRelationshipType";

    @Autowired
    RequestService requestService;

    @Override
    public String getName() {
        return PROJECTION_NAME;
    }

    @Override
    public <T extends RestModel> T transformRest(T restObject) {
        try {
            transformRestInternal(restObject);
        } catch (SQLException e) {
            log.error(String.format("Something went wrong in %s", CheckSideItemInRelationshipProjection.class), e);
        }

        return super.transformRest(restObject);
    }

    protected void transformRestInternal(RestModel restObject) throws SQLException, DSpaceBadRequestException {
        // this projection only applies to RelationshipTypeRest
        if (!(restObject instanceof RelationshipTypeRest)) {
            return;
        }
        RelationshipTypeRest relationshipTypeRest = (RelationshipTypeRest) restObject;

        // ensure that relatedItemLeft and relatedItemRight are present in the response
        relationshipTypeRest.initProjectionCheckSideEntityInRelationshipType();

        // get entity type from request param checkSideEntityInRelationshipType
        String entityTypeStr = getEntityTypeFromRequest();
        if (entityTypeStr == null) {
            return;
        }

        // check if the requested entity type is on the left-hand side of this relationship type
        if (StringUtils.equals(entityTypeStr, relationshipTypeRest.getLeftType().getLabel())) {
            relationshipTypeRest.setRelatedTypeLeft();
        }

        // check if the requested entity type is on the right-hand side of this relationship type
        if (StringUtils.equals(entityTypeStr, relationshipTypeRest.getRightType().getLabel())) {
            relationshipTypeRest.setRelatedTypeRight();
        }
    }

    protected String getEntityTypeFromRequest() throws DSpaceBadRequestException {
        ServletRequest servletRequest = requestService.getCurrentRequest().getServletRequest();

        String[] entityTypes = servletRequest.getParameterValues(PARAM_NAME);

        if (entityTypes == null) {
            return null;
        }

        if (entityTypes.length != 1) {
            throw new DSpaceBadRequestException(String.format(
                "Expected one value for url parameter %s, got %s values", PARAM_NAME, entityTypes.length
            ));
        }

        return entityTypes[0];
    }

}
