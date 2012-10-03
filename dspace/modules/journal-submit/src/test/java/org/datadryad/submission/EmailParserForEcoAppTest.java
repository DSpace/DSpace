package org.datadryad.submission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * JUnit test for EmailParserForEcoApp.
 * 
 * @author Peter E. Midford <peter.midford@gmail.com>
 */

public class EmailParserForEcoAppTest extends TestCase{
    
    static String refnumberLine = "MS reference Number: 08093";
    static String titleLine = "MS Title: Paleohistological estimation of bone growth rate in extinct archosaurs";
    static String keywordLine = "Keywords:  Paleohistology, Archosaurs, Crurotarsi, Ornithodira, Phylogeny";


    private static final List<String> keywordTest;
    static {
        keywordTest = new ArrayList<String>();
        keywordTest.add(refnumberLine);
        keywordTest.add(titleLine);
        keywordTest.add(keywordLine);
    }
    
    private static final Map<String, String> NESTED_ELEMENT_MAP;
    static {
        NESTED_ELEMENT_MAP = new HashMap<String, String>();
        NESTED_ELEMENT_MAP.put("contact author", "CorrespondingAuthor");
        NESTED_ELEMENT_MAP.put("ms reference number", "Manuscript");
        NESTED_ELEMENT_MAP.put("ms title", "ArticleTitle");
        NESTED_ELEMENT_MAP.put("keywords", "Classification");
    }


    EmailParserForEcoApp testParser;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        testParser = new EmailParserForEcoApp();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testParseMessage() {
        ParsingResult testResult = testParser.parseMessage(keywordTest);
        assertNotNull(testResult);
        assertNotNull(testResult.getSubmissionData());
        String test1 = testResult.getSubmissionData().toString();  //too bad this is already a StringBuffer...
        assertTrue(test1.contains("<Manuscript>08093</Manuscript>"));
        assertTrue(test1.contains("<keyword>Paleohistology</keyword>"));
        assertTrue(test1.contains("<keyword>Archosaurs</keyword>"));
        //System.out.println(testResult.getSubmissionData().toString());

    }

}
