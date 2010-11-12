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

import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

/*
 * 
 * to do: helpful error messages - can't find mediafilter.cfg - can't
 * instantiate filter - bitstream format doesn't exist
 *  
 */
public class HTMLFilter extends MediaFilter
{

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
        // try and read the document - set to ignore character set directive,
        // assuming that the input stream is already set properly (I hope)
        HTMLEditorKit kit = new HTMLEditorKit();
        Document doc = kit.createDefaultDocument();

        doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);

        kit.read(source, doc, 0);

        String extractedText = doc.getText(0, doc.getLength());

        // generate an input stream with the extracted text
        byte[] textBytes = extractedText.getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(textBytes);

        return bais; // will this work? or will the byte array be out of scope?
    }
}
