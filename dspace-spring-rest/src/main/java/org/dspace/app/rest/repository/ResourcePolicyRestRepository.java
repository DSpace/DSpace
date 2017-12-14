/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import org.dspace.app.rest.converter.ResourcePolicyConverter;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.hateoas.ResourcePolicyResource;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of default access condition
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component(ResourcePolicyRest.CATEGORY + "." + ResourcePolicyRest.NAME)
public class ResourcePolicyRestRepository extends DSpaceRestRepository<ResourcePolicyRest, Integer> {

    @Autowired
    ResourcePolicyService resourcePolicyService;

    @Autowired
    ResourcePolicyConverter resourcePolicyConverter;

    @Autowired
    Utils utils;

    //TODO @PreAuthorize("hasAuthority('EPERSON')")
    @Override
    public ResourcePolicyRest findOne(Integer id) {
        ResourcePolicy source = null;
        try {
            source = resourcePolicyService.find(obtainContext(), id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (source == null) {
            return null;
        }
        return resourcePolicyConverter.convert(source);
    }

    //TODO @PreAuthorize("hasAuthority('EPERSON')")
    @Override
    public Page<ResourcePolicyRest> findAll(Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(ResourcePolicyRest.NAME, "findAll");
    }


    @Override
    public Class<ResourcePolicyRest> getDomainClass() {
        return ResourcePolicyRest.class;
    }


    @Override
    public ResourcePolicyResource wrapResource(ResourcePolicyRest model, String... rels) {
        return new ResourcePolicyResource(model, utils, rels);
    }

}
