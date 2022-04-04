/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 * Text Extraction media filter which uses Apache Tika to extract text from a large number of file formats (including
 * all Microsoft formats, PDF, HTML, Text, etc).  For a more complete list of file formats supported by Tika see the
 * Tika documentation: https://tika.apache.org/2.3.0/formats.html
 */
public class TikaTextExtractionFilter
    extends MediaFilter {
    private final static Logger log = LogManager.getLogger();

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
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        boolean useTemporaryFile = configurationService.getBooleanProperty("textextractor.use-temp-file", false);

        if (useTemporaryFile) {
            // Extract text out of source file using a temp file, returning results as InputStream
            return extractUsingTempFile(source, verbose);
        }

        // Not using temporary file. We'll use Tika's default in-memory parsing.
        // Get maximum characters to extract. Default is 100,000 chars, which is also Tika's default setting.
        String extractedText;
        int maxChars = configurationService.getIntProperty("textextractor.max-chars", 100000);
        try {
            // Use Tika to extract text from input. Tika will automatically detect the file type.
            Tika tika = new Tika();
            tika.setMaxStringLength(maxChars); // Tell Tika the maximum number of characters to extract
            extractedText = tika.parseToString(source);
        } catch (IOException e) {
            System.err.format("Unable to extract text from bitstream in Item %s%n", currentItem.getID().toString());
            e.printStackTrace();
            log.error("Unable to extract text from bitstream in Item {}", currentItem.getID().toString(), e);
            throw e;
        } catch (OutOfMemoryError oe) {
            System.err.format("OutOfMemoryError occurred when extracting text from bitstream in Item %s. " +
                "You may wish to enable 'textextractor.use-temp-file'.%n", currentItem.getID().toString());
            oe.printStackTrace();
            log.error("OutOfMemoryError occurred when extracting text from bitstream in Item {}. " +
                          "You may wish to enable 'textextractor.use-temp-file'.", currentItem.getID().toString(), oe);
            throw oe;
        }

        if (StringUtils.isNotEmpty(extractedText)) {
            // if verbose flag is set, print out extracted text to STDOUT
            if (verbose) {
                System.out.println("(Verbose mode) Extracted text:");
                System.out.println(extractedText);
            }

            // return the extracted text as a UTF-8 stream.
            return new ByteArrayInputStream(extractedText.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    /**
     * Extracts the text out of a given source InputStream, using a temporary file. This decreases the amount of memory
     * necessary for text extraction, but can be slower as it requires writing extracted text to a temporary file.
     * @param source source InputStream
     * @param verbose verbose mode enabled/disabled
     * @return InputStream for temporary file containing extracted text
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    private InputStream extractUsingTempFile(InputStream source, boolean verbose)
        throws IOException, TikaException, SAXException {
        File tempExtractedTextFile = File.createTempFile("dspacetextextract" + source.hashCode(), ".txt");

        if (verbose) {
            System.out.println("(Verbose mode) Extracted text was written to temporary file at " +
                                   tempExtractedTextFile.getAbsolutePath());
        } else {
            tempExtractedTextFile.deleteOnExit();
        }

        // Open temp file for writing
        try (FileWriter writer = new FileWriter(tempExtractedTextFile, StandardCharsets.UTF_8)) {
            // Initialize a custom ContentHandlerDecorator which is a BodyContentHandler.
            // This mimics the behavior of Tika().parseToString(), which only extracts text from the body of the file.
            // This custom Handler writes any extracted text to the temp file.
            ContentHandlerDecorator handler = new BodyContentHandler(new ContentHandlerDecorator() {
                /**
                 * Write all extracted characters directly to the temp file.
                 */
                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    try {
                        writer.append(new String(ch), start, length);
                    } catch (IOException e) {
                        String errorMsg = String.format("Could not append to temporary file at %s " +
                                                            "when performing text extraction",
                                                        tempExtractedTextFile.getAbsolutePath());
                        log.error(errorMsg, e);
                        throw new SAXException(errorMsg, e);
                    }
                }

                /**
                 * Write all ignorable whitespace directly to the temp file.
                 * This mimics the behaviour of Tika().parseToString() which extracts ignorableWhitespace characters
                 * (like blank lines, indentations, etc.), so that we get the same extracted text either way.
                 */
                @Override
                public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
                    try {
                        writer.append(new String(ch), start, length);
                    } catch (IOException e) {
                        String errorMsg = String.format("Could not append to temporary file at %s " +
                                                            "when performing text extraction",
                                                        tempExtractedTextFile.getAbsolutePath());
                        log.error(errorMsg, e);
                        throw new SAXException(errorMsg, e);
                    }
                }
            });

            AutoDetectParser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            // parse our source InputStream using the above custom handler
            parser.parse(source, handler, metadata);
        }

        // At this point, all extracted text is written to our temp file. So, return a FileInputStream for that file
        return new FileInputStream(tempExtractedTextFile);
    }




}
