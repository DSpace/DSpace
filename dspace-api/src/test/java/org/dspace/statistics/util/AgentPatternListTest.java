/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.util.ArrayList;
import java.util.List;
import org.junit.*;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author mwood
 */
public class AgentPatternListTest
{
    
    public AgentPatternListTest()
    {
    }

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

    /**
     * Test of getPatterns method, of class AgentPatternList.
     */
    @Test
    public void testGetPatterns()
    {
        System.out.println("getPatterns");
        List<String> expResult = new ArrayList<String>();
        AgentPatternList instance = new AgentPatternList(expResult);
        List result = instance.getPatterns();
        assertEquals(expResult, result);
    }
}
