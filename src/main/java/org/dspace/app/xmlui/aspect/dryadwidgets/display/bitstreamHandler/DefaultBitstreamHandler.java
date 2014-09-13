/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler;

import java.io.BufferedReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 *
 * @author Nathan Day
 */
public class DefaultBitstreamHandler extends BaseBitstreamHandler {
    public DefaultBitstreamHandler(BufferedReader bufferedReader, ContentHandler contentHandler, LexicalHandler lexicalHandler, String format) throws SAXException {
        super(bufferedReader, contentHandler, lexicalHandler, format);
    }
    public void generate() {
        // do nothing
        log.debug("In default handler '" + this.getClass().getName() + "' for mime-type: " + format);
    }
}
