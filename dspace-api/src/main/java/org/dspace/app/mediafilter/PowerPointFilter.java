/*
 * PowerPointFilter.java
 *
 * Copyright (c) 2002-2010, Duraspace.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of Duraspace nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.mediafilter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.xslf.extractor.XSLFPowerPointExtractor;
import org.apache.poi.hslf.extractor.PowerPointExtractor;
import org.apache.poi.POITextExtractor;

import org.apache.log4j.Logger;


/*
 * TODO: Allow user to configure extraction of only text or only notes
 * 
 */
public class PowerPointFilter extends MediaFilter
{

    private static Logger log = Logger.getLogger(PowerPointFilter.class);

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
     *
     *  TODO: Check that this is correct
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
        
        try
        {
	  
            String extractedText = null;  
	    POITextExtractor pptExtractor = 
		new ExtractorFactory().createExtractor(source);
	
            // PowerPoint XML files and legacy format PowerPoint files
            // require different classes and APIs for text extraction

            // If this is a PowerPoint XML file, extract accordingly  
	    if (pptExtractor instanceof XSLFPowerPointExtractor)
	    {  
                
                // The true method arguments indicate that text from
                // the slides and the notes is desired
		extractedText = 
                       ((XSLFPowerPointExtractor)pptExtractor).getText(true, true);
	    }    
            
            // Legacy PowerPoint files
            else if (pptExtractor instanceof PowerPointExtractor)
            {

                extractedText = ((PowerPointExtractor)pptExtractor).getText() 
                    + " " + ((PowerPointExtractor)pptExtractor).getNotes();

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
	}
	catch(Exception e)
	{
	    log.error("Error filtering bitstream: " + e.getMessage(), e);
	}
	
        return null;
    }
}
