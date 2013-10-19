/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/*
 * @(#)BinaryRepresentationTest.java
 */
package fi.helsinki.lib.simplerest;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.restlet.data.MediaType;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * JUnit test.
 * <ul>
 * <li>http://www.junit.org/</li>
 * <li>http://junit.sourceforge.net/doc/faq/faq.htm</li>
 * </ul>
 * Testing the methods of <code>BinaryRepresentation</code> class.
 * @author Markos Mevorah
 * @version %I%, %G%
 * @see fi.helsinki.lib.simplerest.BinaryRepresentation
 */
public class BinaryRepresentationTest {

    private byte[] buffer;
    /**
     * @see java.io.ByteArrayInputStream
     */
    private ByteArrayInputStream inputStream;
    /**
     * @see BinaryRepresentation
     */
    private BinaryRepresentation representation;

    public BinaryRepresentationTest() {
    }

    /**
     * JUnit method annotated with {@link org.junit.Before}.
     * Initializing the test resources.
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        this.buffer = new byte[]{1, 2, 3, 4};
        this.inputStream = new ByteArrayInputStream(this.buffer);
        this.representation = new BinaryRepresentation(MediaType.TEXT_PLAIN,
                                                       this.inputStream);
    }

    /**
     * JUnit method annotated with {@link org.junit.After}.
     * Releasing the test resources.
     */
    @After
    public void tearDown() {
        this.buffer = null;
        this.inputStream = null;
        this.representation = null;
    }

    /**
     * Test of write method, of class BinaryRepresentation.
     * @throws Exception
     */
    @Test
    public void testWrite() throws Exception {
        ByteArrayOutputStream outputStream =
                              new ByteArrayOutputStream(this.buffer.length);
        this.representation.write(outputStream);
        for (int i = 0; i < this.buffer.length; i++) {
            assertEquals(this.buffer[i], outputStream.toByteArray()[i]);
        }
    }
}
