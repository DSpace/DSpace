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

import org.apache.log4j.Logger;

import org.textmining.extraction.TextExtractor;
import org.textmining.extraction.word.WordTextExtractorFactory;

/*
 * 
 * to do: helpful error messages - can't find mediafilter.cfg - can't
 * instantiate filter - bitstream format doesn't exist.
 *  
 */
public class WordFilter extends MediaFilter
{

    private static Logger log = Logger.getLogger(WordFilter.class);

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
     * @return String bitstreamformat
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
        // get input stream from bitstream
        // pass to filter, get string back
        try  
        {
            WordTextExtractorFactory factory = new WordTextExtractorFactory();
            TextExtractor e = factory.textExtractor(source);
            String extractedText = e.getText();

            // if verbose flag is set, print out extracted text
            // to STDOUT
            if (MediaFilterManager.isVerbose)
            {
                System.out.println(extractedText);
            }

            // generate an input stream with the extracted text
            byte[] textBytes = extractedText.getBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(textBytes);

            return bais; // will this work? or will the byte array be out of scope?
        } 
        catch (IOException ioe)
        {
            System.out.println("Invalid Word Format");
            log.error("Error detected - Word File format not recognized: "
                    + ioe.getMessage(), ioe);
            throw ioe;
        }
    }
}
