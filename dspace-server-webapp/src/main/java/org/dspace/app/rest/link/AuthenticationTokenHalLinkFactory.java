/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import java.util.LinkedList;

import org.dspace.app.rest.AuthenticationRestController;
import org.dspace.app.rest.model.hateoas.AuthenticationTokenResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This class adds the self link to the AuthenticationTokenResource.
 */
@Component
public class AuthenticationTokenHalLinkFactory
    extends HalLinkFactory<AuthenticationTokenResource, AuthenticationRestController> {

    @Override
    protected void addLinks(AuthenticationTokenResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        list.add(buildLink(IanaLinkRelations.SELF.value(), getMethodOn().shortLivedToken(null)));
    }

    @Override
    protected Class<AuthenticationRestController> getControllerClass() {
        return AuthenticationRestController.class;
    }

    @Override
    protected Class<AuthenticationTokenResource> getResourceClass() {
        return AuthenticationTokenResource.class;
    }
}
