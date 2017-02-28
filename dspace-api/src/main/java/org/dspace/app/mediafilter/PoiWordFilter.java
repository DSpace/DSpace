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
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
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
        try  
        {
            // get input stream from bitstream, pass to filter, get string back
            String text;
            POITextExtractor extractor = ExtractorFactory.createExtractor(source);
            if (extractor instanceof XWPFWordExtractor)
                text = ((XWPFWordExtractor) extractor).getText();
            else if (extractor instanceof WordExtractor)
                text = ((WordExtractor) extractor).getText();
            else
                throw new IllegalArgumentException(
                        "Bitstream is neither .doc nor .docx format.  Extractor returned a "
                                + extractor.getClass().getCanonicalName());

            // if verbose flag is set, print out extracted text to STDOUT
            if (verbose)
            {
                System.out.println(text);
            }

            // return the extracted text as a stream.
            return new ByteArrayInputStream(text.getBytes());
        } 
        catch (IOException ioe)
        {
            System.out.println("Invalid File Format");
            LOG.error("Error detected - Microsoft Word file format not recognized: "
                    + ioe.getMessage(), ioe);
            throw ioe;
        }
    }
}
