package org.dspace.app.rest.projection;

import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.ServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;

// TODO
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

    protected void transformRestInternal(RestModel restObject) throws SQLException {
        // this projection only applies to RelationshipRest
        if (!(restObject instanceof RelationshipRest)) {
            return;
        }
        RelationshipRest relationshipRest = (RelationshipRest) restObject;

        // get uuid from request param checkSideItemInRelationship
        UUID uuid = getUuidFromRequest();
        if (uuid == null) {
            return;
        }

        // ensure that relatedItemLeft and relatedItemRight are present in the response
        relationshipRest.initProjectionCheckSideItemInRelationship();

        // check if the requested uuid is the left item
        if (uuid.equals(relationshipRest.getLeftId())) {
            relationshipRest.setRelatedItemLeft();
        }

        // check if the requested uuid is the right item
        if (uuid.equals(relationshipRest.getRightId())) {
            relationshipRest.setRelatedItemRight();
        }
    }

    protected UUID getUuidFromRequest() {
        ServletRequest servletRequest = requestService.getCurrentRequest().getServletRequest();

        String[] uuids = servletRequest.getParameterValues(PARAM_NAME);

        if (uuids == null) {
            return null;
        }

        if (uuids.length != 1) {
            log.warn(String.format("Expected one value for url parameter %s, got %s values", PARAM_NAME, uuids.length));
            return null;
        }

        return UUIDUtils.fromString(uuids[0]);
    }

}
