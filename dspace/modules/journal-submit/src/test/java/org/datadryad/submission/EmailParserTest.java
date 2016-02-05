package org.datadryad.submission;

import static org.junit.Assert.*;
import junit.framework.TestCase;
import org.datadryad.rest.models.Author;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;


/**
 * JUnit test for EmailParser.
 * 
 * @author Peter E. Midford <peter.midford@gmail.com>
 */

public class EmailParserTest extends TestCase{

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testProcessKeywordList(){
        List<String> test1 = EmailParser.parseClassificationList("Ecology: evolutionary; Evolution: social; Foraging: social; Foraging: theory; Methods: computer simulations;");
        assertNotNull(test1);
        assertEquals(5,test1.size());
        assertEquals("Ecology: evolutionary",test1.get(0));
        assertEquals("Evolution: social",test1.get(1));
        assertEquals("Foraging: social",test1.get(2));
        assertEquals("Foraging: theory",test1.get(3));
        assertEquals("Methods: computer simulations",test1.get(4));
        test1 = EmailParser.parseClassificationList("Extinction, reproductive fitness, negative selection, devolution, genetic variation , heterosis, embryonic abortion, norm of reaction, habitat attenuation, habitat dissolution");
        assertNotNull(test1);
        assertEquals(10,test1.size());
        assertEquals("Extinction",test1.get(0));
        assertEquals("reproductive fitness",test1.get(1));
        assertEquals("negative selection",test1.get(2));
        assertEquals("devolution",test1.get(3));
        assertEquals("genetic variation",test1.get(4));
        assertEquals("heterosis",test1.get(5));
        assertEquals("embryonic abortion",test1.get(6));
        assertEquals("norm of reaction",test1.get(7));
        assertEquals("habitat attenuation",test1.get(8));
        assertEquals("habitat dissolution",test1.get(9));
    }
    
    @Test
    public void testProcessAuthorList() {
        List<Author> test1 = EmailParser.parseAuthorList("Pawel Lichocki; Dr. Danesh Tarapore; Laurent Keller, PhD; Dario Floreano, Ph.D.");
        assertNotNull(test1);
        for(Author a : test1) {
            System.out.println("test1 has " + a.fullName());
        }
        assertEquals(4,test1.size());
        assertEquals("Lichocki, Pawel",test1.get(0).fullName());
        assertEquals("Tarapore, Danesh", test1.get(1).fullName());
        assertEquals("Keller, Laurent", test1.get(2).fullName());
        assertEquals("Floreano, Dario", test1.get(3).fullName());
        test1 = EmailParser.parseAuthorList("Wiens, Delbert; Slaton, Michele");
        assertNotNull(test1);
        for(Author a : test1) {
            System.out.println("test1 has " + a.fullName());
        }
        assertEquals(2,test1.size());
        assertEquals("Wiens, Delbert", test1.get(0).fullName());
        assertEquals("Slaton, Michele",test1.get(1).fullName());
        test1 = EmailParser.parseAuthorList("Thierry Brevault");
        assertNotNull(test1);
        for(Author a : test1) {
            System.out.println("test1 has " + a.fullName());
        }
        assertEquals(1,test1.size());
        assertEquals("Brevault, Thierry",test1.get(0).fullName());
        test1 = EmailParser.parseAuthorList("Riou, Samuel; Combreau, Olivier; Judas, Jacky; Lawrence, Mark; Pitra, Christian");
        assertNotNull(test1);
        for(Author a : test1) {
            System.out.println("test1 has " + a.fullName());
        }
        assertEquals(5, test1.size());
        assertEquals("Riou, Samuel",test1.get(0).fullName());
        assertEquals("Combreau, Olivier",test1.get(1).fullName());
        assertEquals("Judas, Jacky", test1.get(2).fullName());
        assertEquals("Lawrence, Mark",test1.get(3).fullName());
        assertEquals("Pitra, Christian",test1.get(4).fullName());
        test1 = EmailParser.parseAuthorList("Veselin Kostadinov");
        assertNotNull(test1);
        for(Author a : test1) {
            System.out.println("test1 has " + a.fullName());
        }
        assertEquals(1,test1.size());
        assertEquals("Kostadinov, Veselin",test1.get(0).fullName());
        test1 = EmailParser.parseAuthorList("Elizabeth Garrett, Frances Parker, and Winslow Parker");
        assertNotNull(test1);
        for(Author a : test1) {
            System.out.println("test1 has " + a.fullName());
        }
        assertEquals(3,test1.size());
        assertEquals("Garrett, Elizabeth",test1.get(0).fullName());
        assertEquals("Parker, Frances",test1.get(1).fullName());
        assertEquals("Parker, Winslow", test1.get(2).fullName());
        test1 = EmailParser.parseAuthorList("J. David");
        assertNotNull(test1);
        for(Author a : test1) {
            System.out.println("test1 has " + a.fullName());
        }
        assertEquals(1,test1.size());
        assertEquals("David, J.",test1.get(0).fullName());
        test1 = EmailParser.parseAuthorList("Lacy, Robert");
        assertNotNull(test1);
        for(Author a : test1) {
            System.out.println("test1 has " + a.fullName());
        }
        assertEquals(1,test1.size());
        assertEquals("Lacy, Robert",test1.get(0).fullName());
        test1 = EmailParser.parseAuthorList("Bono, Sonny; Cher");
        assertNotNull(test1);
        for(Author a : test1) {
            System.out.println("test1 has " + a.fullName());
        }
        assertEquals(2,test1.size());
        assertEquals("Bono, Sonny",test1.get(0).fullName());
        assertEquals("Cher",test1.get(1).fullName());
        test1 = EmailParser.parseAuthorList("Sonny Bono and Cher");
        assertNotNull(test1);
        for(Author a : test1) {
            System.out.println("test1 has " + a.fullName());
        }
        assertEquals(2,test1.size());
        assertEquals("Bono, Sonny",test1.get(0).fullName());
        assertEquals("Cher",test1.get(1).fullName());
    }


}
