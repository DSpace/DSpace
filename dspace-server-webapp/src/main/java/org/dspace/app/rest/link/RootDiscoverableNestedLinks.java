/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import java.util.Arrays;

import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.model.AuthorizationRest;
import org.dspace.app.rest.model.ClaimedTaskRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.PoolTaskRest;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.repository.AuthorityRestRepository;
import org.dspace.app.rest.repository.ClaimedTaskRestRepository;
import org.dspace.app.rest.repository.EPersonRestRepository;
import org.dspace.app.rest.repository.PoolTaskRestRepository;
import org.dspace.app.rest.repository.ResourcePolicyRestRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This class is responsible to add to the root endpoint the links to standard nested endpoint
 * that are not discoverable due to limitation to access some resource collection endpoint via GET.
 * If a custom endpoint should require to add extra links to the root is recommended to register
 * them directly from the Repository class implementation or the custom controller.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science)
 */
@Component
public class RootDiscoverableNestedLinks implements InitializingBean {

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(ResourcePolicyRestRepository.class , Arrays.asList(new Link("/api/"
                    + ResourcePolicyRest.CATEGORY + "/" + ResourcePolicyRest.NAME + "/search",
                                                          ResourcePolicyRest.NAME + "-search")));

        discoverableEndpointsService
            .register(AuthorityRestRepository.class , Arrays.asList(new Link("/api/"
                    + AuthorizationRest.CATEGORY  + "/" + AuthorizationRest.NAME + "/search",
                                                          AuthorizationRest.NAME  + "-search")));

        discoverableEndpointsService
            .register(ClaimedTaskRestRepository.class , Arrays.asList(new Link("/api/"
                    + ClaimedTaskRest.CATEGORY + "/" + ClaimedTaskRest.NAME + "/search",
                                                       ClaimedTaskRest.NAME + "-search")));

        discoverableEndpointsService
            .register(PoolTaskRestRepository.class , Arrays.asList(new Link("/api/"
                    + PoolTaskRest.CATEGORY + "/" + PoolTaskRest.NAME + "/search",
                                                    PoolTaskRest.NAME + "-search")));

        discoverableEndpointsService
            .register(EPersonRestRepository.class , Arrays.asList(new Link("/api/"
                    + EPersonRest.CATEGORY + "/registrations", EPersonRest.NAME + "-registration")));

    }

}
