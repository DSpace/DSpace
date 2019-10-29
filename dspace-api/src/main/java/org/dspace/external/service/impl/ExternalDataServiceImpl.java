/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.dspace.mock.MockMetadataValue;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ExternalDataService}
 */
public class ExternalDataServiceImpl implements ExternalDataService {

    private static final Logger log = Logger.getLogger(ExternalDataServiceImpl.class);

    @Autowired
    private List<ExternalDataProvider> externalDataProviders;

    @Autowired
    private ItemService itemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private InstallItemService installItemService;

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String source, String id) {
        ExternalDataProvider provider = getExternalDataProvider(source);
        if (provider == null) {
            throw new IllegalArgumentException("Provider for: " + source + " couldn't be found");
        }
        return provider.getExternalDataObject(id);
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String source, String query, int start, int limit) {
        ExternalDataProvider provider = getExternalDataProvider(source);
        if (provider == null) {
            throw new IllegalArgumentException("Provider for: " + source + " couldn't be found");
        }
        return provider.searchExternalDataObjects(query, start, limit);
    }


    @Override
    public List<ExternalDataProvider> getExternalDataProviders() {
        return externalDataProviders;
    }

    @Override
    public ExternalDataProvider getExternalDataProvider(String sourceIdentifier) {
        for (ExternalDataProvider externalDataProvider : externalDataProviders) {
            if (externalDataProvider.supports(sourceIdentifier)) {
                return externalDataProvider;
            }
        }
        return null;
    }

    @Override
    public int getNumberOfResults(String source, String query) {
        ExternalDataProvider provider = getExternalDataProvider(source);
        if (provider == null) {
            throw new IllegalArgumentException("Provider for: " + source + " couldn't be found");
        }
        return provider.getNumberOfResults(query);
    }


    @Override
    public Item createItemFromExternalDataObject(Context context, ExternalDataObject externalDataObject,
                                                 Collection collection)
        throws AuthorizeException, SQLException {
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = workspaceItem.getItem();
        for (MockMetadataValue mockMetadataValue : externalDataObject.getMetadata()) {
            itemService.addMetadata(context, item, mockMetadataValue.getSchema(), mockMetadataValue.getElement(),
                                    mockMetadataValue.getQualifier(), mockMetadataValue.getLanguage(),
                                    mockMetadataValue.getValue(), mockMetadataValue.getAuthority(),
                                    mockMetadataValue.getConfidence());
        }

        return installItemService.installItem(context, workspaceItem);
    }
}
