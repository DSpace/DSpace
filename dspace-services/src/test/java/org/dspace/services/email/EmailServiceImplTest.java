/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.services.email;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import org.dspace.services.ConfigurationService;
import org.dspace.services.EmailService;
import org.dspace.test.DSpaceAbstractKernelTest;
import org.junit.Test;

/**
 * @author mwood
 */
public class EmailServiceImplTest
    extends DSpaceAbstractKernelTest {
    private static final String USERNAME = "auser";
    private static final String PASSWORD = "apassword";

    /*
    @BeforeClass
    public static void setUpClass()
            throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass()
            throws Exception
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }
    */

    /**
     * Test of getSession method, of class EmailService.
     */
    @Test
    public void testGetSession()
        throws MessagingException {
        System.out.println("getSession");
        Session session;
        EmailService instance = getService(EmailServiceImpl.class);

        // Try to get a Session
        session = instance.getSession();
        assertNotNull(" getSession returned null", session);
        assertNull(" getSession returned authenticated session",
                session.getProperties().getProperty("mail.smtp.auth"));
    }

    private static final String CFG_USERNAME = "mail.server.username";
    private static final String CFG_PASSWORD = "mail.server.password";

    /**
     * Test of testGetSession method, of class EmailServiceImpl when an smtp
     * username is provided.
     */
    @Test
    public void testGetAuthenticatedInstance() {
        System.out.println("getSession");
        ConfigurationService cfg = getKernel().getConfigurationService();

        // Save existing values.
        String oldUsername = cfg.getProperty(CFG_USERNAME);
        String oldPassword = cfg.getProperty(CFG_PASSWORD);

        // Set known values.
        cfg.setProperty(CFG_USERNAME, USERNAME);
        cfg.setProperty(CFG_PASSWORD, PASSWORD);

        EmailServiceImpl instance = (EmailServiceImpl) getService(EmailServiceImpl.class);
        instance.reset();
        assertNotNull(" getSession returned null", instance);
        assertEquals(" authenticated session ", "true",
                instance.getSession().getProperties().getProperty("mail.smtp.auth"));

        // Restore old values, if any.
        cfg.setProperty(CFG_USERNAME, oldUsername);
        cfg.setProperty(CFG_PASSWORD, oldPassword);
        instance.reset();
    }

    /**
     * Test of getPasswordAuthentication method, of class EmailServiceImpl.
     */
    @Test
    public void testGetPasswordAuthentication() {
        System.out.println("getPasswordAuthentication");
        ConfigurationService cfg = getKernel().getConfigurationService();

        // Save existing values.
        String oldUsername = cfg.getProperty(CFG_USERNAME);
        String oldPassword = cfg.getProperty(CFG_PASSWORD);

        // Set known values.
        cfg.setProperty(CFG_USERNAME, USERNAME);
        cfg.setProperty(CFG_PASSWORD, PASSWORD);

        EmailServiceImpl instance = (EmailServiceImpl) getService(EmailServiceImpl.class);

        PasswordAuthentication result = instance.getPasswordAuthentication();
        assertNotNull(" null returned", result);
        assertEquals(" username does not match configuration", result.getUserName(), USERNAME);
        assertEquals(" password does not match configuration", result.getPassword(), PASSWORD);

        // Restore old values, if any.
        cfg.setProperty(CFG_USERNAME, oldUsername);
        cfg.setProperty(CFG_PASSWORD, oldPassword);
    }
}
