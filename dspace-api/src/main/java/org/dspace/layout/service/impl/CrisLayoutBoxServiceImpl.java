/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.discovery.configuration.DiscoveryConfigurationUtilsService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxConfiguration;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.dao.CrisLayoutBoxDAO;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.versioning.service.VersionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of service to manage Boxes component of layout
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 */
public class CrisLayoutBoxServiceImpl implements CrisLayoutBoxService {

    @Autowired
    private CrisLayoutBoxDAO dao;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private CrisLayoutBoxAccessService crisLayoutBoxAccessService;

    @Autowired
    private DiscoveryConfigurationUtilsService searchConfigurationUtilsService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private VersionHistoryService versionHistoryService;

    public CrisLayoutBoxServiceImpl() {
    }

    @Override
    public CrisLayoutBox create(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to create a Box");
        }
        return dao.create(context, new CrisLayoutBox());
    }

    @Override
    public CrisLayoutBox find(Context context, int id) throws SQLException {
        return dao.findByID(context, CrisLayoutBox.class, id);
    }

    @Override
    public void update(Context context, CrisLayoutBox boxList) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(boxList));
    }

    @Override
    public void update(Context context, List<CrisLayoutBox> boxList) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to update a Box");
        }
        if (CollectionUtils.isNotEmpty(boxList)) {
            for (CrisLayoutBox box : boxList) {
                dao.save(context, box);
            }
        }
    }

    @Override
    public void delete(Context context, CrisLayoutBox box) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to delete a Box");
        }
        dao.delete(context, box);
    }

    @Override
    public CrisLayoutBox create(Context context, CrisLayoutBox box) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to create a Box");
        }
        return dao.create(context, box);
    }

    @Override
    public CrisLayoutBox create(Context context, EntityType eType, String boxType, boolean collapsed,
                                boolean minor) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to create a Box");
        }
        CrisLayoutBox box = new CrisLayoutBox();
        box.setEntitytype(eType);
        box.setCollapsed(collapsed);
        box.setMinor(minor);
        box.setType(boxType);
        return dao.create(context, box);

    }

    @Override
    public List<CrisLayoutBox> findByEntityType(Context context, String entityType,
        Integer limit, Integer offset) throws SQLException {
        return dao.findByEntityType(context, entityType, limit, offset);
    }

    @Override
    public boolean hasContent(Context context, CrisLayoutBox box, Item item) {
        String boxType = box.getType();

        if (StringUtils.isEmpty(boxType)) {
            return hasMetadataBoxContent(context, box, item);
        }

        switch (boxType.toUpperCase()) {
            case "RELATION":
                return hasRelationBoxContent(context, box, item);
            case "COLLECTIONS":
                return isOwningCollectionPresent(item);
            case "IIIFVIEWER":
                return isIiifEnabled(item);
            case "VERSIONING":
                return hasVersioningBox(context, item);
            case "NETWORKLAB":
                return isNetworkLabEnabled(item);
            case "METADATA":
            default:
                return hasMetadataBoxContent(context, box, item);
        }

    }

    private boolean hasVersioningBox(Context context, Item item) {
        try {
            return versionHistoryService.hasVersionHistory(context, item);
        } catch (SQLException e) {
            return false;
        }
    }
    @Override
    public boolean hasAccess(Context context, CrisLayoutBox box, Item item) {
        return crisLayoutBoxAccessService.hasAccess(context, context.getCurrentUser(), box, item);
    }

    @Override
    public CrisLayoutBoxConfiguration getConfiguration(CrisLayoutBox box) {
        return new CrisLayoutBoxConfiguration(box);
    }

    private boolean hasMetadataBoxContent(Context context, CrisLayoutBox box, Item item) {

        List<CrisLayoutField> boxFields = box.getLayoutFields();
        if (CollectionUtils.isEmpty(boxFields)) {
            return false;
        }

        for (CrisLayoutField field : boxFields) {

            if (field.isMetadataField() && isMetadataFieldPresent(item, field.getMetadataField())) {
                return true;
            }

            if (field.isBitstreamField() && isBitstreamPresent(context, item, (CrisLayoutFieldBitstream) field)) {
                return true;
            }

        }

        return false;
    }

    private boolean isMetadataFieldPresent(DSpaceObject item, MetadataField metadataField) {
        return item.getMetadata().stream()
            .anyMatch(metadataValue -> Objects.equals(metadataField, metadataValue.getMetadataField()));
    }

    private boolean isBitstreamPresent(Context context, Item item, CrisLayoutFieldBitstream field) {
        Map<String, String> filters = new HashMap<>();
        if (field.getMetadataField() != null && StringUtils.isNotBlank(field.getMetadataValue())) {
            filters.put(field.getMetadataField().toString('.'), field.getMetadataValue());
        }
        try {
            return bitstreamService.findShowableByItem(context, item.getID(), field.getBundle(), filters).size() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasRelationBoxContent(Context context, CrisLayoutBox box, Item item) {
        Iterator<Item> relatedItems = searchConfigurationUtilsService.findByRelation(context, item, box.getShortname());
        return relatedItems.hasNext();
    }

    private boolean isIiifEnabled(Item item) {
        return BooleanUtils.toBoolean(itemService.getMetadataFirstValue(item,
            new MetadataFieldName("dspace.iiif.enabled"), Item.ANY));
    }

    private boolean isNetworkLabEnabled(Item item) {
        return BooleanUtils.toBoolean(itemService.getMetadataFirstValue(item,
                new MetadataFieldName("dspace.networklab.enabled"), Item.ANY));
    }

    private boolean isOwningCollectionPresent(Item item) {
        return Objects.nonNull(item.getOwningCollection());
    }

    private boolean currentUserIsNotAllowedToReadItem(Context context, Item item) {
        try {
            return !authorizeService.authorizeActionBoolean(context, item, Constants.READ);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public List<CrisLayoutBox> findByEntityAndType(Context context,String entity, String type) {

        try {
            return dao.findByEntityAndType(context, entity, type);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
