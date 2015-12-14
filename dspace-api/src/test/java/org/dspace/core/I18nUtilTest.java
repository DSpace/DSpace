/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.core;

import java.util.Locale;
import mockit.Expectations;
import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author mwood
 */
public class I18nUtilTest extends AbstractDSpaceTest
{

    public I18nUtilTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of getDefaultLocale method, of class I18nUtil.
     */
/*
    @Test
    public void testGetDefaultLocale()
    {
        System.out.println("getDefaultLocale");
        Locale expResult = null;
        Locale result = I18nUtil.getDefaultLocale();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getEPersonLocale method, of class I18nUtil.
     */
/*
    @Test
    public void testGetEPersonLocale()
    {
        System.out.println("getEPersonLocale");
        EPerson ep = null;
        Locale expResult = null;
        Locale result = I18nUtil.getEPersonLocale(ep);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getSupportedLocales method, of class I18nUtil.
     */
/*
    @Test
    public void testGetSupportedLocales()
    {
        System.out.println("getSupportedLocales");
        Locale[] expResult = null;
        Locale[] result = I18nUtil.getSupportedLocales();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getSupportedLocale method, of class I18nUtil.
     */
/*
    @Test
    public void testGetSupportedLocale()
    {
        System.out.println("getSupportedLocale");
        Locale locale = null;
        Locale expResult = null;
        Locale result = I18nUtil.getSupportedLocale(locale);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getInputFormsFileName method, of class I18nUtil.
     */
/*
    @Test
    public void testGetInputFormsFileName()
    {
        System.out.println("getInputFormsFileName");
        Locale locale = null;
        String expResult = "";
        String result = I18nUtil.getInputFormsFileName(locale);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getMessage method, of class I18nUtil.
     */
    @Test
    public void testGetMessage_String()
    {
        System.out.println("getMessage");
        final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        
        // Override "default.locale" and ensure it is set to US English
        new Expectations(configService.getClass()) {{
            configService.getProperty("default.locale"); result = "en_US.UTF-8";
        }};

        // Assert our overridden default.locale is set in I18nUtil
        assertEquals("Default locale", new Locale("en", "US", "UTF-8"), I18nUtil.getDefaultLocale());
        
        String key, expResult, result;

        // Test for a stock key
        key = "jsp.general.home";
        expResult = "DSpace Home";
        result = I18nUtil.getMessage(key);
        assertEquals("Returns the translation of the key if it is defined",
                expResult, result);

        // Test for a missing key
        key = expResult = "bogus key";
        result = I18nUtil.getMessage(key);
        assertEquals("Returns the key if it is not defined", expResult, result);
    }

    /**
     * Test of getMessage method, of class I18nUtil.
     */
    @Test
    public void testGetMessage_String_Locale()
    {
        System.out.println("getMessage");
        String key, expResult, result;
        Locale locale = Locale.US;

        // Test for a stock key
        key = "jsp.general.home";
        expResult = "DSpace Home";
        result = I18nUtil.getMessage(key, locale);
        assertEquals("Returns the translation of the key if it is defined",
                expResult, result);

        // Test for a missing key
        key = expResult = "bogus key";
        result = I18nUtil.getMessage(key, locale);
        assertEquals("Returns the key if it is not defined", expResult, result);
    }

    /**
     * Test of getMessage method, of class I18nUtil.
     */
/*
    @Test
    public void testGetMessage_String_Context()
            throws SQLException
    {
        System.out.println("getMessage");
        String key, expResult, result;
        Context c = new Context();
        c.setCurrentLocale(Locale.US);

        // Test for a stock key
        key = "jsp.general.home";
        expResult = "DSpace Home";
        result = I18nUtil.getMessage(key, c);
        assertEquals("Returns the translation of the key if it is defined",
                expResult, result);

        // Test for a missing key
        key = expResult = "bogus key";
        result = I18nUtil.getMessage(key, c);
        assertEquals("Returns the key if it is not defined", expResult, result);
    }
*/

    /**
     * Test of getDefaultLicense method, of class I18nUtil.
     */
/*
    @Test
    public void testGetDefaultLicense()
    {
        System.out.println("getDefaultLicense");
        Context context = null;
        String expResult = "";
        String result = I18nUtil.getDefaultLicense(context);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getEmailFilename method, of class I18nUtil.
     */
/*
    @Test
    public void testGetEmailFilename()
    {
        System.out.println("getEmailFilename");
        Locale locale = null;
        String name = "";
        String expResult = "";
        String result = I18nUtil.getEmailFilename(locale, name);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of parseLocales method, of class I18nUtil.
     */
/*
    @Test
    public void testParseLocales()
    {
        System.out.println("parseLocales");
        String ll = "";
        Locale[] expResult = null;
        Locale[] result = I18nUtil.parseLocales(ll);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
}
