/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler;

import java.util.Map;
import org.apache.cocoon.environment.SourceResolver;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * 
 * @author Nathan Day
 */
public class TemplateBitstreamHandler extends BaseBitstreamHandler {
    public TemplateBitstreamHandler(String url, String format, ContentHandler contentHandler, LexicalHandler lexicalHandler, SourceResolver resolver, Map objectModel) 
            throws SAXException 
    {
        super(url, format, contentHandler, lexicalHandler, resolver, objectModel);
        System.exit(1);
    }
    public void generate() {
        // do nothing
        log.fatal("In handler template '" + this.getClass().getName() + "' for mime-type: " + format);
    }
}
