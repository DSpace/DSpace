/*
 * ThumbnailTest.java
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import org.junit.*;
import static org.junit.Assert.* ;
import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;

/**
 * Unit Test for class Thumbnail. The class is a bean (just getters and setters)
 * so no specific tests are created.
 * @author pvillega
 */
public class ThumbnailTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(ThumbnailTest.class);

    /**
     * Bitstream instance for the tests, thumbnail copy
     */
    private Bitstream thumb;

    /**
     * Bitstream instance for the tests, original copy
     */
    private Bitstream orig;

    /**
     * Thumbnail instance for the tests, original copy
     */
    private Thumbnail t;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
        try
        {
            //we have to create a new bitstream in the database
            File f = new File(testProps.get("test.bitstream").toString());
            thumb = Bitstream.create(context, new FileInputStream(f));
            context.commit();
            orig = Bitstream.create(context, new FileInputStream(f));
            context.commit();
            t = new Thumbnail(thumb, orig);            
        }
        catch (IOException ex) {
            log.error("IO Error in init", ex);
            fail("SQL Error in init");
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init");
        }
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy()
    {
        thumb = null;
        orig = null;
        t = null;
        super.destroy();
    }

    /**
     * Dummy test to avoid initialization errors
     */
    @Test
    public void testDummy()
    {
        assertTrue(true);
    }
}