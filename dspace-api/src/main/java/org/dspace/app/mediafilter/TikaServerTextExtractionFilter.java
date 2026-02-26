/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.InputStream;

import org.dspace.app.util.TikaServerAdapter;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Text Extraction media filter which uses Apache Tika Server.
 * Extracts text from a large number of file formats (including all Microsoft
 * formats, PDF, HTML, Text, etc).
 * 
 * For a more complete list of file formats supported by Tika see
 * <a href='https://tika.apache.org/2.3.0/formats.html'>the Tika documentation</a>.
 */
public class TikaServerTextExtractionFilter
        extends MediaFilter {
    private static final String C_MAX_CHARS = "textextractor.max-chars";

    private static final int DEFAULT_MAX_CHARS = 100_000;
    // private static final int DEFAULT_MAX_ARRAY = 1_000_000;

    @Override
    public String getFilteredName(String oldFilename) {
        return oldFilename + ".txt";
    }

    @Override
    public String getBundleName() {
        return "TEXT";
    }

    @Override
    public String getFormatString() {
        return "Text";
    }

    @Override
    public String getDescription() {
        return "Extracted text";
    }

    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
            throws Exception {
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();

        int maxChars = configurationService.getIntProperty(C_MAX_CHARS,
                DEFAULT_MAX_CHARS);

        return TikaServerAdapter.postStream("/tika", source, maxChars);
    }

    /**
     * Ask Tika for the flat text and feed it to a text handler.
     *
     * @param currentItem The Item which contains the Bitstream.
     * @param source the backing file for the Bitstream.
     * @param handler sink for the extracted text.
     * @param verbose not used.
     * @throws Exception passed through.
     */
    //@Override
    public void filter(Item currentItem, InputStream source,
            ExtractedTextHandler handler, boolean verbose)
            throws Exception {
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();

        int maxChars = configurationService.getIntProperty(C_MAX_CHARS,
                DEFAULT_MAX_CHARS);
        TikaServerAdapter.postStream("/tika", source, maxChars, handler);
    }
}
