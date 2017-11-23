/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.sql.SQLException;

import org.dspace.app.rest.AuthenticationRestController;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;
import org.springframework.hateoas.Link;

/**
 * Authn Rest Resource, used to link to login, logout, status, ...
 *
 * @author Atmire NV (info at atmire dot com)
 */
@RelNameDSpaceResource(AuthnRest.NAME)
public class AuthnResource extends DSpaceResource<AuthnRest> {

    public AuthnResource(AuthnRest data, Utils utils, String... rels) throws SQLException {
        super(data, utils, rels);

        AuthenticationRestController methodOn = methodOn(AuthenticationRestController.class);

        add(new Link(linkTo(methodOn
                .login(null, null,  null))
                .toUriComponentsBuilder().build().toString(), "login"));

        add(new Link(linkTo(methodOn
                .logout())
                .toUriComponentsBuilder().build().toString(), "logout"));

        add(new Link(linkTo(methodOn
                .status(null))
                .toUriComponentsBuilder().build().toString(), "status"));
    }
}
