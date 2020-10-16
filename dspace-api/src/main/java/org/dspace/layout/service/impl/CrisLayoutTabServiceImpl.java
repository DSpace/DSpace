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
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.dao.CrisLayoutTabDAO;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.layout.service.CrisLayoutTabAccessService;
import org.dspace.layout.service.CrisLayoutTabService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of service to manage Tabs component of layout
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
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

    @Autowired
    private  CrisLayoutBoxAccessService crisLayoutBoxAccessService;

    @Autowired
    private CrisLayoutTabAccessService crisLayoutTabAccessService;

    //constructor with all fields injected, used for test purposes (mock injection)
    public CrisLayoutTabServiceImpl(CrisLayoutTabDAO dao, AuthorizeService authorizeService,
                                    ItemService itemService, CrisLayoutBoxService boxService,
                                    CrisLayoutBoxAccessService crisLayoutBoxAccessService,
                                    CrisLayoutTabAccessService crisLayoutTabAccessService) {
        this.dao = dao;
        this.authorizeService = authorizeService;
        this.itemService = itemService;
        this.boxService = boxService;
        this.crisLayoutBoxAccessService = crisLayoutBoxAccessService;
        this.crisLayoutTabAccessService = crisLayoutTabAccessService;
    }

    public CrisLayoutTabServiceImpl() {
    }

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
        Item item = Objects.requireNonNull(itemService.find(context, UUID.fromString(itemUuid)),
                                           "The itemUuid entered does not match with any item");

        String entityType  = itemService.getMetadata(item, "relationship.type");

        List<CrisLayoutTab> tabs = dao.findByEntityType(context, entityType);
        if (CollectionUtils.isEmpty(tabs) || CollectionUtils.isEmpty(item.getMetadata())) {
            return Collections.emptyList();
        }

        return tabs.stream()
                   .filter(t -> CollectionUtils.isNotEmpty(t.getTab2Box()))
                   .filter(t -> tabGrantedAccess(context, t, item))
                   .filter(t -> hasABoxToDisplay(context, t, item))
                   .collect(Collectors.toList());
    }

    private boolean hasABoxToDisplay(Context context, CrisLayoutTab tab,
                                     Item item) {
        Predicate<CrisLayoutBox> isGranted = box -> boxGrantedAccess(context, item, box);
        Predicate<CrisLayoutBox> hasContent = box -> boxService.hasContent(context, box, item.getMetadata());
        return tab.getTab2Box().stream()
                  .map(t2b -> t2b.getBox())
                  .anyMatch(isGranted.and(hasContent));
    }

    private boolean boxGrantedAccess(Context context, Item item, CrisLayoutBox box) {
        try {
            return crisLayoutBoxAccessService.hasAccess(context,
                                                        context.getCurrentUser(), box,
                                                        item);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private boolean tabGrantedAccess(Context context, CrisLayoutTab tab, Item item) {
        try {
            return crisLayoutTabAccessService.hasAccess(context, context.getCurrentUser(),
                                                        tab, item);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
