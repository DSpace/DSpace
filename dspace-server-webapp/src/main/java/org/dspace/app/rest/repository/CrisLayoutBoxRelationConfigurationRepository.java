/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.CrisLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutBoxRelationConfigurationRest;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxConfiguration;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component(CrisLayoutBoxRelationConfigurationRest.CATEGORY + "." + CrisLayoutBoxRelationConfigurationRest.NAME)
public class CrisLayoutBoxRelationConfigurationRepository
        extends DSpaceRestRepository<CrisLayoutBoxRelationConfigurationRest, Integer> {

    @Autowired
    private CrisLayoutBoxService service;

    @Override
    public Page<CrisLayoutBoxRelationConfigurationRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not Implemented!", "");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dspace.app.rest.repository.DSpaceRestRepository#findOne(org.dspace.core.
     * Context, java.io.Serializable)
     */
    @Override
    @PreAuthorize("permitAll")
    public CrisLayoutBoxRelationConfigurationRest findOne(Context context, Integer id) {
        try {
            CrisLayoutBox box = service.find(context, id);
            if (box != null) {
                CrisLayoutBoxConfiguration configuration = service.getConfiguration(context, box);
                CrisLayoutBoxConfigurationRest confRest = converter.toRest(configuration, utils.obtainProjection());
                if (confRest instanceof CrisLayoutBoxRelationConfigurationRest) {
                    return (CrisLayoutBoxRelationConfigurationRest) confRest;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#getDomainClass()
     */
    @Override
    public Class<CrisLayoutBoxRelationConfigurationRest> getDomainClass() {
        return CrisLayoutBoxRelationConfigurationRest.class;
    }
}
