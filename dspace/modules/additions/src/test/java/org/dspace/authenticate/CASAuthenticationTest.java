package org.dspace.authenticate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.umd.lib.dspace.authenticate.LdapService;
import edu.umd.lib.dspace.authenticate.impl.Ldap;
import org.dspace.AbstractUnitTest;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
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
        LdapService mockLdapService = MockLdapService.userNotFound();

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl) {
                return "no_such_user";
            }

            @Override
            protected LdapService createLdapService(Context context) {
                return mockLdapService;
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest);

        assertEquals("Expected AuthenticationMethod.NO_SUCH_USER",
                AuthenticationMethod.NO_SUCH_USER, response);
    }

    @Test
    public void authenticate_fails_LdapUserFound_EpersonNotFound_Autoregister_false() throws Exception {
        String netid = "eperson_that_does_not_exist_yet";

        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        int initialEPersonCount = ePersonService.countTotal(context);

        HttpServletRequest mockRequest = new MockHttpServletRequest("", Map.of("ticket", "ST-CAS-TICKET"));
        LdapService mockLdapService = MockLdapService.userFound();

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

            @Override
            protected LdapService createLdapService(Context context) {
                return mockLdapService;
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest);

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
        LdapService mockLdapService = MockLdapService.userFound();

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl) {
                return netid;
            }

            @Override
            protected LdapService createLdapService(Context context) {
                return mockLdapService;
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest);

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
        LdapService mockLdapService = MockLdapService.userFound();

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl) {
                return netid;
            }

            @Override
            protected LdapService createLdapService(Context context) {
                return mockLdapService;
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest);

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
        LdapService mockLdapService = MockLdapService.userFound();

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl) {
                return netid;
            }

            @Override
            protected LdapService createLdapService(Context context) {
                return mockLdapService;
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest);

        assertEquals("Expected AuthenticationMethod.SUCCESS",
                AuthenticationMethod.SUCCESS, response);
    }

    @Test
    public void authenticate_success_userFoundInLdapAndEpersonCreated() throws Exception {
        String netid = "eperson_that_does_not_exist_yet";

        HttpServletRequest mockRequest = new MockHttpServletRequest("", Map.of("ticket", "ST-CAS-TICKET"));

        // Set up MockLdapService so we can verify that the "registerEPerson"
        // method was called.
        MockLdapService mockLdapService = new MockLdapService() {
            @Override
            public Ldap queryLdap(String strUid) {
                return new Ldap(strUid, null);
            }
        };

        MockCASAuthentication stubCas = new MockCASAuthentication() {
            @Override
            protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl) {
                return netid;
            }

            @Override
            public boolean canSelfRegister(Context context, HttpServletRequest request, String username)
                    throws SQLException {
                return true;
            }

            @Override
            protected LdapService createLdapService(Context context) {
                return mockLdapService;
            }
        };

        int response = stubCas.authenticate(context, null, null, null, mockRequest);

        assertEquals("Expected AuthenticationMethod.SUCCESS",
                AuthenticationMethod.SUCCESS, response);

        // Verify that Ldap.registerPerson was called
        assertTrue("Ldap.registerEPerson was not called", stubCas.registerEPersonCalled);
    }

    @Test
    public void getSpecialGroups_requestDoesNotHaveLdapAttribute() {
        HttpServletRequest mockRequest = new MockHttpServletRequest();

        CASAuthentication cas = new CASAuthentication();
        List<Group> specialGroups = cas.getSpecialGroups(context, mockRequest);
        assertTrue(specialGroups.isEmpty());
    }

    @Test
    public void getSpecialGroups_requestHasLdapAttribute_CASAuthenticatedGroupDoesNotExist() throws Exception {
        Ldap mockLdap = new MockLdap(new ArrayList<Group>());
        MockHttpSession mockSession = new MockHttpSession();
        mockSession.setAttribute(CASAuthentication.CAS_LDAP, mockLdap);
        HttpServletRequest mockRequest = new MockHttpServletRequest(mockSession);

        List<Group> specialGroups = cas.getSpecialGroups(context, mockRequest);
        assertEquals(0, specialGroups.size());
    }

    @Test
    public void getSpecialGroups_requestHasLdapAttribute_CASAuthenticatedGroupExists() throws Exception {
        Ldap mockLdap = new MockLdap(new ArrayList<Group>(List.of(
            createGroupWithName("Group1"),
            createGroupWithName("Group2")))
        );
        MockHttpSession mockSession = new MockHttpSession();
        mockSession.setAttribute(CASAuthentication.CAS_LDAP, mockLdap);
        HttpServletRequest mockRequest = new MockHttpServletRequest(mockSession);
        createGroupWithName("CAS Authenticated");

        List<Group> specialGroups = cas.getSpecialGroups(context, mockRequest);
        assertEquals(3, specialGroups.size());
        List<String> groupNames = specialGroups.stream().map(group -> group.getName()).collect(toList());
        assertThat(groupNames, hasItems("Group1", "Group2", "CAS Authenticated"));
    }

    @Test
    public void getSpecialGroups_requestHasLdapAttributeWithMultipleGroups_CASAuthenticatedGroupExists()
            throws Exception {
        Ldap mockLdap = new MockLdap(new ArrayList<Group>());
        MockHttpSession mockSession = new MockHttpSession();
        mockSession.setAttribute(CASAuthentication.CAS_LDAP, mockLdap);
        HttpServletRequest mockRequest = new MockHttpServletRequest(mockSession);
        createGroupWithName("CAS Authenticated");

        List<Group> specialGroups = cas.getSpecialGroups(context, mockRequest);
        assertEquals(1, specialGroups.size());
        assertEquals("CAS Authenticated", specialGroups.get(0).getName());
    }

    @Test
    public void getSpecialGroups_impersonatingUser_impersonatedUserNotInLdap() throws Exception {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        context.switchContextUser(eperson);

        LdapService mockLdapService = MockLdapService.userNotFound();

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected LdapService createLdapService(Context context) {
                return mockLdapService;
            }
        };

        List<Group> specialGroups = stubCas.getSpecialGroups(context, mockRequest);
        assertEquals(0, specialGroups.size());
    }

    @Test
    public void getSpecialGroups_impersonatingUser_impersonatedUserInLdap() throws Exception {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        eperson.setNetid("impersonatedUser");
        context.switchContextUser(eperson);

        LdapService mockLdapService = MockLdapService.userFound();

        CASAuthentication stubCas = new CASAuthentication() {
            @Override
            protected LdapService createLdapService(Context context) {
                return mockLdapService;
            }
        };

        createGroupWithName("CAS Authenticated");

        List<Group> specialGroups = stubCas.getSpecialGroups(context, mockRequest);
        assertEquals(1, specialGroups.size());
        assertEquals("CAS Authenticated", specialGroups.get(0).getName());
    }

    /**
     * Helper method to create a Group with the given name.
     *
     * @param name the name of the Group to create
     */
    private Group createGroupWithName(String name) throws Exception {
        context.turnOffAuthorisationSystem();
        try {
            GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
            Group group = groupService.create(context);
            groupService.setName(group, name);
            return group;
        } finally {
            context.restoreAuthSystemState();
        }
    }

}

class MockHttpServletRequest extends DummyHttpServletRequest {
    private String serviceUrl = null;
    private Map<String, String> parameterMap = Collections.emptyMap();
    private HttpSession session = new MockHttpSession();

    public MockHttpServletRequest() {
    }

    public MockHttpServletRequest(HttpSession session) {
        this.session = session;
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
        return session;
    }

    @Override
    public void setAttribute(String name, Object o) {
        return;
    }
}

class MockLdapService implements LdapService {
    // Convenience method returning MockLdapService instance that returns an
    // Ldap object, indicating that the user was found in LDAP.
    public static MockLdapService userFound() {
        return new MockLdapService() {
            @Override
            public Ldap queryLdap(String strUid) {
                return new Ldap(strUid, null);
            }
        };
    }

    // Convenience method returning MockLdapService instance that always
    // indicates the user was not found in LDAP.
    public static MockLdapService userNotFound() {
        return new MockLdapService();
    }

    @Override
    public Ldap queryLdap(String strUid) {
        return null;
    }

    @Override
    public void close() {
    }
}

class MockCASAuthentication extends CASAuthentication {
    public boolean registerEPersonCalled = false;

    @Override
    protected EPerson registerEPerson(String uid, Context context, Ldap ldap, HttpServletRequest request)
            throws Exception {
        registerEPersonCalled = true;
        return super.registerEPerson(uid, context, ldap, request);
    }

    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }
}

class MockLdap extends Ldap {
    private List<Group> groups;

    public MockLdap(List<Group> groups) {
        super("testUser", null);
        this.groups = groups;
    }

    @Override
    public List<Group> getGroups(Context context) {
        return groups;
    }
}

