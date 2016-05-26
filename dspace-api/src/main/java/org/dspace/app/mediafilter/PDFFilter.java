/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.dspace.content.Item;
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
     * @return String bitstreamformat
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
            boolean useTemporaryFile = ConfigurationManager.getBooleanProperty("pdffilter.largepdfs", false);

            // get input stream from bitstream
            // pass to filter, get string back
            PDFTextStripper pts = new PDFTextStripper();
            pts.setSortByPosition(true);
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
                    {
                        pdfDoc.close();
                    }
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
