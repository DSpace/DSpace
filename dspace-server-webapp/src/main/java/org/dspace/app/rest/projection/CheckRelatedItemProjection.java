package org.dspace.app.rest.projection;

import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.ServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TODO
 */
@Component
public class CheckRelatedItemProjection extends AbstractProjection {

    private static final Logger log = LogManager.getLogger(CheckRelatedItemProjection.class);

    public static final String PROJECTION_NAME = "CheckRelatedItem";
    public static final String PARAM_NAME = "checkRelatedItem";

    @Autowired
    RequestService requestService;

    @Autowired
    ItemService itemService;

    @Autowired
    RelationshipService relationshipService;

    @Override
    public String getName() {
        return PROJECTION_NAME;
    }

    @Override
    public <T extends RestModel> T transformRest(T restObject) {
        if (restObject instanceof ItemRest) {
            ServletRequest servletRequest = requestService.getCurrentRequest().getServletRequest();
            Context context = ContextUtil.obtainContext(servletRequest);
            ItemRest itemRest = (ItemRest) restObject;

            // TODO
            itemRest.initRelatedItems();

            String[] uuidStrs = servletRequest.getParameterValues(PARAM_NAME);
            if (uuidStrs != null) {
                for (String uuidStr : uuidStrs) {
                    try {
                        handleParameterValue(context, itemRest, uuidStr);
                    } catch (SQLException e) {
                        log.error("Something went wrong when processing a parameter value", e);
                    }
                }
            }
        }

        return super.transformRest(restObject);
    }

    protected void handleParameterValue(Context context, ItemRest itemRest, String uuidStr) throws SQLException {
        UUID uuid = UUIDUtils.fromString(uuidStr);
        if (uuid == null) {
            return;
        }

        Item item = itemService.find(context, uuid);
        if (item == null) {
            return;
        }

        int count = relationshipService.countByItem(context, item);
        if (count <= 0) {
            return;
        }

        itemRest.addRelatedItem(UUID.fromString(uuidStr));
    }

}
