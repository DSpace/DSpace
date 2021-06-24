/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Test for class Thumbnail. The class is a bean (just getters and setters)
 * so no specific tests are created.
 *
 * @author pvillega
 */
public class ThumbnailTest extends AbstractUnitTest {

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ThumbnailTest.class);

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
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();
        try {
            //we have to create a new bitstream in the database
            File f = new File(testProps.get("test.bitstream").toString());
            thumb = bitstreamService.create(context, new FileInputStream(f));
            orig = bitstreamService.create(context, new FileInputStream(f));
            Thumbnail t = new Thumbnail(thumb, orig);
            assertEquals(orig, t.getOriginal());
            assertEquals(thumb, t.getThumb());
        } catch (IOException ex) {
            log.error("IO Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        } catch (SQLException ex) {
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
    public void destroy() {
        try {
            context.turnOffAuthorisationSystem();
            bitstreamService.delete(context, thumb);
            bitstreamService.delete(context, orig);
            context.restoreAuthSystemState();
            thumb = null;
            orig = null;
        } catch (Exception e) {
            throw new AssertionError("Error in destroy()", e);
        }
        super.destroy();
    }

    /**
     * Dummy test to avoid initialization errors
     */
    @Test
    public void testDummy() {
        assertTrue(true);
    }
}
