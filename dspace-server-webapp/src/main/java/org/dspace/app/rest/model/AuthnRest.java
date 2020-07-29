/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.AuthenticationRestController;

/**
 * Root rest object for the /api/authn endpoint
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public class AuthnRest extends BaseObjectRest<Integer> {

    public static final String NAME = "authn";
    public static final String CATEGORY = RestAddressableModel.AUTHENTICATION;

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return AuthenticationRestController.class;
    }
}
