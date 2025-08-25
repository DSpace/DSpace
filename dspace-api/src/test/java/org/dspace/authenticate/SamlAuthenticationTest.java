/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.AbstractUnitTest;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.MetadataValue;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class SamlAuthenticationTest extends AbstractUnitTest {
    private static ConfigurationService configurationService;

    private HttpServletRequest request;
    private SamlAuthentication samlAuth;
    private EPerson testUser;

    @BeforeClass
    public static void beforeAll() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        AbstractBuilder.init(); // AbstractUnitTest doesn't do this for us.
    }

    @Before
    public void beforeEach() throws Exception {
        configurationService.setProperty("authentication-saml.autoregister", true);
        configurationService.setProperty("authentication-saml.eperson.metadata.autocreate", true);

        request = new MockHttpServletRequest();
        samlAuth = new SamlAuthentication();
        testUser = null;
    }

    @After
    public void afterEach() throws Exception {
        if (testUser != null) {
            EPersonBuilder.deleteEPerson(testUser.getID());
        }
    }

    @AfterClass
    public static void afterAll() {
        AbstractBuilder.destroy(); // AbstractUnitTest doesn't do this for us.
    }

    @Test
    public void testAuthenticateExistingUserByEmail() throws Exception {
        context.setCurrentUser(null);
        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("alyssa@dspace.org")
            .withNameInMetadata("Alyssa", "Hacker")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        request.setAttribute("org.dspace.saml.EMAIL", List.of("alyssa@dspace.org"));

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.SUCCESS, result);

        EPerson user = context.getCurrentUser();

        assertNotNull(user);
        assertEquals("alyssa@dspace.org", user.getEmail());
        assertNull(user.getNetid());
        assertEquals("Alyssa", user.getFirstName());
        assertEquals("Hacker", user.getLastName());
    }

    @Test
    public void testAuthenticateExistingUserByNetId() throws Exception {
        context.setCurrentUser(null);
        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("alyssa@dspace.org")
            .withNetId("001")
            .withNameInMetadata("Alyssa", "Hacker")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        request.setAttribute("org.dspace.saml.NAME_ID", "001");

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.SUCCESS, result);

        EPerson user = context.getCurrentUser();

        assertNotNull(user);
        assertEquals("alyssa@dspace.org", user.getEmail());
        assertEquals("001", user.getNetid());
        assertEquals("Alyssa", user.getFirstName());
        assertEquals("Hacker", user.getLastName());
    }

    @Test
    public void testAuthenticateExistingUserByEmailWithUnexpectedNetId() throws Exception {
        EPerson originalUser = context.getCurrentUser();

        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("ben@dspace.org")
            .withNetId("002")
            .withNameInMetadata("Ben", "Bitdiddle")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        request.setAttribute("org.dspace.saml.EMAIL", List.of("ben@dspace.org"));
        request.setAttribute("org.dspace.saml.NAME_ID", "oh-no-its-different-than-the-stored-netid");

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.NO_SUCH_USER, result);
        assertEquals(originalUser, context.getCurrentUser());
    }

    @Test
    public void testAuthenticateExistingUserByEmailUpdatesNullNetId() throws Exception {
        context.setCurrentUser(null);
        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("carrie@dspace.org")
            .withNameInMetadata("Carrie", "Pragma")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        request.setAttribute("org.dspace.saml.EMAIL", List.of("carrie@dspace.org"));
        request.setAttribute("org.dspace.saml.NAME_ID", "netid-from-idp");

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.SUCCESS, result);

        EPerson user = context.getCurrentUser();

        assertNotNull(user);
        assertEquals("carrie@dspace.org", user.getEmail());
        assertEquals("netid-from-idp", user.getNetid());
        assertEquals("Carrie", user.getFirstName());
        assertEquals("Pragma", user.getLastName());
    }

    @Test
    public void testAuthenticateExistingUserByNetIdUpdatesEmail() throws Exception {
        context.setCurrentUser(null);
        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("alyssa@dspace.org")
            .withNetId("001")
            .withNameInMetadata("Alyssa", "Hacker")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        request.setAttribute("org.dspace.saml.NAME_ID", "001");
        request.setAttribute("org.dspace.saml.EMAIL", List.of("aphacker@dspace.org"));

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.SUCCESS, result);

        EPerson user = context.getCurrentUser();

        assertNotNull(user);
        assertEquals("aphacker@dspace.org", user.getEmail());
        assertEquals("001", user.getNetid());
        assertEquals("Alyssa", user.getFirstName());
        assertEquals("Hacker", user.getLastName());
    }

    @Test
    public void testAuthenticateExistingUserUpdatesName() throws Exception {
        context.setCurrentUser(null);
        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("alyssa@dspace.org")
            .withNetId("001")
            .withNameInMetadata("Alyssa", "Hacker")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        request.setAttribute("org.dspace.saml.NAME_ID", "001");
        request.setAttribute("org.dspace.saml.GIVEN_NAME", "Liz");
        request.setAttribute("org.dspace.saml.SURNAME", "Hacker-Bitdiddle");

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.SUCCESS, result);

        EPerson user = context.getCurrentUser();

        assertNotNull(user);
        assertEquals("alyssa@dspace.org", user.getEmail());
        assertEquals("001", user.getNetid());
        assertEquals("Liz", user.getFirstName());
        assertEquals("Hacker-Bitdiddle", user.getLastName());
    }

    @Test
    public void testAuthenticateExistingUserAdditionalMetadata() throws Exception {
        configurationService.setProperty("authentication-saml.eperson.metadata",
            "org.dspace.saml.PHONE => phone," +
            "org.dspace.saml.NICKNAME => nickname");

        context.setCurrentUser(null);
        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("alyssa@dspace.org")
            .withNetId("001")
            .withNameInMetadata("Alyssa", "Hacker")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        request.setAttribute("org.dspace.saml.NAME_ID", "001");
        request.setAttribute("org.dspace.saml.PHONE", "123-456-7890");
        request.setAttribute("org.dspace.saml.NICKNAME", "Liz");

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.SUCCESS, result);

        EPerson user = context.getCurrentUser();

        assertNotNull(user);
        assertEquals("alyssa@dspace.org", user.getEmail());
        assertEquals("001", user.getNetid());
        assertEquals("Alyssa", user.getFirstName());
        assertEquals("Hacker", user.getLastName());

        List<MetadataValue> metadata = user.getMetadata();

        assertEquals(4, metadata.size());
        assertEquals("eperson_phone", metadata.get(2).getMetadataField().toString());
        assertEquals("123-456-7890", metadata.get(2).getValue());
        assertEquals("eperson_nickname", metadata.get(3).getMetadataField().toString());
        assertEquals("Liz", metadata.get(3).getValue());
    }

    @Test
    public void testInvalidAdditionalMetadataMappingsAreIgnored() throws Exception {
        configurationService.setProperty("authentication-saml.eperson.metadata",
            "oops this is bad," +
            "org.dspace.saml.NICKNAME => nickname");

        context.setCurrentUser(null);
        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("alyssa@dspace.org")
            .withNetId("001")
            .withNameInMetadata("Alyssa", "Hacker")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        request.setAttribute("org.dspace.saml.NAME_ID", "001");
        request.setAttribute("org.dspace.saml.PHONE", "123-456-7890");
        request.setAttribute("org.dspace.saml.NICKNAME", "Liz");

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.SUCCESS, result);

        EPerson user = context.getCurrentUser();

        assertNotNull(user);
        assertEquals("alyssa@dspace.org", user.getEmail());
        assertEquals("001", user.getNetid());
        assertEquals("Alyssa", user.getFirstName());
        assertEquals("Hacker", user.getLastName());

        List<MetadataValue> metadata = user.getMetadata();

        assertEquals(3, metadata.size());
        assertEquals("eperson_nickname", metadata.get(2).getMetadataField().toString());
        assertEquals("Liz", metadata.get(2).getValue());
    }

    @Test
    public void testAuthenticateExistingUserAdditionalMetadataAutocreateDisabled() throws Exception {
        configurationService.setProperty("authentication-saml.eperson.metadata.autocreate", false);

        configurationService.setProperty("authentication-saml.eperson.metadata",
            "org.dspace.saml.PHONE => phone," +
            "org.dspace.saml.DEPARTMENT => department");

        context.setCurrentUser(null);
        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("alyssa@dspace.org")
            .withNetId("001")
            .withNameInMetadata("Alyssa", "Hacker")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        request.setAttribute("org.dspace.saml.NAME_ID", "001");
        request.setAttribute("org.dspace.saml.PHONE", "123-456-7890");
        request.setAttribute("org.dspace.saml.DEPARTMENT", "Library");

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.SUCCESS, result);

        EPerson user = context.getCurrentUser();

        assertNotNull(user);
        assertEquals("alyssa@dspace.org", user.getEmail());
        assertEquals("001", user.getNetid());
        assertEquals("Alyssa", user.getFirstName());
        assertEquals("Hacker", user.getLastName());

        List<MetadataValue> metadata = user.getMetadata();

        assertEquals(3, metadata.size());
        assertEquals("eperson_phone", metadata.get(2).getMetadataField().toString());
        assertEquals("123-456-7890", metadata.get(2).getValue());
    }

    @Test
    public void testAdditionalMetadataWithInvalidNameNotAutocreated() throws Exception {
        configurationService.setProperty("authentication-saml.eperson.metadata",
            "org.dspace.saml.PHONE => phone," +
            "org.dspace.saml.DEPARTMENT => (department)"); // parens not allowed

        context.setCurrentUser(null);
        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("alyssa@dspace.org")
            .withNetId("001")
            .withNameInMetadata("Alyssa", "Hacker")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        request.setAttribute("org.dspace.saml.NAME_ID", "001");
        request.setAttribute("org.dspace.saml.PHONE", "123-456-7890");
        request.setAttribute("org.dspace.saml.DEPARTMENT", "Library");

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.SUCCESS, result);

        EPerson user = context.getCurrentUser();

        assertNotNull(user);
        assertEquals("alyssa@dspace.org", user.getEmail());
        assertEquals("001", user.getNetid());
        assertEquals("Alyssa", user.getFirstName());
        assertEquals("Hacker", user.getLastName());

        List<MetadataValue> metadata = user.getMetadata();

        assertEquals(3, metadata.size());
        assertEquals("eperson_phone", metadata.get(2).getMetadataField().toString());
        assertEquals("123-456-7890", metadata.get(2).getValue());
    }

    @Test
    public void testExistingUserLoginDisabled() throws Exception {
        EPerson originalUser = context.getCurrentUser();

        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("alyssa@dspace.org")
            .withNameInMetadata("Alyssa", "Hacker")
            .withCanLogin(false)
            .build();

        context.restoreAuthSystemState();

        request.setAttribute("org.dspace.saml.EMAIL", List.of("alyssa@dspace.org"));

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.BAD_ARGS, result);
        assertEquals(originalUser, context.getCurrentUser());
    }

    @Test
    public void testNonExistentUserWithoutEmail() throws Exception {
        EPerson originalUser = context.getCurrentUser();

        request.setAttribute("org.dspace.saml.NAME_ID", "non-existent-netid");

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.NO_SUCH_USER, result);
        assertEquals(originalUser, context.getCurrentUser());
    }

    @Test
    public void testNonExistentUserWithEmailAutoregisterEnabled() throws Exception {
        context.setCurrentUser(null);

        request.setAttribute("org.dspace.saml.NAME_ID", "non-existent-netid");
        request.setAttribute("org.dspace.saml.EMAIL", List.of("ben@dspace.org"));
        request.setAttribute("org.dspace.saml.GIVEN_NAME", "Ben");
        request.setAttribute("org.dspace.saml.SURNAME", "Bitdiddle");

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.SUCCESS, result);

        EPerson user = context.getCurrentUser();

        assertNotNull(user);
        assertEquals("ben@dspace.org", user.getEmail());
        assertEquals("non-existent-netid", user.getNetid());
        assertEquals("Ben", user.getFirstName());
        assertEquals("Bitdiddle", user.getLastName());
        assertTrue(user.canLogIn());
        assertTrue(user.getSelfRegistered());

        testUser = user; // Make sure the autoregistered user gets deleted.
    }

    @Test
    public void testNonExistentUserWithEmailAutoregisterDisabled() throws Exception {
        configurationService.setProperty("authentication-saml.autoregister", false);

        EPerson originalUser = context.getCurrentUser();

        request.setAttribute("org.dspace.saml.NAME_ID", "non-existent-netid");
        request.setAttribute("org.dspace.saml.EMAIL", List.of("ben@dspace.org"));
        request.setAttribute("org.dspace.saml.GIVEN_NAME", "Ben");
        request.setAttribute("org.dspace.saml.SURNAME", "Bitdiddle");

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.NO_SUCH_USER, result);
        assertEquals(originalUser, context.getCurrentUser());
    }

    @Test
    public void testNoEmailOrNameIdInRequest() throws Exception {
        context.setCurrentUser(null);
        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("alyssa@dspace.org")
            .withNameInMetadata("Alyssa", "Hacker")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        int result = samlAuth.authenticate(context, null, null, null, request);

        assertEquals(AuthenticationMethod.NO_SUCH_USER, result);
    }

    @Test
    public void testRequestIsNull() throws Exception {
        EPerson originalUser = context.getCurrentUser();

        int result = samlAuth.authenticate(context, null, null, null, null);

        assertEquals(AuthenticationMethod.BAD_ARGS, result);
        assertEquals(originalUser, context.getCurrentUser());
    }
}
