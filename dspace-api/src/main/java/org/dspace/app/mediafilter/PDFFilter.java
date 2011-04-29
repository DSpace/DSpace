/*
 * PDFFilter.java
 *
 * Version: $Revision: 3733 $
 *
 * Date: $Date: 2009-04-23 23:52:11 -0400 (Thu, 23 Apr 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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
import java.io.Writer;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.CharArrayWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.PDPage;
import org.pdfbox.pdmodel.common.PDStream;
import org.pdfbox.util.PDFTextStripper;
import org.dspace.core.ConfigurationManager;

/*
 *
 * to do: helpful error messages - can't find mediafilter.cfg - can't
 * instantiate filter - bitstream format doesn't exist
 *
 */
public class PDFFilter extends MediaFilter
{

    private static Logger log = Logger.getLogger(PDFFilter.class);

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
        try
        {
            boolean useTemporaryFile = ConfigurationManager.getBooleanProperty("pdffilter.largepdfs", false);

            // get input stream from bitstream
            // pass to filter, get string back
            PDFTextStripper pts = new PDFTextStripper();
            PDDocument pdfDoc = null;
            Writer writer = null;
            File tempTextFile = null;
            ByteArrayOutputStream byteStream = null;

            if (useTemporaryFile)
            {
                tempTextFile = File.createTempFile("dspacepdfextract" + source.hashCode(), ".txt");
                tempTextFile.deleteOnExit();
                writer = new OutputStreamWriter(new FileOutputStream(tempTextFile));
            }
            else
            {
                byteStream = new ByteArrayOutputStream();
                writer = new OutputStreamWriter(byteStream);
            }
            
            try
            {
                pdfDoc = PDDocument.load(source);
                pts.writeText(pdfDoc, writer);
            }
            finally
            {
                try
                {
                    if (pdfDoc != null)
                        pdfDoc.close();
                }
                catch(Exception e)
                {
                   log.error("Error closing PDF file: " + e.getMessage(), e);
                }

                try
                {
                    writer.close();
                }
                catch(Exception e)
                {
                   log.error("Error closing temporary extract file: " + e.getMessage(), e);
                }
            }

            if (useTemporaryFile)
            {
                return new FileInputStream(tempTextFile);
            }
            else
            {
                byte[] bytes = byteStream.toByteArray();
                return new ByteArrayInputStream(bytes);
            }
        }
        catch (OutOfMemoryError oome)
        {
            log.error("Error parsing PDF document " + oome.getMessage(), oome);
            if (!ConfigurationManager.getBooleanProperty("pdffilter.skiponmemoryexception", false))
            {
                throw oome;
            }
        }

        return null;
    }
}
