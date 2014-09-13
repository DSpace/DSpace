/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler;

import java.io.BufferedReader;
import java.io.IOException;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Nathan Day
 */
public abstract class BaseBitstreamHandler {
    protected static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BaseBitstreamHandler.class);    
    /*
        A bitstream handler generates an XML with this structure:

        <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <meta property="dc:format">...</meta>
            </head>
            <body>
                ...
            </body>
        </html>

        The generated XML document will be handled by an XSLT transformer,
        so it must be well-formed. Lexically risky content shuld be 
        escaped in a <!CDATA[]]> declaration.
    */
    private final String NSURI = "http://www.w3.org/1999/xhtml";
    private final String NSNAME = "";
    private final String htmlEltName = "html";
    private final String headEltName = "head";
    private final String metaEltName = "meta";
    private final String propertyAttName = "property";
    private final String dcFormatVal = "dc:format";
    private final String bodyEltName = "body";
    
    protected BufferedReader bufferedReader;
    protected ContentHandler contentHandler;
    protected LexicalHandler lexicalHandler;
    protected String format;
    public BaseBitstreamHandler(BufferedReader bufferedReader, ContentHandler contentHandler, LexicalHandler lexicalHandler, String format) throws SAXException {
        this.bufferedReader = bufferedReader;
        this.contentHandler = contentHandler;
        this.lexicalHandler = lexicalHandler;
        this.format = format;
        log.debug("contentHandler.startDocument();");
        contentHandler.startDocument();
        // <html>
        log.debug("contentHandler.startElement(NSURI, htmlEltName, htmlEltName, null);");
        try {
            contentHandler.startElement(NSURI, htmlEltName, htmlEltName, null);
        } catch (SAXException e) {
            log.error("SAXException with startElement(): " + e.getMessage());
            throw new SAXException(e.getMessage());
        } catch (Throwable e) {
            log.error("Throwable with startElement(): " + e.getMessage());
            throw new SAXException(e.getMessage());
        }
        /*  <head>
                <meta property="dc:format">plain/text</meta>
            </head> */
        log.debug("contentHandler.startElement(NSURI, headEltName, headEltName, null);");
        contentHandler.startElement(NSURI, headEltName, headEltName, null);
        log.debug("AttributesImpl atts = new AttributesImpl();");
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(NSURI, propertyAttName, propertyAttName, "String", dcFormatVal);
        log.debug("contentHandler.startElement(NSURI, metaEltName, metaEltName, atts);");
        contentHandler.startElement(NSURI, metaEltName, metaEltName, atts);
        log.debug("contentHandler.characters(format.toCharArray(), 0, format.length());");
        contentHandler.characters(format.toCharArray(), 0, format.length());
        log.debug("contentHandler.endElement(NSURI, metaEltName, metaEltName);");
        contentHandler.endElement(NSURI, metaEltName, metaEltName);
        log.debug("contentHandler.endElement(NSURI, headEltName, headEltName);");
        contentHandler.endElement(NSURI, headEltName, headEltName);
        // <body>
        log.debug("contentHandler.startElement(NSURI, bodyEltName, bodyEltName, null);");
        contentHandler.startElement(NSURI, bodyEltName, bodyEltName, null);
    }
    public void finalize() throws SAXException {
        // </body>
        contentHandler.endElement(NSURI, bodyEltName, bodyEltName);
        // </html>
        contentHandler.endElement(NSURI, htmlEltName, htmlEltName);
        contentHandler.endDocument();
    }
    public abstract void generate() throws SAXException, IOException;
}
