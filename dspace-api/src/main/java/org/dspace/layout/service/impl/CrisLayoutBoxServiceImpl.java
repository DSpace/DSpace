/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxConfiguration;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.dao.CrisLayoutBoxDAO;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.layout.service.CrisLayoutBoxService;
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
    private ItemService itemService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private MetadataExposureService metadataExposureService;

    @Autowired
    private CrisLayoutBoxAccessService crisLayoutBoxAccessService;

    //constructor with all fields injected, used for test purposes (mock injection)
    CrisLayoutBoxServiceImpl(CrisLayoutBoxDAO dao, ItemService itemService, AuthorizeService authorizeService,
                             EntityTypeService entityTypeService,
                             CrisLayoutBoxAccessService crisLayoutBoxAccessService) {
        this.dao = dao;
        this.itemService = itemService;
        this.authorizeService = authorizeService;
        this.entityTypeService = entityTypeService;
        this.crisLayoutBoxAccessService = crisLayoutBoxAccessService;
    }

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

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutBoxService#findByTabId(org.dspace.core.Context, java.lang.Integer)
     */
    @Override
    public List<CrisLayoutBox> findByTabId(Context context, Integer tabId) throws SQLException {
        return dao.findByTabId(context, tabId);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutBoxService#findByTabId
     * (org.dspace.core.Context, java.lang.Integer, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<CrisLayoutBox> findByTabId(Context context, Integer tabId, Integer limit, Integer offset)
        throws SQLException {
        return dao.findByTabId(context, tabId, limit, offset);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutBoxService#countTotalBoxesInTab
     * (org.dspace.core.Context, java.lang.Integer)
     */
    @Override
    public Long countTotalBoxesInTab(Context context, Integer tabId) throws SQLException {
        return dao.countTotalBoxesInTab(context, tabId);
    }

    @Override
    public Long countTotalEntityBoxes(Context context, String entityType) throws SQLException {
        return dao.countTotalEntityBoxes(context, entityType);
    }

    @Override
    public List<CrisLayoutBox> findEntityBoxes(Context context, String entityType, Integer limit, Integer offset)
        throws SQLException {
        return dao.findByEntityType(context, entityType, null, limit, offset);
    }

    @Override
    public List<MetadataField> getMetadataField(Context context, Integer boxId, Integer limit, Integer offset)
        throws SQLException {
        return dao.getMetadataField(context, boxId, limit, offset);
    }

    @Override
    public Long totalMetadataField(Context context, Integer boxId) throws SQLException {
        return dao.totalMetadatafield(context, boxId);
    }

    @Override
    public List<CrisLayoutBox> findByItem(
        Context context, UUID itemUuid, Integer tabId) throws SQLException {
        Item item = Objects.requireNonNull(itemService.find(context, itemUuid),
                                           "The item uuid entered does not match with any item");

        String entityType = itemService.getMetadata(item, "relationship.type");

        List<CrisLayoutBox> boxes = dao.findByEntityType(context, entityType, tabId, null, null);
        if (CollectionUtils.isEmpty(boxes)) {
            return new ArrayList<>();
        }
        return Optional.ofNullable(item.getMetadata())
                       .filter(im -> !im.isEmpty())
                       .map(itemMetadata -> boxes
                           .stream()
                           .filter(b -> hasContent(context, b, itemMetadata))
                           .filter(b -> accessGranted(context, item, b))
                           .collect(Collectors.toList()))
                       .orElse(new ArrayList<>());
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutBoxService#findByShortname(org.dspace.core.Context, java.lang.String)
     */
    @Override
    public CrisLayoutBox findByShortname(Context context, String entityType, String shortname) throws SQLException {
        Integer entityId = entityTypeService.findByEntityType(context, entityType).getID();
        return dao.findByShortname(context, entityId, shortname);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutBoxService#hasContent()
     */
    @Override
    public boolean hasContent(Context context, CrisLayoutBox box, List<MetadataValue> values) {
        String boxType = box.getType();
        if (StringUtils.isEmpty(boxType)) {
            return hasMetadataBoxContent(box, values);
        }

        switch (boxType.toUpperCase()) {
            case "RELATION":
                return hasRelationBoxContent(box, values);
            case "ORCID_SYNC_SETTINGS":
            case "ORCID_SYNC_QUEUE":
                return hasOrcidSyncBoxContent(context, box, values);
            case "ORCID_AUTHORIZATIONS":
                return hasOrcidAuthorizationsBoxContent(context, box, values);
            case "METADATA":
            default:
                return hasMetadataBoxContent(box, values);
        }

    }

    @Override
    public CrisLayoutBoxConfiguration getConfiguration(Context context, CrisLayoutBox box) {
        return new CrisLayoutBoxConfiguration(box);
    }

    private boolean hasMetadataBoxContent(CrisLayoutBox box, List<MetadataValue> values) {
        List<CrisLayoutField> boxFields = box.getLayoutFields();
        if (boxFields == null || boxFields.isEmpty()) {
            return false;
        }

        for (MetadataValue value : values) {
            for (CrisLayoutField field : boxFields) {
                if (value.getMetadataField().equals(field.getMetadataField())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasRelationBoxContent(CrisLayoutBox box, List<MetadataValue> values) {
        // The relation box has no associated content
        return true;
    }

    private boolean hasOrcidSyncBoxContent(Context context, CrisLayoutBox box, List<MetadataValue> values) {
        return isOwnProfile(context, values) && values.stream()
                                                      .map(metadata -> metadata.getMetadataField().toString('.'))
                                                      .anyMatch(metadata -> metadata.equals("person.identifier.orcid"));
    }

    private boolean hasOrcidAuthorizationsBoxContent(Context context, CrisLayoutBox box, List<MetadataValue> values) {
        return isOwnProfile(context, values);
    }

    private boolean isOwnProfile(Context context, List<MetadataValue> values) {
        MetadataValue crisOwner = values.stream()
                                        .filter(
                                            metadata -> metadata.getMetadataField().toString('.').equals("cris.owner"))
                                        .findFirst()
                                        .orElse(null);

        if (crisOwner == null || crisOwner.getAuthority() == null || context.getCurrentUser() == null) {
            return false;
        }

        return crisOwner.getAuthority().equals(context.getCurrentUser().getID().toString());
    }

    // in private method so that exception can be handled and method can be invoked within a lambda
    private boolean accessGranted(final Context context, final Item item, final CrisLayoutBox box) {

        try {
            return crisLayoutBoxAccessService.hasAccess(context, context.getCurrentUser(), box, item);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
