/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.CrisLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxConfiguration;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage CrisLayoutMetadataConfigurationRest object
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component(CrisLayoutMetadataConfigurationRest.CATEGORY + "." + CrisLayoutMetadataConfigurationRest.NAME)
public class CrisLayoutMetadataConfigurationRepository
    extends DSpaceRestRepository<CrisLayoutMetadataConfigurationRest, Integer> {

    @Autowired
    private CrisLayoutBoxService service;

    @Autowired
    private ResourcePatch<CrisLayoutBox> resourcePatch;

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#findOne(org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    @PreAuthorize("permitAll")
    public CrisLayoutMetadataConfigurationRest findOne(Context context, Integer id) {
        try {
            CrisLayoutBox box = service.find(context, id);
            if (box != null) {
                CrisLayoutBoxConfiguration configuration = service.getConfiguration(context, box);
                CrisLayoutBoxConfigurationRest confRest = converter.toRest(configuration, utils.obtainProjection());
                if (confRest instanceof CrisLayoutMetadataConfigurationRest) {
                    return (CrisLayoutMetadataConfigurationRest) confRest;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#
     * findAll(org.dspace.core.Context, org.springframework.data.domain.Pageable)
     */
    @Override
    public Page<CrisLayoutMetadataConfigurationRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not Implemented!", "");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#getDomainClass()
     */
    @Override
    public Class<CrisLayoutMetadataConfigurationRest> getDomainClass() {
        return CrisLayoutMetadataConfigurationRest.class;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public void patch(Context context, HttpServletRequest request, String apiCategory, String model, Integer id,
            Patch patch) throws AuthorizeException, SQLException {
        CrisLayoutBox box = null;
        try {
            box = service.find(context, id);
            if (box == null) {
                throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
            }
            resourcePatch.patch(context, box, patch.getOperations());
            service.update(context, box);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
