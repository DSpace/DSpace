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
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.CrisLayoutTab2Box;
import org.dspace.layout.dao.CrisLayoutTabDAO;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.layout.service.CrisLayoutTabService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of service to manage Tabs component of layout
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutTabServiceImpl implements CrisLayoutTabService {

    @Autowired(required = true)
    private CrisLayoutTabDAO dao;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CrisLayoutBoxService boxService;

    @Override
    public CrisLayoutTab create(Context c, CrisLayoutTab tab) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(c)) {
            throw new AuthorizeException(
                "You must be an admin to create a Tab");
        }
        return dao.create(c, tab);
    }

    @Override
    public CrisLayoutTab create(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to create a Tab");
        }
        return dao.create(context, new CrisLayoutTab());
    }

    @Override
    public CrisLayoutTab find(Context context, int id) throws SQLException {
        return dao.findByID(context, CrisLayoutTab.class, id);
    }

    @Override
    public void update(Context context, CrisLayoutTab tab) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to update a Tab");
        }
        dao.save(context, tab);
    }

    @Override
    public void update(Context context, List<CrisLayoutTab> tabList) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(tabList)) {
            for (CrisLayoutTab tab: tabList) {
                update(context, tab);
            }
        }
    }

    @Override
    public void delete(Context context, CrisLayoutTab tab) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to delete a Tab");
        }
        dao.delete(context, tab);
    }

    @Override
    public CrisLayoutTab create(Context context, EntityType eType, Integer priority)
            throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to create a Tab");
        }
        CrisLayoutTab tab = new CrisLayoutTab();
        tab.setEntity(eType);
        tab.setPriority(priority);
        return dao.create(context, tab);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutTabService#findAll(org.dspace.core.Context)
     */
    @Override
    public List<CrisLayoutTab> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        return dao.findAll(context, CrisLayoutTab.class, limit, offset);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutTabService#countTotal(org.dspace.core.Context)
     */
    @Override
    public Long countTotal(Context context) throws SQLException {
        return dao.countTotal(context);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutTabService#findByEntityType(org.dspace.core.Context, java.lang.String)
     */
    @Override
    public List<CrisLayoutTab> findByEntityType(Context context, String entityType) throws SQLException {
        return dao.findByEntityType(context, entityType);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutTabService#findByEntityType
     * (org.dspace.core.Context, java.lang.String, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<CrisLayoutTab> findByEntityType(Context context, String entityType, Integer limit, Integer offset)
            throws SQLException {
        return dao.findByEntityType(context, entityType, limit, offset);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutTabService#countByEntityType(org.dspace.core.Context, java.lang.String)
     */
    @Override
    public Long countByEntityType(Context context, String entityType) throws SQLException {
        return dao.countByEntityType(context, entityType);
    }

    @Override
    public List<MetadataField> getMetadataField(Context context, Integer tabId, Integer limit, Integer offset)
            throws SQLException {
        return dao.getMetadataField(context, tabId, limit, offset);
    }

    @Override
    public Long totalMetadataField(Context context, Integer tabId) throws SQLException {
        return dao.totalMetadatafield(context, tabId);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutTabService#findByItem(org.dspace.core.Context, java.util.UUID)
     */
    @Override
    public List<CrisLayoutTab> findByItem(Context context, String itemUuid) throws SQLException {
        Item item = itemService.find(context, UUID.fromString(itemUuid));
        String entityType = "";
        if (item != null) {
            entityType = itemService.getMetadata(item, "relationship.type");
        }
        List<CrisLayoutTab> tabs = dao.findByEntityType(context, entityType);
        List<CrisLayoutTab> resTabs = new ArrayList<>();
        if (tabs != null && !tabs.isEmpty()) {
            List<MetadataValue> itemMetadata = item.getMetadata();
            if (itemMetadata != null && !itemMetadata.isEmpty() ) {
                for (CrisLayoutTab tab: tabs) {
                    List<CrisLayoutTab2Box> tab2box = tab.getTab2Box();
                    if (tab2box != null && !tab2box.isEmpty()) {
                        for (CrisLayoutTab2Box t2b: tab2box) {
                            if (boxService.hasContent(context, t2b.getBox(), itemMetadata)) {
                                resTabs.add(tab);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return resTabs;
    }

}
