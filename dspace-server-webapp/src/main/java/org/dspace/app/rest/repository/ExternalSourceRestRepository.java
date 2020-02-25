/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;
import java.util.Optional;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.ExternalSourceEntryRest;
import org.dspace.app.rest.model.ExternalSourceRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is the Repository that is responsible for the functionality and implementations coming from
 * {@link org.dspace.app.rest.ExternalSourcesRestController}
 */
@Component(ExternalSourceRest.CATEGORY + "." + ExternalSourceRest.NAME)
public class ExternalSourceRestRepository extends DSpaceRestRepository<ExternalSourceRest, String> {

    @Autowired
    private ExternalDataService externalDataService;

    @Autowired
    ConverterService converter;

    /**
     * This method will retrieve one ExternalSourceEntryResource based on the ExternalSource for the given
     * externalSourceName and with the given entryId
     * @param externalSourceName The externalSourceName that defines which ExternalDataProvider is used
     * @param entryId       The entryId used for the lookup
     * @return              An ExternalSourceEntryRest object that complies with the above params
     */
    public ExternalSourceEntryRest getExternalSourceEntryValue(String externalSourceName, String entryId) {
        if (externalDataService.getExternalDataProvider(externalSourceName) == null) {
            throw new ResourceNotFoundException("The externalSource for: " + externalSourceName + " couldn't be found");
        }
        Optional<ExternalDataObject> externalDataObject = externalDataService.getExternalDataObject(externalSourceName,
                                                                                                    entryId);
        ExternalDataObject dataObject = externalDataObject.orElseThrow(() -> new ResourceNotFoundException(
            "Couldn't find an ExternalSource for source: " + externalSourceName + " and ID: " + entryId));
        return converter.toRest(dataObject, Projection.DEFAULT);
    }

    /**
     * This method will retrieve all the ExternalSourceEntries for the ExternalSource for the given externalSourceName
     * param
     * @param externalSourceName The externalSourceName that defines which ExternalDataProvider is used
     * @param query         The query used in the lookup
     * @param parent        The parent used in the lookup
     * @param pageable      The pagination object
     * @return              A paginated list of ExternalSourceEntryResource objects that comply with the params
     */
    public Page<ExternalSourceEntryRest> getExternalSourceEntries(String externalSourceName, String query,
                                                                  String parent, Pageable pageable) {
        if (externalDataService.getExternalDataProvider(externalSourceName) == null) {
            throw new ResourceNotFoundException("The externalSource for: " + externalSourceName + " couldn't be found");
        }
        List<ExternalDataObject> externalDataObjects = externalDataService
            .searchExternalDataObjects(externalSourceName, query, Math.toIntExact(pageable.getOffset()),
                    pageable.getPageSize());
        int numberOfResults = externalDataService.getNumberOfResults(externalSourceName, query);
        return converter.toRestPage(externalDataObjects, pageable, numberOfResults,
                                    utils.obtainProjection());
    }

    @Override
    public ExternalSourceRest findOne(Context context, String externalSourceName) {
        ExternalDataProvider externalDataProvider = externalDataService.getExternalDataProvider(externalSourceName);
        if (externalDataProvider == null) {
            throw new ResourceNotFoundException("ExternalDataProvider for: " +
                                                    externalSourceName + " couldn't be found");
        }
        return converter.toRest(externalDataProvider, Projection.DEFAULT);
    }

    @Override
    public Page<ExternalSourceRest> findAll(Context context, Pageable pageable) {
        List<ExternalDataProvider> externalSources = externalDataService.getExternalDataProviders();
        return converter.toRestPage(externalSources, pageable, externalSources.size(),
                                    utils.obtainProjection());
    }

    public Class<ExternalSourceRest> getDomainClass() {
        return ExternalSourceRest.class;
    }
}
