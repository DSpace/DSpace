/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.service.impl;

import java.util.List;
import java.util.Optional;

import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ExternalDataService}
 */
public class ExternalDataServiceImpl implements ExternalDataService {

    @Autowired
    private List<ExternalDataProvider> externalDataProviders;

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

}
