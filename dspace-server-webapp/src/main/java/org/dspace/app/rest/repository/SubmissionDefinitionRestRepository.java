/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage MetadataField Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(SubmissionDefinitionRest.CATEGORY + "." + SubmissionDefinitionRest.NAME)
public class SubmissionDefinitionRestRepository extends DSpaceRestRepository<SubmissionDefinitionRest, String> {
    private SubmissionConfigReader submissionConfigReader;

    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    public SubmissionDefinitionRestRepository() throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public SubmissionDefinitionRest findOne(Context context, String submitName) {
        SubmissionConfig subConfig = submissionConfigReader.getSubmissionConfigByName(submitName);
        if (subConfig == null) {
            return null;
        }
        return converter.toRest(subConfig, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public Page<SubmissionDefinitionRest> findAll(Context context, Pageable pageable) {
        int total = submissionConfigReader.countSubmissionConfigs();
        List<SubmissionConfig> subConfs = submissionConfigReader.getAllSubmissionConfigs(
                pageable.getPageSize(), Math.toIntExact(pageable.getOffset()));
        return converter.toRestPage(subConfs, pageable, total, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @SearchRestMethod(name = "findByCollection")
    public SubmissionDefinitionRest findByCollection(@Parameter(value = "uuid", required = true) UUID collectionUuid)
            throws SQLException {
        Collection col = collectionService.find(obtainContext(), collectionUuid);
        if (col == null) {
            return null;
        }
        SubmissionDefinitionRest def = converter
            .toRest(submissionConfigReader.getSubmissionConfigByCollection(col.getHandle()),
                    utils.obtainProjection());
        return def;
    }

    @Override
    public Class<SubmissionDefinitionRest> getDomainClass() {
        return SubmissionDefinitionRest.class;
    }
}
