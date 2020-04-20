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
import org.dspace.app.rest.model.VersionRest;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the Repository that takes care of the operations on the {@link VersionRest} objects
 */
@Component(VersionRest.CATEGORY + "." + VersionRest.NAME)
public class VersionRestRepository extends DSpaceRestRepository<VersionRest, Integer> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(VersionRestRepository.class);

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private ConverterService converterService;

    @Override
    @PreAuthorize("hasPermission(#id, 'VERSION', 'READ')")
    public VersionRest findOne(Context context, Integer id) {
        try {
            Version version = versioningService.getVersion(context, id);
            if (version == null) {
                throw new ResourceNotFoundException("Couldn't find version for id: " + id);
            }
            return converterService.toRest(version, utils.obtainProjection());
        } catch (SQLException e) {
            log.error("Something with wrong getting version with id:" + id, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Page<VersionRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    public Class<VersionRest> getDomainClass() {
        return VersionRest.class;
    }
}
