/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

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
        extends MediaFilter
{
    private static final Logger LOG = LoggerFactory.getLogger(PoiWordFilter.class);

    @Override
    public String getFilteredName(String oldFilename)
    {
        return oldFilename + ".txt";
    }

    @Override
    public String getBundleName()
    {
        return "TEXT";
    }

    @Override
    public String getFormatString()
    {
        return "Text";
    }

    @Override
    public String getDescription()
    {
        return "Extracted text";
    }

    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
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
            System.err.format("Invalid File Format:  %s%n", e.getMessage());
            LOG.error("Unable to parse the bitstream:  ", e);
            throw e;
        }

        // if verbose flag is set, print out extracted text to STDOUT
        if (verbose)
        {
            System.out.println(text);
        }

        // return the extracted text as a stream.
        return new ByteArrayInputStream(text.getBytes());
    }
}
