/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link AuthenticationMethod} that delegate all the method
 * invocations to the bean of class {@link OrcidAuthenticationBean}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OidcAuthentication implements AuthenticationMethod {

    private final ServiceManager serviceManager = new DSpace().getServiceManager();

    private static final String OIDC_AUTHENTICATED = "oidc.authenticated";

    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request, String username) throws SQLException {
        return getOidcAuthentication().canSelfRegister(context, request, username);
    }

    @Override
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException {
        getOidcAuthentication().initEPerson(context, request, eperson);
    }

    @Override
    public boolean allowSetPassword(Context context, HttpServletRequest request, String username) throws SQLException {
        return getOidcAuthentication().allowSetPassword(context, request, username);
    }

    @Override
    public boolean isImplicit() {
        return getOidcAuthentication().isImplicit();
    }

    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
        return getOidcAuthentication().getSpecialGroups(context, request);
    }

    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request)
        throws SQLException {
        return getOidcAuthentication().authenticate(context, username, password, realm, request);
    }

    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {
        return getOidcAuthentication().loginPageURL(context, request, response);
    }

    @Override
    public String getName() {
        return getOidcAuthentication().getName();
    }

    private OidcAuthenticationBean getOidcAuthentication() {
        return serviceManager.getServiceByName("oidcAuthentication", OidcAuthenticationBean.class);
    }

    @Override
    public boolean isUsed(final Context context, final HttpServletRequest request) {
        if (request != null &&
                context.getCurrentUser() != null &&
                request.getAttribute(OIDC_AUTHENTICATED) != null) {
            return true;
        }
        return false;
    }

}
