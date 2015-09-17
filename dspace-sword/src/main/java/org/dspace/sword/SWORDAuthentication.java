/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.authenticate.AuthenticationMethod;

/**
 * This class offers a thin wrapper for the default DSpace
 * authentication module for the SWORD implementation
 *
 * @author Richard Jones
 *
 */
public class SWORDAuthentication
{
    /**
     * Does the given username and password authenticate for the
     * given DSpace Context?
     *
     * @param context
     * @param un
     * @param pw
     * @return true if yes, false if not
     */
    public boolean authenticates(Context context, String un, String pw)
    {
        AuthenticationService authService = AuthenticateServiceFactory
                .getInstance().getAuthenticationService();
        int auth = authService.authenticate(context, un, pw, null, null);
        return auth == AuthenticationMethod.SUCCESS;
    }
}
