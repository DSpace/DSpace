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

import org.apache.poi.POITextExtractor;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;

import org.apache.log4j.Logger;

/*
 * ExcelFilter
 *
 * Entries you must add to dspace.cfg:
 *
 * filter.plugins = blah, \
 *     Excel Text Extractor
 *
 * plugin.named.org.dspace.app.mediafilter.FormatFilter = \
 *     blah = blah, \
 *     org.dspace.app.mediafilter.ExcelFilter = Excel Text Extractor
 *
 * #Configure each filter's input Formats
 * filter.org.dspace.app.mediafilter.ExcelFilter.inputFormats = Microsoft Excel, Microsoft Excel XML
 *
 */
public class ExcelFilter extends MediaFilter
{

    private static Logger log = Logger.getLogger(ExcelFilter.class);

    public String getFilteredName(String oldFilename)
    {
        return oldFilename + ".txt";
    }

    /**
     * @return String bundle name
     * 
     */
    public String getBundleName()
    {
        return "TEXT";
    }

    /**
     * @return String bitstream format
     * 
     *         TODO: Check that this is correct
     */
    public String getFormatString()
    {
        return "Text";
    }

    /**
     * @return String description
     */
    public String getDescription()
    {
        return "Extracted text";
    }

    /**
     * @param source
     *            source input stream
     * 
     * @return InputStream the resulting input stream
     */
    public InputStream getDestinationStream(InputStream source)
            throws Exception
    {
        String extractedText = null;

        try
        {
            new ExtractorFactory();
            POITextExtractor theExtractor = ExtractorFactory.createExtractor(source);
            if (theExtractor instanceof ExcelExtractor) // for xls file
                extractedText = ((ExcelExtractor ) theExtractor).getText();
            else if (theExtractor instanceof XSSFExcelExtractor) // for xlsx file
                extractedText = ((XSSFExcelExtractor ) theExtractor).getText();
        }
        catch (Exception e)
        {
            log.error("Error filtering bitstream: " + e.getMessage(), e);
            throw e;
        }

        if (extractedText != null)
        {
            // if verbose flag is set, print out extracted text
            // to STDOUT
            if (MediaFilterManager.isVerbose)
            {
                System.out.println(extractedText);
            }

            // generate an input stream with the extracted text
            byte[] textBytes = extractedText.getBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(textBytes);

            return bais;
        }

        return null;
    }
}
