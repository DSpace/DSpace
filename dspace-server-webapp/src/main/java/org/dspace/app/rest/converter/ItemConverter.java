/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MetadataValueList;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.EPersonAuthority;
import org.dspace.content.authority.GroupAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Item in the DSpace API data model and the
 * REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ItemConverter
        extends DSpaceObjectConverter<Item, ItemRest>
        implements IndexableObjectConverter<Item, ItemRest> {

    @Autowired
    private ItemService itemService;

    @Autowired
    private CrisLayoutBoxService crisLayoutBoxService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ChoiceAuthorityService cas;

    @Autowired
    GroupService groupService;

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemConverter.class);

    @Override
    public ItemRest convert(Item obj, Projection projection) {
        ItemRest item = super.convert(obj, projection);
        item.setInArchive(obj.isArchived());
        item.setDiscoverable(obj.isDiscoverable());
        item.setWithdrawn(obj.isWithdrawn());
        item.setLastModified(obj.getLastModified());

        return item;
    }

    /**
     * Retrieves the metadata list filtered according to the hidden metadata configuration
     * When the context is null, it will return the metadatalist as for an anonymous user
     * Overrides the parent method to include virtual metadata
     * @param context The context
     * @param obj     The object of which the filtered metadata will be retrieved
     * @return A list of object metadata (including virtual metadata) filtered based on the the hidden metadata
     * configuration
     */
    @Override
    public MetadataValueList getPermissionFilteredMetadata(Context context, Item obj) {
        List<MetadataValue> fullList = itemService.getMetadata(obj, Item.ANY, Item.ANY, Item.ANY, Item.ANY, true);
        List<MetadataValue> returnList = new LinkedList<>();
        try {
            for (MetadataValue metadataValue : fullList) {
                MetadataField metadataField = metadataValue.getMetadataField();
                if (checkMetadataFieldVisibility(context, obj, metadataField)) {
                    returnList.add(metadataValue);
                }
            }
        } catch (SQLException e) {
            log.error("Error filtering item metadata based on permissions", e);
        }
        return new MetadataValueList(returnList);
    }

    public boolean checkMetadataFieldVisibility(Context context, Item item, MetadataField metadataField)
            throws SQLException {
        String entityType = itemService.getMetadataFirstValue(item, MetadataSchemaEnum.RELATIONSHIP.getName(),
                                                              "type", null, Item.ANY);
        List<CrisLayoutBox> boxes = crisLayoutBoxService.findEntityBoxes(context, entityType, 1000, 0);
        List<MetadataField> allPublicMetadata = getPublicMetadata(boxes);
        EPerson currentUser = context.getCurrentUser();
        if (isPublicMetadataField(metadataField, allPublicMetadata)) {
            return true;
        } else if (currentUser != null) {
            List<CrisLayoutBox> boxesWithMetadataFieldExcludedPublic = getBoxesWithMetadataFieldExcludedPublic(
                    metadataField, boxes);
            for (CrisLayoutBox box : boxesWithMetadataFieldExcludedPublic) {
                if (grantAccess(context, currentUser, box, item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean grantAccess(Context context, EPerson currentUser, CrisLayoutBox box, Item item)
            throws SQLException {
        boolean grantAcces;
        int layoutSecurity = box.getSecurity();
        switch (layoutSecurity) {
            case 1 : grantAcces = authorizeService.isAdmin(context);
            break;
            case 2 : grantAcces = isOwner(currentUser, item);
            break;
            case 3 : grantAcces = (isOwner(currentUser, item) || authorizeService.isAdmin(context));
            break;
            case 4 : grantAcces = customDataGrantAccess(context, currentUser, box, item);
            break;
            default: grantAcces = false;
        }
        return grantAcces;
    }

    private boolean customDataGrantAccess(Context context, EPerson currentUser, CrisLayoutBox box, Item item) {
        List<MetadataField> metadataEpersons = new LinkedList<MetadataField>();
        List<MetadataField> metadataGroups = new LinkedList<MetadataField>();
        Set<MetadataField> metadataSecurityFields = box.getMetadataSecurityFields();
        for (MetadataField metadataField : metadataSecurityFields) {
            String authorityName = cas.getChoiceAuthorityName(metadataField.getMetadataSchema().getName(),
                                   metadataField.getElement(),metadataField.getQualifier(), null);
            ChoiceAuthority source = cas.getChoiceAuthorityByAuthorityName(authorityName);
            if (source instanceof EPersonAuthority) {
                metadataEpersons.add(metadataField);
            }
            if (source instanceof GroupAuthority) {
                metadataGroups.add(metadataField);
            }
        }
        return (grantAccesToEperson(currentUser, metadataEpersons, item) ||
                grantAccesToGroup(context, currentUser, metadataGroups, item));
    }

    private boolean grantAccesToGroup(Context context, EPerson currentUser, List<MetadataField> metadataGroups,
            Item item) {
        for (MetadataField field : metadataGroups) {
            List<MetadataValue> values = itemService.getMetadata(item, field.getMetadataSchema().getName(),
                    field.getElement(), field.getQualifier(), Item.ANY, true);
            for (MetadataValue value : values) {
                UUID uuidGroup = UUID.fromString(value.getAuthority());
                try {
                    Group group = groupService.find(context, uuidGroup);
                    if (groupService.isMember(context, currentUser, group)) {
                        return true;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        return false;
    }

    private boolean grantAccesToEperson(EPerson currentUser, List<MetadataField> metadataEpersons, Item item) {
        for (MetadataField field : metadataEpersons) {
            List<MetadataValue> values = itemService.getMetadata(item, field.getMetadataSchema().getName(),
                                                     field.getElement(), field.getQualifier(), Item.ANY, true);
            for (MetadataValue value : values) {
                if (value.getAuthority().equals(currentUser.getID().toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOwner(EPerson currentUser, Item item) {
        String uuidOwner = itemService.getMetadataFirstValue(item, "cris", "owner", null, Item.ANY);
        return (uuidOwner != null && uuidOwner.equals(currentUser.getID().toString()));
    }

    private List<CrisLayoutBox> getBoxesWithMetadataFieldExcludedPublic(MetadataField metadataField,
            List<CrisLayoutBox> boxes) {
        List<CrisLayoutBox> boxesWithMetadataField = new LinkedList<CrisLayoutBox>();
        for (CrisLayoutBox box : boxes) {
            List<CrisLayoutField> crisLayoutFields = box.getLayoutFields();
            for (CrisLayoutField field : crisLayoutFields) {
                if (field.getMetadataField().equals(metadataField)
                    && box.getSecurity() != LayoutSecurity.PUBLIC.getValue()) {
                    boxesWithMetadataField.add(box);
                }
            }
        }
        return boxesWithMetadataField;
    }

    private boolean isPublicMetadataField(MetadataField metadataField, List<MetadataField> allPublicMetadata) {
        for (MetadataField publicField : allPublicMetadata) {
            if (publicField.equals(metadataField)) {
                return true;
            }
        }
        return false;
    }

    private List<MetadataField> getPublicMetadata(List<CrisLayoutBox> boxes) {
        List<MetadataField> publicMetadata = new ArrayList<MetadataField>();
        for (CrisLayoutBox box : boxes) {
            if (box.getSecurity() == LayoutSecurity.PUBLIC.getValue()) {
                List<CrisLayoutField> crisLayoutFields = box.getLayoutFields();
                for (CrisLayoutField field : crisLayoutFields) {
                    publicMetadata.add(field.getMetadataField());
                }
            }
        }
        return publicMetadata;
    }

    @Override
    protected ItemRest newInstance() {
        return new ItemRest();
    }

    @Override
    public Class<Item> getModelClass() {
        return Item.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof Item;
    }
}
