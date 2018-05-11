/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.FileInputStream;
import java.io.File;

import org.dspace.AbstractUnitTest;
import org.apache.log4j.Logger;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

/**
 * Unit Tests for class FormatIdentifier
 * @author pvillega
 */
public class FormatIdentifierTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(FormatIdentifierTest.class);

    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();

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
        super.destroy();
    }

    /**
     * Test of guessFormat method, of class FormatIdentifier.
     */
    @Test
    public void testGuessFormat() throws Exception
    {
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = null; 
        BitstreamFormat result = null;
        BitstreamFormat pdf = bitstreamFormatService.findByShortDescription(context, "Adobe PDF");
        
        //test null filename
        //TODO: the check if filename is null is wrong, as it checks after using a toLowerCase
        //which can trigger the NPE
        bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, null);
        result = bitstreamFormatService.guessFormat(context, bs);
        assertThat("testGuessFormat 0",result, nullValue());

        //test unknown format
        bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, "file_without_extension.");
        result = bitstreamFormatService.guessFormat(context, bs);
        assertThat("testGuessFormat 1",result, nullValue());

        //test known format
        bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, testProps.get("test.bitstream").toString());
        result = bitstreamFormatService.guessFormat(context, bs);
        assertThat("testGuessFormat 2",result.getID(), equalTo(pdf.getID()));
        assertThat("testGuessFormat 3",result.getMIMEType(), equalTo(pdf.getMIMEType()));
        assertThat("testGuessFormat 4",result.getExtensions(), equalTo(pdf.getExtensions()));
    }

}