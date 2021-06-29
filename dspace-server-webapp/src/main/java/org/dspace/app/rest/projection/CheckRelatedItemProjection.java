/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.ServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Check if one or more item uuids are related to the requested item.
 *
 * Example request: {{url}}/api/core/items/{{requested-item}}?projection=CheckRelatedItem
 *                  &checkRelatedItem={{related-item}}&checkRelatedItem={{unrelated-item}}
 *
 * In the example above, the response will contain a relatedItems list that contains {{related-item}}.
 *
 * If this projection is not requested, then relatedItems is not present in the response.
 * If this projection is requested but no related items are found, an empty array is returned in the response.
 */
@Component
public class CheckRelatedItemProjection extends AbstractProjection {

    private static final Logger log = LogManager.getLogger(CheckRelatedItemProjection.class);

    public static final String PROJECTION_NAME = "CheckRelatedItem";
    public static final String PARAM_NAME = "checkRelatedItem";
    public static final String RELATIONSHIP_UUID_SEPARATOR = "=";

    @Autowired
    RequestService requestService;

    @Autowired
    ItemService itemService;

    @Autowired
    RelationshipService relationshipService;

    @Autowired
    RelationshipTypeService relationshipTypeService;

    @Override
    public String getName() {
        return PROJECTION_NAME;
    }

    @Override
    public <T extends RestModel> T transformRest(T restObject) {
        try {
            transformRestInternal(restObject);
        } catch (SQLException e) {
            log.error(String.format("Something went wrong in %s", CheckRelatedItemProjection.class), e);
        }

        return super.transformRest(restObject);
    }

    protected void transformRestInternal(RestModel restObject) throws SQLException {
        ServletRequest servletRequest = requestService.getCurrentRequest().getServletRequest();
        Context context = ContextUtil.obtainContext(servletRequest);

        // this projection only applies to ItemRest
        if (!(restObject instanceof ItemRest)) {
            return;
        }
        ItemRest itemRest = (ItemRest) restObject;

        // ensure that relatedItems is present in the response
        itemRest.initRelatedItems();

        // item 1
        Item item1 = getItem(context, itemRest.getId());
        if (item1 == null) {
            return;
        }

        // get checkRelatedItem values from url
        String[] inputs = servletRequest.getParameterValues(PARAM_NAME);
        if (inputs == null) {
            return;
        }

        for (String input : inputs) {
            // item 2
            Item item2 = getItemFromInput(context, input);
            if (item2 == null) {
                continue;
            }

            // relationship type
            String relationshipTypeName = getRelationshipTypeNameFromInput(input);
            RelationshipType relationshipType = getRelationship(context, relationshipTypeName, item1, item2);

            // count related items
            int count;
            if (relationshipType == null) {
                count = relationshipService.countByRelatedItems(context, item1, item2);
            } else {
                boolean isLeft = StringUtils.equals(relationshipType.getLeftwardType(), relationshipTypeName);
                count = relationshipService.countByRelatedItems(context, item1, item2, relationshipType, isLeft);
            }
            if (count <= 0) {
                continue;
            }

            // item 2 is related to item 1
            itemRest.addRelatedItem(item2.getID());
        }
    }

    protected Item getItem(Context context, String uuidStr) throws SQLException {
        if (StringUtils.isBlank(uuidStr)) {
            return null;
        }

        UUID item1Uuid = UUIDUtils.fromString(uuidStr);
        if (item1Uuid == null) {
            return null;
        }

        return itemService.find(context, item1Uuid);
    }

    protected Item getItemFromInput(Context context, String input) throws SQLException {
        if (StringUtils.isBlank(input)) {
            return null;
        }

        String uuidStr = input;
        if (StringUtils.contains(input, RELATIONSHIP_UUID_SEPARATOR)) {
            String[] fragments = StringUtils.split(input, RELATIONSHIP_UUID_SEPARATOR, 2);
            uuidStr = fragments[1];
        }

        return getItem(context, uuidStr);
    }

    protected String getRelationshipTypeNameFromInput(String input) {
        if (StringUtils.isBlank(input) || !StringUtils.contains(input, RELATIONSHIP_UUID_SEPARATOR)) {
            return null;
        }

        return StringUtils.split(input, RELATIONSHIP_UUID_SEPARATOR, 2)[0];
    }

    protected RelationshipType getRelationship(
        Context context, String relationshipTypeName, Item item1, Item item2
    ) throws SQLException {
        if (StringUtils.isBlank(relationshipTypeName)) {
            return null;
        }

        String item1EntityType = getEntityType(item1);
        if (item1EntityType == null) {
            return null;
        }

        String item2EntityType = getEntityType(item2);
        if (item2EntityType == null) {
            return null;
        }

        List<RelationshipType> relationshipTypes = relationshipTypeService
            .findByLeftwardOrRightwardTypeName(context, relationshipTypeName).stream()
            .filter(relationshipType -> {
                if (
                    StringUtils.equals(relationshipType.getLeftwardType(), relationshipTypeName) &&
                    StringUtils.equals(relationshipType.getLeftType().getLabel(), item1EntityType) &&
                    StringUtils.equals(relationshipType.getRightType().getLabel(), item2EntityType)
                ) {
                    return true;
                }

                if (
                    StringUtils.equals(relationshipType.getRightwardType(), relationshipTypeName) &&
                    StringUtils.equals(relationshipType.getLeftType().getLabel(), item2EntityType) &&
                    StringUtils.equals(relationshipType.getRightType().getLabel(), item1EntityType)
                ) {
                    return true;
                }

                return false;
            })
            .collect(Collectors.toList());

        if (relationshipTypes.size() <= 0) {
            return null;
        }

        if (relationshipTypes.size() > 1) {
            log.warn("found multiple relationship types");
            return null;
        }

        return relationshipTypes.get(0);
    }

    protected String getEntityType(Item item) {
        List<MetadataValue> mdvs = itemService.getMetadata(
            item, "dspace", "entity", "type", Item.ANY, false
        );

        if (mdvs.isEmpty()) {
            return null;
        }

        return mdvs.get(0).getValue();
    }

}
