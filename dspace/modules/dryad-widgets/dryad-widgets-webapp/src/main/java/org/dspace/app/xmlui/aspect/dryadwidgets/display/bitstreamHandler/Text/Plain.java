/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text;

import java.io.IOException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
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

    public Plain(ContentHandler contentHandler, LexicalHandler lexicalHandler) {
        super(contentHandler, lexicalHandler);
    }
    
    /**
     * Generate a plain text data section, wrapped in <!CDATA[]]>.
     * @throws SAXException
     * @throws IOException
     */
    @Override
    public void generate() throws SAXException, IOException {
        TextGenerator text = new TextGenerator();
        Parameters params = new Parameters();
        /*
            params.setParameter("encoding", "");
        */
        BitstreamXMLConsumer consumer = new BitstreamXMLConsumer(contentHandler, lexicalHandler);
        try {
            text.setup(resolver, objectModel, source, params);
            text.setConsumer(consumer);
            text.generate();
        } catch (Exception e) {
            log.error("Failed to generate Text data: " + e.getMessage());
        } finally {
            if (text != null) {
                text.dispose();
            }
        }

    }
}
