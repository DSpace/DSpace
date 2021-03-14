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
import org.dspace.app.rest.model.hateoas.AuthnResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to provide a means to add links to {@link org.dspace.app.rest.model.hateoas.AuthnResource}s
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 */
@Component
public class AuthnHalLinkFactory extends HalLinkFactory<AuthnResource, AuthenticationRestController> {

    @Override
    protected void addLinks(AuthnResource halResource, Pageable pageable, LinkedList<Link> list) throws Exception {
        AuthenticationRestController methodOn = getMethodOn();

        list.add(buildLink("login", methodOn
            .login(null, null, null)));

        list.add(buildLink("logout", methodOn
            .logout()));

        list.add(buildLink("status", methodOn
            .status(null, null)));
    }

    @Override
    protected Class<AuthenticationRestController> getControllerClass() {
        return AuthenticationRestController.class;
    }

    @Override
    protected Class<AuthnResource> getResourceClass() {
        return AuthnResource.class;
    }
}
