/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text;

import java.io.IOException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.TextGenerator;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.BaseBitstreamHandler;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.BitstreamXMLConsumer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 *
 * @author Nathan Day
 */
public class Plain extends BaseBitstreamHandler {
    public Plain(String url, String format, ContentHandler contentHandler, LexicalHandler lexicalHandler, SourceResolver resolver) throws SAXException {
        super(url, format, contentHandler, lexicalHandler, resolver);
    }
    /**
     * Generate a plain text data section, wrapped in <!CDATA[]]>.
     * @throws SAXException
     * @throws IOException
     */
    @Override
    public void generate() throws SAXException, IOException, ProcessingException {
        TextGenerator text = new TextGenerator();
        /*
            Parameters params = new Parameters();
            params.setParameter("encoding", "");
            params.setParameter("separator", "");
            params.setParameter("escape", "");
            params.setParameter("buffer-size", "");
            params.setParameter("max-records", "");
        */
        text.setup(sourceResolver, null, url, Parameters.EMPTY_PARAMETERS);
        BitstreamXMLConsumer consumer = new BitstreamXMLConsumer(contentHandler, lexicalHandler);
        try {
            text.setConsumer(consumer);
            text.generate();
        } catch (Exception e) {
            log.error("Failed to generate CSV data: " + e.getMessage());
        } finally {
            if (text != null) {
                text.dispose();
            }
        }

    }
}
