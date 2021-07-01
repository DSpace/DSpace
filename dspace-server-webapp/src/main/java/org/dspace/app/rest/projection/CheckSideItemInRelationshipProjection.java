/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.ServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.services.RequestService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Check if an item is on the left or right side of a relationship.
 */
@Component
public class CheckSideItemInRelationshipProjection extends AbstractProjection {

    private static final Logger log = LogManager.getLogger(CheckSideItemInRelationshipProjection.class);

    public static final String PROJECTION_NAME = "CheckSideItemInRelationship";
    public static final String PARAM_NAME = "checkSideItemInRelationship";

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
        // this projection only applies to RelationshipRest
        if (!(restObject instanceof RelationshipRest)) {
            return;
        }
        RelationshipRest relationshipRest = (RelationshipRest) restObject;

        // ensure that relatedItemLeft and relatedItemRight are present in the response
        relationshipRest.initProjectionCheckSideItemInRelationship();

        // get uuid from request param checkSideItemInRelationship
        UUID uuid = getUuidFromRequest();
        if (uuid == null) {
            return;
        }

        // check if the requested uuid is the left item
        if (uuid.equals(relationshipRest.getLeftId())) {
            relationshipRest.setRelatedItemLeft();
        }

        // check if the requested uuid is the right item
        if (uuid.equals(relationshipRest.getRightId())) {
            relationshipRest.setRelatedItemRight();
        }
    }

    protected UUID getUuidFromRequest() throws DSpaceBadRequestException {
        ServletRequest servletRequest = requestService.getCurrentRequest().getServletRequest();

        String[] uuids = servletRequest.getParameterValues(PARAM_NAME);

        if (uuids == null) {
            return null;
        }

        if (uuids.length != 1) {
            throw new DSpaceBadRequestException(String.format(
                "Expected one value for url parameter %s, got %s values", PARAM_NAME, uuids.length
            ));
        }

        return UUIDUtils.fromString(uuids[0]);
    }

}
