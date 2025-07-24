/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.action;

import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.qaevent.QualityAssuranceAction;
import org.dspace.qaevent.service.dto.OpenaireMessageDTO;
import org.dspace.qaevent.service.dto.QAMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link QualityAssuranceAction} that handle the relationship between the
 * item to correct and a related item.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QAEntityOpenaireMetadataAction implements QualityAssuranceAction {
    private String relation;
    private String entityType;
    private Map<String, String> entityMetadata;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private CollectionService collectionService;

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String[] splitMetadata(String metadata) {
        String[] result = new String[3];
        String[] split = metadata.split("\\.");
        result[0] = split[0];
        result[1] = split[1];
        if (split.length == 3) {
            result[2] = split[2];
        }
        return result;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Map<String, String> getEntityMetadata() {
        return entityMetadata;
    }

    public void setEntityMetadata(Map<String, String> entityMetadata) {
        this.entityMetadata = entityMetadata;
    }

    @Override
    public void applyCorrection(Context context, Item item, Item relatedItem, QAMessageDTO message) {
        try {
            if (relatedItem != null) {
                link(context, item, relatedItem);
            } else {

                Collection collection = collectionService.retrieveCollectionWithSubmitByEntityType(context,
                    item, entityType);
                if (collection == null) {
                    throw new IllegalStateException("No collection found by entity type: " + collection);
                }

                WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, true);
                relatedItem = workspaceItem.getItem();

                for (String key : entityMetadata.keySet()) {
                    String value = getValue(message, key);
                    if (StringUtils.isNotBlank(value)) {
                        String[] targetMetadata = splitMetadata(entityMetadata.get(key));
                        itemService.addMetadata(context, relatedItem, targetMetadata[0], targetMetadata[1],
                                targetMetadata[2], null, value);
                    }
                }
                installItemService.installItem(context, workspaceItem);
                itemService.update(context, relatedItem);
                link(context, item, relatedItem);
            }
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a new relationship between the two given item, based on the configured
     * relation.
     */
    private void link(Context context, Item item, Item relatedItem) throws SQLException, AuthorizeException {
        EntityType project = entityTypeService.findByEntityType(context, entityType);
        RelationshipType relType = relationshipTypeService.findByEntityType(context, project).stream()
                .filter(r -> StringUtils.equals(r.getRightwardType(), relation)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No relationshipType named " + relation
                        + " was found for the entity type " + entityType
                        + ". A proper configuration is required to use the QAEntitiyMetadataAction."
                        + " If you don't manage funding in your repository please skip this topic in"
                        + " the qaevents.cfg"));
        // Create the relationship
        relationshipService.create(context, item, relatedItem, relType, -1, -1);
    }

    private String getValue(QAMessageDTO message, String key) {
        if (!(message instanceof OpenaireMessageDTO)) {
            return null;
        }

        OpenaireMessageDTO openaireMessage = (OpenaireMessageDTO) message;

        if (StringUtils.equals(key, "acronym")) {
            return openaireMessage.getAcronym();
        } else if (StringUtils.equals(key, "code")) {
            return openaireMessage.getCode();
        } else if (StringUtils.equals(key, "funder")) {
            return openaireMessage.getFunder();
        } else if (StringUtils.equals(key, "fundingProgram")) {
            return openaireMessage.getFundingProgram();
        } else if (StringUtils.equals(key, "jurisdiction")) {
            return openaireMessage.getJurisdiction();
        } else if (StringUtils.equals(key, "openaireId")) {
            return openaireMessage.getOpenaireId();
        } else if (StringUtils.equals(key, "title")) {
            return openaireMessage.getTitle();
        }

        return null;
    }
}
