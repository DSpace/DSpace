/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.dspace.content.Item;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Drive the POI-based MS Word filter.
 * @author mwood
 */
public class PoiWordFilterTest
{

    public PoiWordFilterTest()
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

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of getFilteredName method, of class PoiWordFilter.
     */
/*
    @Test
    public void testGetFilteredName()
    {
        System.out.println("getFilteredName");
        String oldFilename = "";
        PoiWordFilter instance = new PoiWordFilter();
        String expResult = "";
        String result = instance.getFilteredName(oldFilename);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getBundleName method, of class PoiWordFilter.
     */
/*
    @Test
    public void testGetBundleName()
    {
        System.out.println("getBundleName");
        PoiWordFilter instance = new PoiWordFilter();
        String expResult = "";
        String result = instance.getBundleName();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getFormatString method, of class PoiWordFilter.
     */
/*
    @Test
    public void testGetFormatString()
    {
        System.out.println("getFormatString");
        PoiWordFilter instance = new PoiWordFilter();
        String expResult = "";
        String result = instance.getFormatString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getDescription method, of class PoiWordFilter.
     */
/*
    @Test
    public void testGetDescription()
    {
        System.out.println("getDescription");
        PoiWordFilter instance = new PoiWordFilter();
        String expResult = "";
        String result = instance.getDescription();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getDestinationStream method, of class PoiWordFilter.
     * Read a constant .doc document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamDoc()
            throws Exception
    {
        System.out.println("getDestinationStream");
        Item currentItem = null;
        InputStream source;
        boolean verbose = false;
        PoiWordFilter instance = new PoiWordFilter();
        InputStream result;

        source = getClass().getResourceAsStream("wordtest.doc");
        result = instance.getDestinationStream(currentItem, source, verbose);
        assertTrue("Known content was not found", readAll(result).contains("quick brown fox"));
    }

    /**
     * Test of getDestinationStream method, of class PoiWordFilter.
     * Read a constant .docx document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamDocx()
            throws Exception
    {
        System.out.println("getDestinationStream");
        Item currentItem = null;
        InputStream source;
        boolean verbose = false;
        PoiWordFilter instance = new PoiWordFilter();
        InputStream result;

        source = getClass().getResourceAsStream("wordtest.docx");
        result = instance.getDestinationStream(currentItem, source, verbose);
        assertTrue("Known content was not found", readAll(result).contains("quick brown fox"));
    }

    /**
     * Read the entire content of a stream into a String.
     *
     * @param stream a stream of UTF-8 characters.
     * @return complete content of {@link stream}
     * @throws IOException
     */
    private static String readAll(InputStream stream)
            throws IOException
    {
        if (null == stream) return null;

        byte[] bytes = new byte[stream.available()];
        StringBuilder resultSb = new StringBuilder(bytes.length/2); // Guess:  average 2 bytes per character
        int howmany;
        while((howmany = stream.read(bytes)) > 0)
        {
            resultSb.append(new String(bytes, 0, howmany, StandardCharsets.UTF_8));
        }
        return resultSb.toString();
    }
}
