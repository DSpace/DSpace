/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.bulkaccesscontrol.model.AccessCondition;
import org.dspace.app.bulkaccesscontrol.model.BulkAccessControlInput;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.clarin.ClarinItemService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ProvenanceServiceImpl is an implementation of ProvenanceService.
 *
 * @author Michaela Paurikova (dspace at dataquest.sk)
 */
public class ProvenanceServiceImpl implements ProvenanceService {
    private static final Logger log = LogManager.getLogger(ProvenanceServiceImpl.class);

    @Autowired
    private ItemService itemService;
    @Autowired
    private ClarinItemService clarinItemService;
    @Autowired
    private ClarinLicenseResourceMappingService clarinResourceMappingService;
    @Autowired
    private BitstreamService bitstreamService;

    private final ProvenanceMessageFormatter messageProvider = new ProvenanceMessageFormatter();

    public void setItemPolicies(Context context, Item item, BulkAccessControlInput accessControl) {
        String resPoliciesStr = extractAccessConditions(accessControl.getItem().getAccessConditions());
        if (StringUtils.isNotBlank(resPoliciesStr)) {
            String msg = messageProvider.getMessage(context, ProvenanceMessageTemplates.ACCESS_CONDITION.getTemplate(),
                    resPoliciesStr, "item", item.getID());
            try {
                addProvenanceMetadata(context, item, msg);
            } catch (SQLException | AuthorizeException e) {
                log.error("Unable to add new provenance metadata when setting item policies.", e);
            }
        }
    }

    public void removeReadPolicies(Context context, DSpaceObject dso, List<ResourcePolicy> resPolicies) {
        if (resPolicies.isEmpty()) {
            return;
        }
        String resPoliciesStr = messageProvider.getMessage(resPolicies);
        try {
            if (dso.getType() == Constants.ITEM) {
                Item item = (Item) dso;
                String msg = messageProvider.getMessage(context,
                        ProvenanceMessageTemplates.RESOURCE_POLICIES_REMOVED.getTemplate(),
                        resPoliciesStr.isEmpty() ? "empty" : resPoliciesStr, "item", item.getID());
                addProvenanceMetadata(context, item, msg);
            } else if (dso.getType() == Constants.BITSTREAM) {
                Bitstream bitstream = (Bitstream) dso;
                Item item = findItemByBitstream(context, bitstream);
                if (Objects.nonNull(item)) {
                    String msg = messageProvider.getMessage(context,
                            ProvenanceMessageTemplates.RESOURCE_POLICIES_REMOVED.getTemplate(),
                            resPoliciesStr.isEmpty() ? "empty" : resPoliciesStr, "bitstream", bitstream.getID());
                    addProvenanceMetadata(context, item, msg);
                }
            }
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to remove read policies from the DSpace object.", e);
        }
    }

    public void setBitstreamPolicies(Context context, Bitstream bitstream, Item item,
                                     BulkAccessControlInput accessControl) {
        String accConditionsStr = extractAccessConditions(accessControl.getBitstream().getAccessConditions());
        if (StringUtils.isNotBlank(accConditionsStr)) {
            String msg = messageProvider.getMessage(context, ProvenanceMessageTemplates.ACCESS_CONDITION.getTemplate(),
                    accConditionsStr, "bitstream", bitstream.getID());
            try {
                addProvenanceMetadata(context, item, msg);
            } catch (SQLException | AuthorizeException e) {
                log.error("Unable to add new provenance metadata when setting bitstream policies.", e);
            }
        }
    }

    public void updateLicense(Context context, Item item, boolean newLicense) {
        String oldLicense = null;

        try {
            oldLicense = findLicenseInBundles(item, Constants.LICENSE_BUNDLE_NAME, oldLicense, context);
            if (oldLicense == null) {
                oldLicense = findLicenseInBundles(item, Constants.CONTENT_BUNDLE_NAME, oldLicense, context);
            }

            String msg = messageProvider.getMessage(context, ProvenanceMessageTemplates.EDIT_LICENSE.getTemplate(),
                    item, Objects.isNull(oldLicense) ? "empty" : oldLicense,
                    !newLicense ? "removed" : Objects.isNull(oldLicense) ? "added" : "updated");
            addProvenanceMetadata(context, item, msg);
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when editing Item's license.", e);
        }

    }

    public void moveItem(Context context, Item item, Collection collection) {
        try {
            String msg = messageProvider.getMessage(context, ProvenanceMessageTemplates.MOVE_ITEM.getTemplate(),
                    item, collection.getID());
            // Update item in DB
            // Because a user can move an item without authorization turn off authorization
            context.turnOffAuthorisationSystem();
            addProvenanceMetadata(context, item, msg);
            context.restoreAuthSystemState();
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when moving an item to a different collection.",
                    e);
        }
    }

    public void mappedItem(Context context, Item item, Collection collection) {
        String msg = messageProvider.getMessage(context, ProvenanceMessageTemplates.MAPPED_ITEM.getTemplate(),
                collection.getID());
        try {
            addProvenanceMetadata(context, item, msg);
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when mapping an item into a collection.", e);
        }
    }

    public void deletedItemFromMapped(Context context, Item item, Collection collection) {
        String msg = messageProvider.getMessage(context,
                ProvenanceMessageTemplates.DELETED_ITEM_FROM_MAPPED.getTemplate(), collection.getID());
        try {
            addProvenanceMetadata(context, item, msg);
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when deleting an item from a mapped collection.",
                    e);
        }
    }

    public void deleteBitstream(Context context, Bitstream bitstream, Item item) {
        try {
            if (Objects.nonNull(item)) {
                String msg = messageProvider.getMessage(context,
                        ProvenanceMessageTemplates.EDIT_BITSTREAM.getTemplate(), item, item.getID(),
                        messageProvider.getMessage(bitstream));
                addProvenanceMetadata(context, item, msg);
            }
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when deleting a bitstream.", e);
        }
    }

    public void addMetadata(Context context, DSpaceObject dso, MetadataField metadataField) {
        try {
            if (Constants.ITEM == dso.getType()) {
                String msg = messageProvider.getMessage(context, ProvenanceMessageTemplates.ITEM_METADATA.getTemplate(),
                        messageProvider.getMetadataField(metadataField), "added");
                addProvenanceMetadata(context, (Item) dso, msg);
            }

            if (dso.getType() == Constants.BITSTREAM) {
                Bitstream bitstream = (Bitstream) dso;
                Item item = findItemByBitstream(context, bitstream);
                if (Objects.nonNull(item)) {
                    String msg = messageProvider.getMessage(context,
                            ProvenanceMessageTemplates.BITSTREAM_METADATA.getTemplate(), item,
                            messageProvider.getMetadataField(metadataField), "added by",
                            messageProvider.getMessage(bitstream));
                    addProvenanceMetadata(context, item, msg);
                }
            }
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when adding metadata to a DSpace object.", e);
        }
    }

    public void removeMetadata(Context context, DSpaceObject dso, String schema, String element, String qualifier) {
        if (dso.getType() != Constants.BITSTREAM) {
            return;
        }
        MetadataField oldMtdKey = null;
        String oldMtdValue = null;
        List<MetadataValue> mtd = bitstreamService.getMetadata((Bitstream) dso, schema, element, qualifier, Item.ANY);
        if (CollectionUtils.isEmpty(mtd)) {
            // Do not add any provenance message when there are no metadata to remove
            return;
        }
        oldMtdKey = mtd.get(0).getMetadataField();
        oldMtdValue = mtd.get(0).getValue();
        Bitstream bitstream = (Bitstream) dso;
        try {
            Item item = findItemByBitstream(context, bitstream);
            if (Objects.nonNull(item)) {
                String msg = messageProvider.getMessage(context,
                        ProvenanceMessageTemplates.BITSTREAM_METADATA.getTemplate(), item,
                        messageProvider.getMetadata(messageProvider.getMetadataField(oldMtdKey), oldMtdValue),
                        "deleted from", messageProvider.getMessage(bitstream));
                addProvenanceMetadata(context, item, msg);
            }
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when removing metadata from a dso.", e);
        }

    }

    public void removeMetadataAtIndex(Context context, DSpaceObject dso, List<MetadataValue> metadataValues,
                                      int indexInt) {
        if (dso.getType() != Constants.ITEM) {
            return;
        }
        // Remember removed mtd
        String oldMtdKey = messageProvider.getMetadataField(metadataValues.get(indexInt).getMetadataField());
        String oldMtdValue = metadataValues.get(indexInt).getValue();
        try {
            String msg = messageProvider.getMessage(context, ProvenanceMessageTemplates.ITEM_METADATA.getTemplate(),
                    (Item) dso, messageProvider.getMetadata(oldMtdKey, oldMtdValue), "deleted");
            addProvenanceMetadata(context, (Item) dso, msg);
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when removing metadata at a specific index " +
                    "from a dso", e);
        }
    }

    public void replaceMetadata(Context context, DSpaceObject dso, MetadataField metadataField, String oldMtdVal) {
        if (dso.getType() != Constants.ITEM) {
            return;
        }
        try {
            String msg = messageProvider.getMessage(context, ProvenanceMessageTemplates.ITEM_METADATA.getTemplate(),
                    (Item) dso,messageProvider.getMetadata(messageProvider.getMetadataField(metadataField),
                            oldMtdVal), "updated");
            addProvenanceMetadata(context, (Item) dso, msg);
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when replacing metadata in a dso.", e);
        }

    }

    public void replaceMetadataSingle(Context context, DSpaceObject dso, MetadataField metadataField,
                                      String oldMtdVal) {
        if (dso.getType() != Constants.BITSTREAM) {
            return;
        }

        Bitstream bitstream = (Bitstream) dso;
        try {
            Item item = findItemByBitstream(context, bitstream);
            if (Objects.nonNull(item)) {
                String msg = messageProvider.getMessage(context,
                        ProvenanceMessageTemplates.ITEM_REPLACE_SINGLE_METADATA.getTemplate(), item,
                        messageProvider.getMessage(bitstream),
                        messageProvider.getMetadata(messageProvider.getMetadataField(metadataField), oldMtdVal));
                addProvenanceMetadata(context, item, msg);;
            }
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when replacing metadata in a item.", e);
        }
    }

    public void makeDiscoverable(Context context, Item item, boolean discoverable) {
        try {
            String msg = messageProvider.getMessage(context, ProvenanceMessageTemplates.DISCOVERABLE.getTemplate(),
                    item, discoverable ? "" : "non-") + messageProvider.getMessage(item);
            addProvenanceMetadata(context, item, msg);
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when making an item discoverable.", e);
        }
    }

    public void uploadBitstream(Context context, Bundle bundle) {
        Item item = bundle.getItems().get(0);
        try {
            String msg = messageProvider.getMessage(context, ProvenanceMessageTemplates.BUNDLE_ADDED.getTemplate(),
                    item, bundle.getID());
            addProvenanceMetadata(context,item, msg);
            itemService.update(context, item);
        } catch (SQLException | AuthorizeException e) {
            log.error("Unable to add new provenance metadata when updating an item's bitstream.", e);
        }
    }

    private void addProvenanceMetadata(Context context, Item item, String msg)
            throws SQLException, AuthorizeException {
        itemService.addMetadata(context, item, MetadataSchemaEnum.DC.getName(),
                "description", "provenance", "en", msg);
        itemService.update(context, item);
    }

    private String extractAccessConditions(List<AccessCondition> accessConditions) {
        return accessConditions.stream()
                .map(AccessCondition::getName)
                .collect(Collectors.joining(";"));
    }

    public Item findItemByBitstream(Context context, Bitstream bitstream) {
        List<Item> items = null;
        try {
            items = clarinItemService.findByBitstreamUUID(context, bitstream.getID());
        } catch (SQLException e) {
            log.error("Unable to find item by bitstream (" + bitstream.getID() + " ).", e);
            return null;
        }
        if (items.isEmpty()) {
            log.warn("Bitstream (" + bitstream.getID() + ") is not assigned to any item.");
            return null;
        }
        return items.get(0);
    }

    private String findLicenseInBundles(Item item, String bundleName, String currentLicense, Context context)
            throws SQLException {
        List<Bundle> bundles = item.getBundles(bundleName);
        for (Bundle clarinBundle : bundles) {
            List<Bitstream> bitstreamList = clarinBundle.getBitstreams();
            for (Bitstream bundleBitstream : bitstreamList) {
                if (Objects.isNull(currentLicense)) {
                    List<ClarinLicenseResourceMapping> mappings =
                            this.clarinResourceMappingService.findByBitstreamUUID(context, bundleBitstream.getID());
                    if (CollectionUtils.isNotEmpty(mappings)) {
                        return mappings.get(0).getLicense().getName();
                    }
                }
            }
        }
        return currentLicense;
    }
}
