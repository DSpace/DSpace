/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.statistics.configuration;

import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author mwood
 */
public class StatisticsSpringLoaderTest
{
    public StatisticsSpringLoaderTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
            throws Exception
    {
        DSpaceKernelInit.getKernel(null).start();
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

    /**
     * Test of getResourcePaths method, of class StatisticsSpringLoader.
     */
    @Test
    public void testGetResourcePaths()
    {
        System.out.println("getResourcePaths");

        ConfigurationService configurationService = new DSpace().getConfigurationService();

        StatisticsSpringLoader instance = new StatisticsSpringLoader();
        String[] result = instance.getResourcePaths(configurationService);
        assertNotNull("getResourcePaths returned null", result);
        assertEquals("Wrong number of paths returned", 1, result.length);
        for (int i = 0; i < result.length; i++)
        {
            System.out.printf("result[%d] = %s\n", i, result[i]);
        }
    }
}
