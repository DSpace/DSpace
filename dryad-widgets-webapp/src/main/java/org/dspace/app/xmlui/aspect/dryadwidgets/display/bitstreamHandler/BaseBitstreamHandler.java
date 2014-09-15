/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler;

import java.io.IOException;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
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
    private final String htmlEltName = "xhtml";
    private final String headEltName = "head";
    private final String metaEltName = "meta";
    private final String propertyAttName = "property";
    private final String dcFormatVal = "dc.format";
    private final String bodyEltName = "body";
    
    protected String url;
    protected String format;
    protected ContentHandler contentHandler;
    protected LexicalHandler lexicalHandler;
    protected SourceResolver sourceResolver;
    
    public BaseBitstreamHandler(String url, String format, ContentHandler contentHandler, LexicalHandler lexicalHandler, SourceResolver resolver) throws SAXException {
        this.url = url;
        this.format = format.trim();
        this.contentHandler = contentHandler;
        this.lexicalHandler = lexicalHandler;
        this.sourceResolver = sourceResolver;
    }
    public void start() throws SAXException {
        final Attributes emptyAttr = new AttributesImpl();
        contentHandler.startDocument();
        contentHandler.startPrefixMapping("", NSURI);
        // <xhtml xmlns="http://www.w3.org/1999/xhtml">
        contentHandler.startElement(NSURI, htmlEltName, htmlEltName, emptyAttr);
        /*  <head>
                <meta property="dc:format">plain/text</meta>
            </head> */
        contentHandler.startElement(NSURI, headEltName, headEltName, emptyAttr);
        AttributesImpl metaAtts = new AttributesImpl();
        metaAtts.addAttribute("", propertyAttName, propertyAttName, "String", dcFormatVal);
        contentHandler.startElement(NSURI, metaEltName, metaEltName, metaAtts);
        contentHandler.characters(format.toCharArray(), 0, format.length());
        contentHandler.endElement(NSURI, metaEltName, metaEltName);
        contentHandler.endElement(NSURI, headEltName, headEltName);
        // <body>
        contentHandler.startElement(NSURI, bodyEltName, bodyEltName, emptyAttr);
    }
    public void end() throws SAXException {
        // </body>
        contentHandler.endElement(NSURI, bodyEltName, bodyEltName);
        // </html>
        contentHandler.endElement(NSURI, htmlEltName, htmlEltName);
        contentHandler.endPrefixMapping("");
        contentHandler.endDocument();
    }
    public abstract void generate() throws SAXException, IOException, ProcessingException;
}
