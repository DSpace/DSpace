/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
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

    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

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
            thumb = bitstreamService.create(context, new FileInputStream(f));
            orig = bitstreamService.create(context, new FileInputStream(f));
            t = new Thumbnail(thumb, orig);
        }
        catch (IOException ex) {
            log.error("IO Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
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