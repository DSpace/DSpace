/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.saml2;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.dspace.AbstractDSpaceTest;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration.AssertingPartyDetails;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

public class DSpaceRelyingPartyRegistrationRepositoryTest extends AbstractDSpaceTest {
    private static ConfigurationService configurationService;

    @BeforeClass
    public static void beforeAll() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Before
    public void beforeEach() {
        resetConfigurationService();
    }

    @Test
    public void testConfigureAssertingPartyFromMetadata() throws Exception {
        configurationService.setProperty(
          "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);

        AssertingPartyDetails assertingPartyDetails = registration.getAssertingPartyDetails();

        assertNotNull(assertingPartyDetails);
        assertEquals("urn:dev-vynkcnqhac3c0s10.us.auth0.com", assertingPartyDetails.getEntityId());

        assertEquals("https://dev-vynkcnqhac3c0s10.us.auth0.com/samlp/Vn8jWX0iFHtepmXi7rjZa9h5M1kqXNWY/logout",
            assertingPartyDetails.getSingleLogoutServiceLocation());

        assertEquals(Saml2MessageBinding.REDIRECT, assertingPartyDetails.getSingleLogoutServiceBinding());

        assertEquals("https://dev-vynkcnqhac3c0s10.us.auth0.com/samlp/Vn8jWX0iFHtepmXi7rjZa9h5M1kqXNWY/logout",
            assertingPartyDetails.getSingleLogoutServiceResponseLocation());

        assertEquals("https://dev-vynkcnqhac3c0s10.us.auth0.com/samlp/Vn8jWX0iFHtepmXi7rjZa9h5M1kqXNWY",
            assertingPartyDetails.getSingleSignOnServiceLocation());

        assertEquals(Saml2MessageBinding.REDIRECT, assertingPartyDetails.getSingleSignOnServiceBinding());
        assertFalse(assertingPartyDetails.getWantAuthnRequestsSigned());
        assertNotNull(assertingPartyDetails.getVerificationX509Credentials());
        assertEquals(1, assertingPartyDetails.getVerificationX509Credentials().size());

        X509Certificate cert = assertingPartyDetails.getVerificationX509Credentials().stream()
            .findFirst().get().getCertificate();

        assertNotNull(cert);
        assertEquals("SHA256withRSA", cert.getSigAlgName());
        assertEquals("CN=dev-vynkcnqhac3c0s10.us.auth0.com", cert.getSubjectDN().toString());
    }

    @Test
    public void testConfigureAssertingPartyWithMetadataOverrides() throws Exception {
        configurationService.setProperty(
          "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        configurationService.setProperty("saml-relying-party.auth0.asserting-party.entity-id", "my-entity-id");
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-sign-on.url", "http://my.idp.org/sso");
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-sign-on.binding", "post");
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-sign-on.sign-request", true);
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-logout.url", "http://my.idp.org/slo");
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-logout.binding", "post");
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-logout.response-url", "http://my.idp.org/slo-response");

        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.verification.credentials.0.certificate-location",
            "classpath:auth0-ap-override-certificate.crt");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);

        AssertingPartyDetails assertingPartyDetails = registration.getAssertingPartyDetails();

        assertNotNull(assertingPartyDetails);
        assertEquals("my-entity-id", assertingPartyDetails.getEntityId());
        assertEquals("http://my.idp.org/slo", assertingPartyDetails.getSingleLogoutServiceLocation());
        assertEquals(Saml2MessageBinding.POST, assertingPartyDetails.getSingleLogoutServiceBinding());
        assertEquals("http://my.idp.org/slo-response", assertingPartyDetails.getSingleLogoutServiceResponseLocation());
        assertEquals("http://my.idp.org/sso", assertingPartyDetails.getSingleSignOnServiceLocation());
        assertEquals(Saml2MessageBinding.POST, assertingPartyDetails.getSingleSignOnServiceBinding());
        assertTrue(assertingPartyDetails.getWantAuthnRequestsSigned());
        assertNotNull(assertingPartyDetails.getVerificationX509Credentials());
        assertEquals(1, assertingPartyDetails.getVerificationX509Credentials().size());

        X509Certificate cert = assertingPartyDetails.getVerificationX509Credentials().stream()
            .findFirst().get().getCertificate();

        assertNotNull(cert);
        assertEquals("SHA256withRSA", cert.getSigAlgName());

        assertEquals(
            "EMAILADDRESS=fhanik@pivotal.io, CN=simplesamlphp.cfapps.io, OU=IT, " +
                "O=Saml Testing Server, L=Castle Rock, ST=CO, C=US",
            cert.getSubjectDN().toString());
    }

    @Test
    public void testConfigureAssertingPartyWithoutMetadata() throws Exception {
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.entity-id", "my-entity-id");
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-sign-on.url", "http://my.idp.org/sso");
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-sign-on.binding", "post");
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-sign-on.sign-request", true);
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-logout.url", "http://my.idp.org/slo");
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-logout.binding", "post");
        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-logout.response-url", "http://my.idp.org/slo-response");

        configurationService.setProperty(
            // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
            "saml-relying-party.auth0.asserting-party.verification.credentials.0.certificate-location",
            "classpath:auth0-ap-override-certificate.crt");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);

        AssertingPartyDetails assertingPartyDetails = registration.getAssertingPartyDetails();

        assertNotNull(assertingPartyDetails);
        assertEquals("my-entity-id", assertingPartyDetails.getEntityId());
        assertEquals("http://my.idp.org/slo", assertingPartyDetails.getSingleLogoutServiceLocation());
        assertEquals(Saml2MessageBinding.POST, assertingPartyDetails.getSingleLogoutServiceBinding());
        assertEquals("http://my.idp.org/slo-response", assertingPartyDetails.getSingleLogoutServiceResponseLocation());
        assertEquals("http://my.idp.org/sso", assertingPartyDetails.getSingleSignOnServiceLocation());
        assertEquals(Saml2MessageBinding.POST, assertingPartyDetails.getSingleSignOnServiceBinding());
        assertTrue(assertingPartyDetails.getWantAuthnRequestsSigned());
        assertNotNull(assertingPartyDetails.getVerificationX509Credentials());
        assertEquals(1, assertingPartyDetails.getVerificationX509Credentials().size());

        X509Certificate cert = assertingPartyDetails.getVerificationX509Credentials().stream()
            .findFirst().get().getCertificate();

        assertNotNull(cert);
        assertEquals("SHA256withRSA", cert.getSigAlgName());

        assertEquals(
            "EMAILADDRESS=fhanik@pivotal.io, CN=simplesamlphp.cfapps.io, OU=IT, " +
                "O=Saml Testing Server, L=Castle Rock, ST=CO, C=US",
            cert.getSubjectDN().toString());
    }

    @Test
    public void testConfigureRelyingPartySigningCredentials() throws Exception {
        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        configurationService.setProperty("saml-relying-party.auth0.signing.credentials.0.private-key-location",
            "classpath:auth0-rp-private.key");

        configurationService.setProperty("saml-relying-party.auth0.signing.credentials.0.certificate-location",
            "classpath:auth0-rp-certificate.crt");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);
        assertNotNull(registration.getSigningX509Credentials());
        assertEquals(1, registration.getSigningX509Credentials().size());

        Saml2X509Credential credential = registration.getSigningX509Credentials().stream()
            .findFirst().get();

        X509Certificate cert = credential.getCertificate();

        assertNotNull(cert);
        assertEquals("SHA256withRSA", cert.getSigAlgName());

        assertEquals(
            "CN=sp.spring.security.saml, OU=sp, O=Spring Security SAML, L=Vancouver, ST=Washington, C=US",
            cert.getSubjectDN().toString());

        PrivateKey key = credential.getPrivateKey();

        assertNotNull(key);
        assertEquals("RSA", key.getAlgorithm());
        assertEquals("PKCS#8", key.getFormat());
    }

    @Test
    public void testConfigureRelyingPartyDecryptionCredentials() throws Exception {
        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        configurationService.setProperty("saml-relying-party.auth0.decryption.credentials.0.private-key-location",
            "classpath:auth0-rp-private.key");

        configurationService.setProperty("saml-relying-party.auth0.decryption.credentials.0.certificate-location",
            "classpath:auth0-rp-certificate.crt");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);
        assertNotNull(registration.getDecryptionX509Credentials());
        assertEquals(1, registration.getDecryptionX509Credentials().size());

        Saml2X509Credential credential = registration.getDecryptionX509Credentials().stream()
            .findFirst().get();

        X509Certificate cert = credential.getCertificate();

        assertNotNull(cert);
        assertEquals("SHA256withRSA", cert.getSigAlgName());

        assertEquals(
            "CN=sp.spring.security.saml, OU=sp, O=Spring Security SAML, L=Vancouver, ST=Washington, C=US",
            cert.getSubjectDN().toString());

        PrivateKey key = credential.getPrivateKey();

        assertNotNull(key);
        assertEquals("RSA", key.getAlgorithm());
        assertEquals("PKCS#8", key.getFormat());
    }

    @Test
    public void testConfigureRelyingPartyInvalidBoolean() throws Exception {
        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        configurationService.setProperty("saml-relying-party.auth0.asserting-party.single-sign-on.sign-request",
            "not a boolean");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNull(registration);
    }

    @Test
    public void testConfigureRelyingPartyCredentialsMissingKey() throws Exception {
        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        configurationService.setProperty("saml-relying-party.auth0.decryption.credentials.0.certificate-location",
            "classpath:auth0-rp-certificate.crt");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);
        assertNotNull(registration.getDecryptionX509Credentials());
        assertEquals(0, registration.getDecryptionX509Credentials().size());
    }

    @Test
    public void testConfigureRelyingPartyCredentialsKeyLocationNotFound() throws Exception {
        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        configurationService.setProperty("saml-relying-party.auth0.decryption.credentials.0.private-key-location",
            "classpath:does-not-exist.key");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);
        assertNotNull(registration.getDecryptionX509Credentials());
        assertEquals(0, registration.getDecryptionX509Credentials().size());
    }

    @Test
    public void testConfigureRelyingPartyCredentialsKeyFileInvalid() throws Exception {
        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        configurationService.setProperty("saml-relying-party.auth0.decryption.credentials.0.private-key-location",
            "classpath:invalid.key");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);
        assertNotNull(registration.getDecryptionX509Credentials());
        assertEquals(0, registration.getDecryptionX509Credentials().size());
    }

    @Test
    public void testConfigureRelyingPartyCredentialsMissingCertificate() throws Exception {
        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        configurationService.setProperty("saml-relying-party.auth0.decryption.credentials.0.private-key-location",
            "classpath:auth0-rp-private.key");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);
        assertNotNull(registration.getDecryptionX509Credentials());
        assertEquals(0, registration.getDecryptionX509Credentials().size());
    }

    @Test
    public void testConfigureRelyingPartyCredentialsCertificateLocationNotFound() throws Exception {
        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        configurationService.setProperty("saml-relying-party.auth0.decryption.credentials.0.certificate-location",
            "classpath:does-not-exist.crt");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);
        assertNotNull(registration.getDecryptionX509Credentials());
        assertEquals(0, registration.getDecryptionX509Credentials().size());
    }

    @Test
    public void testConfigureRelyingPartyCredentialsCertificateFileInvalid() throws Exception {
        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

         configurationService.setProperty("saml-relying-party.auth0.decryption.credentials.0.certificate-location",
            "classpath:invalid.crt");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);
        assertNotNull(registration.getDecryptionX509Credentials());
        assertEquals(0, registration.getDecryptionX509Credentials().size());
    }

    @Test
    public void testConfigureRelyingPartyMissingAssertingParty() throws Exception {
        configurationService.setProperty("saml-relying-party.auth0.signing.credentials.0.private-key-location",
            "classpath:auth0-rp-private.key");

        configurationService.setProperty("saml-relying-party.auth0.signing.credentials.0.certificate-location",
            "classpath:auth0-rp-certificate.crt");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNull(registration);
    }

    @Test
    public void testConfigureRelyingPartyCredentialsFromFileUrls() throws Exception {
        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        // Windows requires three slashes in a file URL.  Linux/unix does not.
        String fileUrlPrefix = IS_OS_WINDOWS ? "file:///" : "file://";

        configurationService.setProperty("saml-relying-party.auth0.signing.credentials.0.private-key-location",
            fileUrlPrefix + new ClassPathResource("auth0-rp-private.key").getFile().getAbsolutePath());

        configurationService.setProperty("saml-relying-party.auth0.signing.credentials.0.certificate-location",
            fileUrlPrefix + new ClassPathResource("auth0-rp-certificate.crt").getFile().getAbsolutePath());

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);
        assertNotNull(registration.getSigningX509Credentials());
        assertEquals(1, registration.getSigningX509Credentials().size());

        Saml2X509Credential credential = registration.getSigningX509Credentials().stream()
            .findFirst().get();

        X509Certificate cert = credential.getCertificate();

        assertNotNull(cert);
        assertEquals("SHA256withRSA", cert.getSigAlgName());

        assertEquals(
            "CN=sp.spring.security.saml, OU=sp, O=Spring Security SAML, L=Vancouver, ST=Washington, C=US",
            cert.getSubjectDN().toString());

        PrivateKey key = credential.getPrivateKey();

        assertNotNull(key);
        assertEquals("RSA", key.getAlgorithm());
        assertEquals("PKCS#8", key.getFormat());
    }

    @Test
    public void testConfigureRelyingPartyCredentialsFromMalformedUrls() throws Exception {
        configurationService.setProperty(
            "saml-relying-party.auth0.asserting-party.metadata-uri", "classpath:auth0-ap-metadata.xml");

        configurationService.setProperty("saml-relying-party.auth0.signing.credentials.0.private-key-location",
            "xyz://auth0-rp-p rivate.key"); //oops

        configurationService.setProperty("saml-relying-party.auth0.signing.credentials.0.certificate-location",
            "classpath:auth0-rp-certificate.crt");

        DSpaceRelyingPartyRegistrationRepository repo = new DSpaceRelyingPartyRegistrationRepository();
        RelyingPartyRegistration registration = repo.findByRegistrationId("auth0");

        assertNotNull(registration);
        assertNotNull(registration.getSigningX509Credentials());
        assertEquals(0, registration.getSigningX509Credentials().size());
    }

    private void resetConfigurationService() {
        ((DSpaceConfigurationService) configurationService).clear();

        configurationService.reloadConfig();
    }
}
