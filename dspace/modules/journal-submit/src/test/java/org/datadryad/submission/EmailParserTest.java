package org.datadryad.submission;

import static org.junit.Assert.*;
import junit.framework.TestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


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
        String[] test1 = EmailParser.processKeywordList("Ecology: evolutionary; Evolution: social; Foraging: social; Foraging: theory; Methods: computer simulations;");
        assertNotNull(test1);
        assertEquals(5,test1.length);
        assertEquals("Ecology: evolutionary",test1[0]);
        assertEquals("Evolution: social",test1[1]);
        assertEquals("Foraging: social",test1[2]);
        assertEquals("Foraging: theory",test1[3]);
        assertEquals("Methods: computer simulations",test1[4]);
        test1 = EmailParser.processKeywordList("Extinction, reproductive fitness, negative selection, devolution, genetic variation , heterosis, embryonic abortion, norm of reaction, habitat attenuation, habitat dissolution");
        assertNotNull(test1);
        assertEquals(10,test1.length);
        assertEquals("Extinction",test1[0]);
        assertEquals("reproductive fitness",test1[1]);
        assertEquals("negative selection",test1[2]);
        assertEquals("devolution",test1[3]);
        assertEquals("genetic variation",test1[4]);
        assertEquals("heterosis",test1[5]);
        assertEquals("embryonic abortion",test1[6]);
        assertEquals("norm of reaction",test1[7]);
        assertEquals("habitat attenuation",test1[8]);
        assertEquals("habitat dissolution",test1[9]);
    }
    
    @Test
    public void testProcessAuthorList() {
        String[] test1 = EmailParser.processAuthorList("Pawel Lichocki; Danesh Tarapore, Dr.; Laurent Keller, Prof.; Dario Floreano, Prof.");
        assertNotNull(test1);
        assertEquals(4,test1.length);
        assertEquals("Pawel Lichocki",test1[0]);
        assertEquals("Danesh Tarapore, Dr.",test1[1]);
        assertEquals("Laurent Keller, Prof.",test1[2]);
        assertEquals("Dario Floreano, Prof.",test1[3]);
        test1 = EmailParser.processAuthorList("Wiens, Delbert; Slaton, Michele");
        assertNotNull(test1);
        assertEquals(2,test1.length);
        assertEquals("Wiens, Delbert",test1[0]);
        assertEquals("Slaton, Michele",test1[1]);
        test1 = EmailParser.processAuthorList("Awaya, Akira; MURAYAMA, Koji");
        assertNotNull(test1);
        assertEquals(2,test1.length);
        assertEquals("Awaya, Akira",test1[0]);
        assertEquals("MURAYAMA, Koji",test1[1]);
        test1 = EmailParser.processAuthorList("Thierry Brevault");
        assertNotNull(test1);
        assertEquals(1,test1.length);
        assertEquals("Brevault, Thierry",test1[0]);
        test1 = EmailParser.processAuthorList("Menzel, Florian; Schmitt, Thomas");
        assertNotNull(test1);
        assertEquals(2,test1.length);
        assertEquals("Menzel, Florian",test1[0]);
        assertEquals("Schmitt, Thomas",test1[1]);
        //Line break separated author lists work, but don't seem to be testable
//        test1 = EmailParser.processAuthorList("Wenhui Nie " +
//        "Jinhuan Wang " +
//        "Weiting Su " +
//        "Ding Wang " +
//        "Alongkoad Tanomtong " +
//        "Polina Perelman " +
//        "Alexander Graphodatsky" +
//        "Fengtang Yang");
//        assertNotNull(test1);
//        //assertEquals(8,test1.length);
//        for(String s : test1)
//            System.out.print(s + "|");
//        System.out.println();
        test1 = EmailParser.processAuthorList("Hodgins, Kathryn; Rieseberg, Loren");
        assertNotNull(test1);
        assertEquals(2,test1.length);
        assertEquals("Hodgins, Kathryn",test1[0]);
        assertEquals("Rieseberg, Loren",test1[1]);
        test1 = EmailParser.processAuthorList("Riou, Samuel; Combreau, Olivier; Judas, Jacky; Lawrence, Mark; Pitra, Christian");
        assertNotNull(test1);
        assertEquals(5,test1.length);
        assertEquals("Riou, Samuel",test1[0]);
        assertEquals("Combreau, Olivier",test1[1]);
        assertEquals("Judas, Jacky",test1[2]);
        assertEquals("Lawrence, Mark",test1[3]);
        assertEquals("Pitra, Christian",test1[4]);
        test1 = EmailParser.processAuthorList("Flight, Patrick; O'Brien, Megan; Schmidt, Paul; Rand, David");
        assertNotNull(test1);
        assertEquals(4,test1.length);
        assertEquals("Flight, Patrick",test1[0]);
        assertEquals("O&apos;Brien, Megan",test1[1]);
        assertEquals("Schmidt, Paul",test1[2]);
        assertEquals("Rand, David",test1[3]);
        test1 = EmailParser.processAuthorList("Loiseau, Claire; Harrigan, Ryan; Robert, Alexandre; Bowie, Rauri; Thomassen, Henri; Smith, Thomas; Sehgal, Ravinder");
        assertNotNull(test1);
        assertEquals(7,test1.length);
        assertEquals("Loiseau, Claire",test1[0]);
        assertEquals("Harrigan, Ryan",test1[1]);
        assertEquals("Robert, Alexandre",test1[2]);
        assertEquals("Bowie, Rauri",test1[3]);
        assertEquals("Thomassen, Henri",test1[4]);
        assertEquals("Smith, Thomas",test1[5]);
        assertEquals("Sehgal, Ravinder",test1[6]);
        test1 = EmailParser.processAuthorList("Yan, Chi; Sun, Genlou");
        assertNotNull(test1);
        assertEquals(2,test1.length);
        assertEquals("Yan, Chi",test1[0]);
        assertEquals("Sun, Genlou",test1[1]);
        test1 = EmailParser.processAuthorList("Veselin Kostadinov");
        assertNotNull(test1);
        assertEquals(1,test1.length);
        assertEquals("Kostadinov, Veselin",test1[0]);
        test1 = EmailParser.processAuthorList("Elizabeth Garrett, Frances Parker, and Winslow Parker");
        assertNotNull(test1);
        assertEquals(3,test1.length);
        assertEquals("Garrett, Elizabeth",test1[0]);
        assertEquals("Parker, Frances",test1[1]);
        assertEquals("Parker, Winslow",test1[2]);
        test1 = EmailParser.processAuthorList("J. David");
        assertNotNull(test1);
        assertEquals(1,test1.length);
        assertEquals("David, J.",test1[0]);
        test1 = EmailParser.processAuthorList("Micheal Head");
        assertNotNull(test1);
        assertEquals(1,test1.length);
        assertEquals("Head, Micheal",test1[0]);
        test1 = EmailParser.processAuthorList("David Lindenmayer, Jeff Wood, Lachlan McBurney, Damian Michael," + "\n" +
                                              "Mason Crane, Christopher MacGregor, Rebecca Montague-Drake, Philip Gibbons," + "\n" +
                                              "and Sam Banks");
        assertNotNull(test1);
        assertEquals(9,test1.length);
        assertEquals("Lindenmayer, David",test1[0]);
        assertEquals("Wood, Jeff",test1[1]);
        assertEquals("McBurney, Lachlan",test1[2]);
        assertEquals("Michael, Damian",test1[3]);
        assertEquals("Crane, Mason",test1[4]);
        assertEquals("MacGregor, Christopher",test1[5]);
        assertEquals("Montague-Drake, Rebecca",test1[6]);
        assertEquals("Gibbons, Philip",test1[7]);
        assertEquals("Banks, Sam",test1[8]);
        test1 = EmailParser.processAuthorList("Lacy, Robert");
        assertNotNull(test1);
        assertEquals(1,test1.length);
        assertEquals("Lacy, Robert",test1[0]);

    }


}
