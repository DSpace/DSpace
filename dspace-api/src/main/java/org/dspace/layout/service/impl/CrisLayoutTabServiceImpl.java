/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import static org.dspace.util.FunctionalUtils.throwingMapperWrapper;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.dao.CrisLayoutTabDAO;
import org.dspace.layout.service.CrisLayoutTabAccessService;
import org.dspace.layout.service.CrisLayoutTabService;
import org.dspace.services.ConfigurationService;
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
    private ConfigurationService configurationService;

    private SubmissionConfigReader submissionConfigReader;

    @Autowired
    CrisLayoutTabAccessService crisLayoutTabAccessService;

    @PostConstruct
    private void setup() throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
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
    public CrisLayoutTab findAndEagerlyFetch(Context context, Integer id) throws SQLException {
        return dao.findAndEagerlyFetchBoxes(context, id);
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

    @Override
    public List<CrisLayoutTab> findAll(Context context) throws SQLException {
        return dao.findAll(context, CrisLayoutTab.class);
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
    public List<CrisLayoutTab> findByEntityType(Context context, String entityType, String customFilter)
        throws SQLException {
        return dao.findByEntityTypeAndEagerlyFetchBoxes(context, entityType, customFilter);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.CrisLayoutTabService#findByEntityType
     * (org.dspace.core.Context, java.lang.String, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<CrisLayoutTab> findByEntityType(Context context, String entityType, String customFilter, Integer limit,
                                                Integer offset) throws SQLException {
        return dao.findByEntityTypeAndEagerlyFetchBoxes(context, entityType, customFilter, limit, offset);
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

    @Override
    public List<CrisLayoutTab> findByItem(Context context, String itemUuid) throws SQLException {
        Item item = Objects.requireNonNull(itemService.find(context, UUID.fromString(itemUuid)),
            "The itemUuid entered does not match with any item");

        String entityTypeValue = itemService.getMetadata(item, "dspace.entity.type");
        String submissionName = getSubmissionDefinitionName(item);

        List<CrisLayoutTab> layoutTabs =
            Optional.ofNullable(this.configurationService.getProperty("dspace.metadata.layout.tab"))
                    .map(metadataField -> this.itemService.getMetadataByMetadataString(item, metadataField))
                    .filter(metadatas -> !metadatas.isEmpty())
                    .map(metadatas -> metadatas.get(0))
                    .map(metadata ->
                            findValidEntityType(context, entityTypeValue, submissionName + "." +
                                metadata.getAuthority())
                                .orElse(
                                    findValidEntityType(context, entityTypeValue, submissionName + "." +
                                        metadata.getValue())
                                        .orElse(findValidEntityType(context, entityTypeValue, metadata.getAuthority())
                                            .orElse(findValidEntityType(context, entityTypeValue, metadata.getValue())
                                                .orElse(null))))
                    )
                    .orElse(findValidEntityType(context, entityTypeValue, submissionName)
                    .orElse(findByEntityType(context, entityTypeValue, null)));
        if (layoutTabs == null) {
            return Collections.emptyList();
        }
        return layoutTabs;
    }

    @Override
    public boolean hasAccess(Context context, CrisLayoutTab tab, Item item) {
        return crisLayoutTabAccessService.hasAccess(context, context.getCurrentUser(), tab, item);
    }

    private String getSubmissionDefinitionName(Item item) {
        if (submissionConfigReader == null || item.getOwningCollection() == null) {
            return "";
        }

        return submissionConfigReader.getSubmissionConfigByCollection(item.getOwningCollection()).getSubmissionName();
    }

    private Optional<List<CrisLayoutTab>> findValidEntityType(Context context, String entityTypeValue,
                                                              String customFilter) {
        return Optional.ofNullable(customFilter)
                       .map(
                           throwingMapperWrapper(
                               value -> findByEntityType(context, entityTypeValue, value),
                               null
                           )
                       )
                       .filter(tabs -> tabs != null && !tabs.isEmpty());
    }

}
