/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authenticate.factory.AuthenticateServiceFactory;
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
public class OrcidAuthentication implements AuthenticationMethod {

    private final ServiceManager serviceManager = new DSpace().getServiceManager();

    /**
     * Check if OrcidAuthentication plugin is enabled
     * @return true if enabled, false otherwise
     */
    public static boolean isEnabled() {

        String pluginName = new OrcidAuthentication().getName();

        Iterator<AuthenticationMethod> authenticationMethodIterator = AuthenticateServiceFactory.getInstance()
            .getAuthenticationService().authenticationMethodIterator();

        while (authenticationMethodIterator.hasNext()) {
            if (pluginName.equals(authenticationMethodIterator.next().getName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request, String username) throws SQLException {
        return getOrcidAuthentication().canSelfRegister(context, request, username);
    }

    @Override
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException {
        getOrcidAuthentication().initEPerson(context, request, eperson);
    }

    @Override
    public boolean allowSetPassword(Context context, HttpServletRequest request, String username) throws SQLException {
        return getOrcidAuthentication().allowSetPassword(context, request, username);
    }

    @Override
    public boolean isImplicit() {
        return getOrcidAuthentication().isImplicit();
    }

    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
        return getOrcidAuthentication().getSpecialGroups(context, request);
    }

    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request)
        throws SQLException {
        return getOrcidAuthentication().authenticate(context, username, password, realm, request);
    }

    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {
        return getOrcidAuthentication().loginPageURL(context, request, response);
    }

    @Override
    public String getName() {
        return getOrcidAuthentication().getName();
    }

    private OrcidAuthenticationBean getOrcidAuthentication() {
        return serviceManager.getServiceByName("orcidAuthentication", OrcidAuthenticationBean.class);
    }

    @Override
    public boolean isUsed(Context context, HttpServletRequest request) {
        return getOrcidAuthentication().isUsed(context, request);
    }

}
