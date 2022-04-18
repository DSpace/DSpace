/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.ByteArrayInputStream;
<<<<<<< HEAD
import java.io.InputStream;
import java.io.IOException;
=======
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
>>>>>>> dspace-7.2.1

import org.apache.poi.POITextExtractor;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.xmlbeans.XmlException;
import org.dspace.content.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract flat text from Microsoft Word documents (.doc, .docx).
 */
public class PoiWordFilter
<<<<<<< HEAD
        extends MediaFilter
{
    private static final Logger LOG = LoggerFactory.getLogger(PoiWordFilter.class);

    @Override
    public String getFilteredName(String oldFilename)
    {
=======
    extends MediaFilter {
    private static final Logger LOG = LoggerFactory.getLogger(PoiWordFilter.class);

    @Override
    public String getFilteredName(String oldFilename) {
>>>>>>> dspace-7.2.1
        return oldFilename + ".txt";
    }

    @Override
<<<<<<< HEAD
    public String getBundleName()
    {
=======
    public String getBundleName() {
>>>>>>> dspace-7.2.1
        return "TEXT";
    }

    @Override
<<<<<<< HEAD
    public String getFormatString()
    {
=======
    public String getFormatString() {
>>>>>>> dspace-7.2.1
        return "Text";
    }

    @Override
<<<<<<< HEAD
    public String getDescription()
    {
=======
    public String getDescription() {
>>>>>>> dspace-7.2.1
        return "Extracted text";
    }

    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
<<<<<<< HEAD
            throws Exception
    {
        String text;
        try  
        {
            // get input stream from bitstream, pass to filter, get string back
            POITextExtractor extractor = ExtractorFactory.createExtractor(source);
            text = extractor.getText();
        }
        catch (IOException | OpenXML4JException | XmlException e)
        {
=======
        throws Exception {
        String text;
        try {
            // get input stream from bitstream, pass to filter, get string back
            POITextExtractor extractor = ExtractorFactory.createExtractor(source);
            text = extractor.getText();
        } catch (IOException | OpenXML4JException | XmlException e) {
>>>>>>> dspace-7.2.1
            System.err.format("Invalid File Format:  %s%n", e.getMessage());
            LOG.error("Unable to parse the bitstream:  ", e);
            throw e;
        }

        // if verbose flag is set, print out extracted text to STDOUT
<<<<<<< HEAD
        if (verbose)
        {
=======
        if (verbose) {
>>>>>>> dspace-7.2.1
            System.out.println(text);
        }

        // return the extracted text as a stream.
<<<<<<< HEAD
        return new ByteArrayInputStream(text.getBytes());
=======
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
>>>>>>> dspace-7.2.1
    }
}
