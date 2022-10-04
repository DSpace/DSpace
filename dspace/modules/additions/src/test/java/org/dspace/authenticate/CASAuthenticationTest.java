package org.dspace.authenticate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.umd.lib.dspace.authenticate.Ldap;
import edu.umd.lib.dspace.authenticate.impl.LdapInfo;

import org.dspace.AbstractUnitTest;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.statistics.util.DummyHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;

public class CASAuthenticationTest extends AbstractUnitTest {
    private CASAuthentication cas;

    @Before
    public void setUp() {
        cas = new CASAuthentication();
    }

    @Test
    public void getServiceUrlFromRequest_noRedirectUrl() {
        String serviceUrl = "http://localhost:8080/server/api/authn/cas";
        HttpServletRequest stubRequest = new MockHttpServletRequest(serviceUrl);

        String response = cas.getServiceUrlFromRequest(context, stubRequest);
        assertEquals(serviceUrl, response);
    }

    @Test
    public void getServiceUrlFromRequest_withRedirectUrl() {
        // Treat this as a GET request, where the query string is not provided
        String serviceUrl = "http://localhost:8080/server/api/authn/cas";
        String redirectUrl = "http://localhost:8080/server/login.html";

        HttpServletRequest mockRequest = new MockHttpServletRequest(serviceUrl,
                Map.of("redirectUrl", redirectUrl));

        String response = cas.getServiceUrlFromRequest(context, mockRequest);

        assertEquals(serviceUrl + "?redirectUrl=" + redirectUrl, response);
    }

    // Authentication failures
    @Test
    public void authenticate_fails_NoCasTicket() throws Exception {
        HttpServletRequest stubRequest = new MockHttpServletRequest();

        int response = cas.authenticate(context, null, null, null, stubRequest);

        assertEquals(AuthenticationMethod.BAD_ARGS, response);
    }

    @Test
    public void authenticate_fails_LdapUserNotFound() throws Exception {
        HttpServletRequest mockRequest = new MockHttpServletRequest("", Map.of("ticket", "ST-CAS-TICKET"));

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl) {
                return "no_such_user";
            }
        };

        Ldap mockLdap = new MockLdap() {
            @Override
            public LdapInfo checkUid(String strUid) throws NamingException {
              return new LdapInfo(strUid, null);
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest, mockLdap);

        assertEquals("Expected AuthenticationMethod.NO_SUCH_USER",
                AuthenticationMethod.NO_SUCH_USER, response);
    }

    @Test
    public void authenticate_fails_LdapUserFound_EpersonNotFound_Autoregister_false() throws Exception {
        String netid = "eperson_that_does_not_exist_yet";

        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        int initialEPersonCount = ePersonService.countTotal(context);

        HttpServletRequest mockRequest = new MockHttpServletRequest("", Map.of("ticket", "ST-CAS-TICKET"));

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl) {
                return netid;
            }

            @Override
            public boolean canSelfRegister(Context context, HttpServletRequest request, String username)
                    throws SQLException {
                return false;
            }
        };

        Ldap mockLdap = new MockLdap() {
            @Override
            public LdapInfo checkUid(String strUid) throws NamingException {
              return new LdapInfo(strUid, null);
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest, mockLdap);

        assertEquals("Expected AuthenticationMethod.NO_SUCH_USER",
                AuthenticationMethod.NO_SUCH_USER, response);

        int currentEPersonCount = ePersonService.countTotal(context);
        int expectedEPersonCount = initialEPersonCount;
        assertEquals("EPerson count did not increase by 1",
                expectedEPersonCount, currentEPersonCount);
    }

    @Test
    public void authenticate_fail_EPersonCertRequired() throws Exception {
        String netid = "test_user";
        eperson.setNetid(netid);
        eperson.setRequireCertificate(true);
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        ePersonService.update(context, eperson);

        HttpServletRequest mockRequest = new MockHttpServletRequest("", Map.of("ticket", "ST-CAS-TICKET"));

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl) {
                return netid;
            }
        };

        MockLdap mockLdap = new MockLdap() {
            @Override
            public LdapInfo checkUid(String strUid) throws NamingException {
              return new LdapInfo(strUid, null);
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest, mockLdap);

        assertEquals("Expected AuthenticationMethod.CERT_REQUIRED",
                AuthenticationMethod.CERT_REQUIRED, response);
    }

    @Test
    public void authenticate_fail_EPersonCannotLogin() throws Exception {
        String netid = "test_user";
        eperson.setNetid(netid);
        eperson.setCanLogIn(false);
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        ePersonService.update(context, eperson);

        HttpServletRequest mockRequest = new MockHttpServletRequest("", Map.of("ticket", "ST-CAS-TICKET"));

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl) {
                return netid;
            }
        };

        MockLdap mockLdap = new MockLdap() {
            @Override
            public LdapInfo checkUid(String strUid) throws NamingException {
              return new LdapInfo(strUid, null);
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest, mockLdap);

        assertEquals("Expected AuthenticationMethod.BAD_ARGS",
                AuthenticationMethod.BAD_ARGS, response);
    }

    // Successful authenticaations
    @Test
    public void authenticate_success_userFoundInLdapAndEpersonExists() throws Exception {
        String netid = "test_user";
        eperson.setNetid(netid);
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        ePersonService.update(context, eperson);

        HttpServletRequest mockRequest = new MockHttpServletRequest("", Map.of("ticket", "ST-CAS-TICKET"));

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl) {
                return netid;
            }
        };

        Ldap mockLdap = new MockLdap() {
            @Override
            public LdapInfo checkUid(String strUid) throws NamingException {
              return new LdapInfo(strUid, null);
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest, mockLdap);

        assertEquals("Expected AuthenticationMethod.SUCCESS",
                AuthenticationMethod.SUCCESS, response);
    }

    @Test
    public void authenticate_success_userFoundInLdapAndEpersonCreated() throws Exception {
        String netid = "eperson_that_does_not_exist_yet";

        HttpServletRequest mockRequest = new MockHttpServletRequest("", Map.of("ticket", "ST-CAS-TICKET"));

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl) {
                return netid;
            }

            @Override
            public boolean canSelfRegister(Context context, HttpServletRequest request, String username)
                    throws SQLException {
                return true;
            }
        };

        // Set up MockLdap so we can verify that the "registerEPerson" method was
        // called.
        MockLdap mockLdap = new MockLdap() {
            @Override
            public LdapInfo checkUid(String strUid) throws NamingException {
                return new LdapInfo(strUid, null);
            }

            @Override
            public EPerson registerEPerson(String uid, LdapInfo ldapInfo, HttpServletRequest request) throws Exception {
                registerEPersonCalled = true;
                return eperson;
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest, mockLdap);

        assertEquals("Expected AuthenticationMethod.SUCCESS",
                AuthenticationMethod.SUCCESS, response);

        // Verify that Ldap.registerPerson was called
        assertTrue("Ldap.registerEPerson was not called", mockLdap.registerEPersonCalled);
    }
}

class MockHttpServletRequest extends DummyHttpServletRequest {
    private String serviceUrl = null;
    private Map<String, String> parameterMap = Collections.emptyMap();

    public MockHttpServletRequest() {
    }

    public MockHttpServletRequest(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public MockHttpServletRequest(String serviceUrl, Map<String, String> parameterMap) {
        this.serviceUrl = serviceUrl;
        this.parameterMap = parameterMap;
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(serviceUrl);
    }

    @Override
    public String getParameter(String arg) {
        return parameterMap.get(arg);
    }

    @Override
    public HttpSession getSession() {
        return new MockHttpSession();
    }

    @Override
    public void setAttribute(String name, Object o) {
        return;
    }
}

class MockLdap implements edu.umd.lib.dspace.authenticate.Ldap {

    public boolean registerEPersonCalled = false;

    @Override
    public LdapInfo checkUid(String strUid) throws NamingException {
        return null;
    }

    @Override
    public boolean checkPassword(String strPassword) throws NamingException {
        return false;
    }

    @Override
    public boolean checkAdmin(String strLdapPassword) {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public EPerson registerEPerson(String uid, LdapInfo ldapInfo, HttpServletRequest request) throws Exception {
        return null;
    }

    @Override
    public void setContext(Context context) {
    }

    @Override
    public void finalize() {
    }
}
