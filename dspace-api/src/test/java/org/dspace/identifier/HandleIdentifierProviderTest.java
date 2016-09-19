/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.dspace.AbstractDSpaceTest;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.*;

/**
 * Test the HandleIdentifierProvider.
 * <p>
 * We need to define a Bean for the System Under Test so that we can get Spring
 * to do autowiring on it.  There are several conflicting definitions of Handle
 * provider beans in the XML Spring configuration, so to test them all we'd have
 * to reconfigure between tests.  Instead, create a new definition just for
 * testing so that we know that we have the one that we want.
 *
 * @author mwood
 */
public class HandleIdentifierProviderTest
        extends AbstractDSpaceTest
{
    /** A name for our testing bean definition. */
    private static final String BEAN_NAME = "test-HandleIdentifierProvider";

    /** Spring application context. */
    private static AnnotationConfigApplicationContext applicationContext;

    public HandleIdentifierProviderTest()
    {
    }

    /**
     * The special test bean for the target class is defined here.
     */
    @BeforeClass
    public static void setUpClass()
    {
        ServiceManager serviceManager = kernelImpl.getServiceManager();

        // Get the normal ApplicationContext
        ApplicationContext parentApplicationContext
                = (ApplicationContext) serviceManager.getServiceByName(
                        ApplicationContext.class.getName(),
                        ApplicationContext.class);

        // Wrap it in a new empty context that we can configure.
        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.setParent(parentApplicationContext);
        applicationContext.setId("TestingContext");

        // Define our special bean for testing the target class.
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(HandleIdentifierProvider.class);
        bd.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
        bd.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
        applicationContext.registerBeanDefinition(BEAN_NAME, bd); // Now our SUT is a Bean.
        applicationContext.refresh();
    }

    /**
     * Clean up special test Spring stuff.
     */
    @AfterClass
    public static void tearDownClass()
    {
        // Clean up testing ApplicationContext and any beans within.
        applicationContext.close();
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of supports(Class) method, of class HandleIdentifierProvider.
     */
/*
    @Test
    public void testSupports_Class()
    {
        System.out.println("supports(Class)");
        Class<? extends Identifier> identifier = null;
        HandleIdentifierProvider instance = new HandleIdentifierProvider();
        boolean expResult = false;
        boolean result = instance.supports(identifier);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of supports(String) method, of class HandleIdentifierProvider.
     * Read a property list of identifiers and ask an instance of the provider
     * whether it supports each.  Properties are "identifier = true/false",
     * where the value indicates whether the identifier should be supported.
     * The list is a .properties on the class path.
     */
    @Test
    public void testSupports_String()
    {
        System.out.println("supports(String)");

        DSpaceServicesFactory.getInstance().getConfigurationService().setProperty("handle.prefix", "123456789");
        DSpaceServicesFactory.getInstance().getConfigurationService().setProperty("handle.additional.prefixes", "123456789.1,123456789.2");
        
        // We have to get Spring to instantiate the provider as a Bean, because
        // the bean class has autowired fields.
        HandleIdentifierProvider instance = new HandleIdentifierProvider();
        applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(
                instance, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);

        // Load the test cases
        Properties forms = new Properties();
        try {
            forms.load(this.getClass().getResourceAsStream("handle-forms.properties"));
        } catch (IOException e) {
            System.err.format("Could not load handle-forms.properties:  %s%n", e.getMessage());
            return;
        }

        // Test each case
        for (Map.Entry<Object, Object> entry : forms.entrySet())
        {
            String identifier = (String)entry.getKey();
            boolean expResult = Boolean.parseBoolean((String)entry.getValue());
            boolean result = instance.supports(identifier);
            String message = expResult ?
                    "This provider should support " + identifier :
                    "This provider should not support " + identifier;
            assertEquals(message, expResult, result);
        }
    }

    /**
     * Test of register method, of class HandleIdentifierProvider.
     */
/*
    @Test
    public void testRegister_Context_DSpaceObject()
    {
        System.out.println("register");
        Context context = null;
        DSpaceObject dso = null;
        HandleIdentifierProvider instance = new HandleIdentifierProvider();
        String expResult = "";
        String result = instance.register(context, dso);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of register method, of class HandleIdentifierProvider.
     */
/*
    @Test
    public void testRegister_3args()
    {
        System.out.println("register");
        Context context = null;
        DSpaceObject dso = null;
        String identifier = "";
        HandleIdentifierProvider instance = new HandleIdentifierProvider();
        instance.register(context, dso, identifier);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of reserve method, of class HandleIdentifierProvider.
     */
/*
    @Test
    public void testReserve()
    {
        System.out.println("reserve");
        Context context = null;
        DSpaceObject dso = null;
        String identifier = "";
        HandleIdentifierProvider instance = new HandleIdentifierProvider();
        instance.reserve(context, dso, identifier);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of mint method, of class HandleIdentifierProvider.
     */
/*
    @Test
    public void testMint()
    {
        System.out.println("mint");
        Context context = null;
        DSpaceObject dso = null;
        HandleIdentifierProvider instance = new HandleIdentifierProvider();
        String expResult = "";
        String result = instance.mint(context, dso);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of resolve method, of class HandleIdentifierProvider.
     */
/*
    @Test
    public void testResolve()
    {
        System.out.println("resolve");
        Context context = null;
        String identifier = "";
        String[] attributes = null;
        HandleIdentifierProvider instance = new HandleIdentifierProvider();
        DSpaceObject expResult = null;
        DSpaceObject result = instance.resolve(context, identifier, attributes);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of lookup method, of class HandleIdentifierProvider.
     * @throws java.lang.Exception passed through.
     */
/*
    @Test
    public void testLookup()
            throws Exception
    {
        System.out.println("lookup");
        Context context = null;
        DSpaceObject dso = null;
        HandleIdentifierProvider instance = new HandleIdentifierProvider();
        String expResult = "";
        String result = instance.lookup(context, dso);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of delete method, of class HandleIdentifierProvider.
     * @throws java.lang.Exception passed through.
     */
/*
    @Test
    public void testDelete_3args()
            throws Exception
    {
        System.out.println("delete");
        Context context = null;
        DSpaceObject dso = null;
        String identifier = "";
        HandleIdentifierProvider instance = new HandleIdentifierProvider();
        instance.delete(context, dso, identifier);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of delete method, of class HandleIdentifierProvider.
     * @throws java.lang.Exception passed through.
     */
/*
    @Test
    public void testDelete_Context_DSpaceObject()
            throws Exception
    {
        System.out.println("delete");
        Context context = null;
        DSpaceObject dso = null;
        HandleIdentifierProvider instance = new HandleIdentifierProvider();
        instance.delete(context, dso);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of retrieveHandleOutOfUrl method, of class HandleIdentifierProvider.
     * @throws java.lang.Exception passed through.
     */
/*
    @Test
    public void testRetrieveHandleOutOfUrl()
            throws Exception
    {
        System.out.println("retrieveHandleOutOfUrl");
        String url = "";
        String expResult = "";
        String result = HandleIdentifierProvider.retrieveHandleOutOfUrl(url);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getPrefix method, of class HandleIdentifierProvider.
     */
/*
    @Test
    public void testGetPrefix()
    {
        System.out.println("getPrefix");
        String expResult = "";
        String result = HandleIdentifierProvider.getPrefix();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of populateHandleMetadata method, of class HandleIdentifierProvider.
     * @throws java.lang.Exception passed through.
     */
/*
    @Test
    public void testPopulateHandleMetadata()
            throws Exception
    {
        System.out.println("populateHandleMetadata");
        Context context = null;
        Item item = null;
        String handle = "";
        HandleIdentifierProvider instance = new HandleIdentifierProvider();
        instance.populateHandleMetadata(context, item, handle);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
}
