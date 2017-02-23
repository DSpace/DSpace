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

import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.xslf.extractor.XSLFPowerPointExtractor;
import org.apache.poi.hslf.extractor.PowerPointExtractor;
import org.apache.poi.POITextExtractor;

import org.apache.log4j.Logger;
import org.dspace.content.Item;

/*
 * TODO: Allow user to configure extraction of only text or only notes
 * 
 */
public class PowerPointFilter extends MediaFilter
{

    private static Logger log = Logger.getLogger(PowerPointFilter.class);

    @Override
    public String getFilteredName(String oldFilename)
    {
        return oldFilename + ".txt";
    }

    /**
     * @return String bundle name
     * 
     */
    @Override
    public String getBundleName()
    {
        return "TEXT";
    }

    /**
     * @return String bitstream format
     * 
     *         TODO: Check that this is correct
     */
    @Override
    public String getFormatString()
    {
        return "Text";
    }

    /**
     * @return String description
     */
    @Override
    public String getDescription()
    {
        return "Extracted text";
    }

    /**
     * @param currentItem item
     * @param source source input stream
     * @param verbose verbose mode
     * 
     * @return InputStream the resulting input stream
     * @throws Exception if error
     */
    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
            throws Exception
    {

        try
        {

            String extractedText = null;
            new ExtractorFactory();
            POITextExtractor pptExtractor = ExtractorFactory
                    .createExtractor(source);

            // PowerPoint XML files and legacy format PowerPoint files
            // require different classes and APIs for text extraction

            // If this is a PowerPoint XML file, extract accordingly
            if (pptExtractor instanceof XSLFPowerPointExtractor)
            {

                // The true method arguments indicate that text from
                // the slides and the notes is desired
                extractedText = ((XSLFPowerPointExtractor) pptExtractor)
                        .getText(true, true);
            }

            // Legacy PowerPoint files
            else if (pptExtractor instanceof PowerPointExtractor)
            {

                extractedText = ((PowerPointExtractor) pptExtractor).getText()
                        + " " + ((PowerPointExtractor) pptExtractor).getNotes();

            }
            if (extractedText != null)
            {
                // if verbose flag is set, print out extracted text
                // to STDOUT
                if (verbose)
                {
                    System.out.println(extractedText);
                }

                // generate an input stream with the extracted text
                byte[] textBytes = extractedText.getBytes();
                ByteArrayInputStream bais = new ByteArrayInputStream(textBytes);

                return bais;
            }
        }
        catch (Exception e)
        {
            log.error("Error filtering bitstream: " + e.getMessage(), e);
            throw e;
        }

        return null;
    }
}
