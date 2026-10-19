/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.VersionHistoryRest;
import org.dspace.core.Context;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Repository for the operations on the {@link VersionHistoryRest} endpoints
 */
@Component(VersionHistoryRest.CATEGORY + "." + VersionHistoryRest.PLURAL_NAME)
public class VersionHistoryRestRepository extends DSpaceRestRepository<VersionHistoryRest, Integer> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(VersionHistoryRestRepository.class);

    @Autowired
    private VersionHistoryService versionHistoryService;

    @Autowired
    private ConverterService converterService;

    @Override
    @PreAuthorize("@versioningSecurity.isEnableVersioning() && hasPermission(#id, 'VERSIONHISTORY', 'READ')")
    public VersionHistoryRest findOne(Context context, Integer id) {
        try {
            VersionHistory versionHistory = versionHistoryService.find(context, id);
            if (versionHistory == null) {
                throw new ResourceNotFoundException("Couldn't find version for id: " + id);
            }
            return converterService.toRest(versionHistory, utils.obtainProjection());
        } catch (SQLException e) {
            log.error("Something with wrong getting version with id:" + id, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Page<VersionHistoryRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    public Class<VersionHistoryRest> getDomainClass() {
        return VersionHistoryRest.class;
    }
}
