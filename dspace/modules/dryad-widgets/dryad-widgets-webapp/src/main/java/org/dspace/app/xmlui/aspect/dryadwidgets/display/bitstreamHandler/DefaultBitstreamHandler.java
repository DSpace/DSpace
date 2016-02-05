/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 *
 * @author Nathan Day
 */
public class DefaultBitstreamHandler extends BaseBitstreamHandler {
    public DefaultBitstreamHandler(ContentHandler contentHandler, LexicalHandler lexicalHandler) {
        super(contentHandler, lexicalHandler);
    }
    public void generate() {
        // do nothing
        log.trace("In default handler '" + this.getClass().getName() + "' for mime-type: " + format);
    }
}
