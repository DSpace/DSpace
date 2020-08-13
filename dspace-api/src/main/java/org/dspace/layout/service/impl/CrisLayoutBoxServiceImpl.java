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
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
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
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of service to manage Boxes component of layout
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
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
        update(context,Collections.singletonList(boxList));
    }

    @Override
    public void update(Context context, List<CrisLayoutBox> boxList) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to update a Box");
        }
        if (CollectionUtils.isNotEmpty(boxList)) {
            for (CrisLayoutBox box: boxList) {
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
    public CrisLayoutBox create(Context context, EntityType eType, String boxType, boolean collapsed, int priority,
            boolean minor) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to create a Box");
        }
        CrisLayoutBox box = new CrisLayoutBox();
        box.setEntitytype(eType);
        box.setCollapsed(collapsed);
//        box.setPriority(priority);
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
        Item item = itemService.find(context, itemUuid);
        String entityType = "";
        if ( item != null ) {
            entityType = itemService.getMetadata(item, "relationship.type");
        }
        List<CrisLayoutBox> boxes = dao.findByEntityType(context, entityType, tabId, null, null);
        // resBoxes contains only the box with available data for the associated item
        List<CrisLayoutBox> resBoxes = new ArrayList<>();
        if (boxes != null && !boxes.isEmpty()) {
            List<MetadataValue> itemMetadata = item.getMetadata();
            if ( itemMetadata != null && !itemMetadata.isEmpty() ) {
                for (CrisLayoutBox box: boxes) {
                    if (hasContent(box, itemMetadata) ) {
                        resBoxes.add(box);
                    }
                }
            }
        }
        return resBoxes;
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
    public boolean hasContent(CrisLayoutBox box, List<MetadataValue> values) {
        boolean found = false;
        List<CrisLayoutField> boxFields = box.getLayoutFields();
        // Check if the box type is relation
        boolean isRelationBox = box.getType() != null ?
                box.getType().equalsIgnoreCase("relation") : false;
        if (isRelationBox) {
            // The relation box has no associated content
            found = true;
        } else if ( boxFields != null && !boxFields.isEmpty() ) {
            for (MetadataValue value: values) {
                for (CrisLayoutField field: boxFields) {
                    if (value.getMetadataField().equals(field.getMetadataField())) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }
        return found;
    }

    @Override
    public CrisLayoutBoxConfiguration getConfiguration(Context context, CrisLayoutBox box) {
        return new CrisLayoutBoxConfiguration(box);
    }
}
