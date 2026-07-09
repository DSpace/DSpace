/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileInputStream;

import org.dspace.AbstractUnitTest;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Tests for bitstream format identification (BitstreamFormatService#guessFormat).
 *
 * @author pvillega
 */
public class FormatIdentifierTest extends AbstractUnitTest {

    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
                                                                                   .getBitstreamFormatService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
                                                                               .getConfigurationService();

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
        // Reset any override of the content-based identification toggle
        configurationService.setProperty("bitstream.format.identification.by-content.enabled", null);
        super.destroy();
    }

    /**
     * Content-based identification (the default): the real format is detected from the
     * file content, even when the filename extension is missing, empty, or misleading.
     * The test bitstream ({@code test.bitstream}) is a PDF.
     */
    @Test
    public void testGuessFormatByContent() throws Exception {
        File f = new File(testProps.get("test.bitstream").toString());
        BitstreamFormat pdf = bitstreamFormatService.findByShortDescription(context, "Adobe PDF");

        // No filename at all: still detected as PDF from its content.
        Bitstream bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, null);
        BitstreamFormat result = bitstreamFormatService.guessFormat(context, bs);
        assertThat("content detection with null name is not null", result, notNullValue());
        assertThat("content detection with null name -> PDF", result.getID(), equalTo(pdf.getID()));

        // Filename without a usable extension: still detected as PDF from its content.
        bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, "file_without_extension.");
        result = bitstreamFormatService.guessFormat(context, bs);
        assertThat("content detection without extension is not null", result, notNullValue());
        assertThat("content detection without extension -> PDF", result.getID(), equalTo(pdf.getID()));

        // Deliberately misleading extension: content wins over the (wrong) extension.
        bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, "actually-a-pdf.txt");
        result = bitstreamFormatService.guessFormat(context, bs);
        assertThat("content beats a misleading extension is not null", result, notNullValue());
        assertThat("content beats a misleading extension -> PDF", result.getID(), equalTo(pdf.getID()));
    }

    /**
     * Legacy extension-based fallback (content identification disabled): the format is
     * guessed purely from the filename extension.
     */
    @Test
    public void testGuessFormatByExtensionFallback() throws Exception {
        configurationService.setProperty("bitstream.format.identification.by-content.enabled", false);

        File f = new File(testProps.get("test.bitstream").toString());
        BitstreamFormat pdf = bitstreamFormatService.findByShortDescription(context, "Adobe PDF");

        // Null filename: unknown (null), content is never inspected.
        Bitstream bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, null);
        assertThat("extension fallback, null name", bitstreamFormatService.guessFormat(context, bs), nullValue());

        // No usable extension: unknown (null).
        bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, "file_without_extension.");
        assertThat("extension fallback, no extension",
                   bitstreamFormatService.guessFormat(context, bs), nullValue());

        // A known extension is resolved from the registry (content is not verified).
        bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, "document.pdf");
        BitstreamFormat result = bitstreamFormatService.guessFormat(context, bs);
        assertThat("extension fallback, .pdf -> PDF", result.getID(), equalTo(pdf.getID()));
    }

}
