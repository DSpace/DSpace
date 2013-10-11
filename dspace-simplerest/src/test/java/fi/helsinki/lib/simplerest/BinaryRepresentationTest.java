/**
 * A RESTful web service on top of DSpace.
 * Copyright (C) 2010-2013 National Library of Finland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
