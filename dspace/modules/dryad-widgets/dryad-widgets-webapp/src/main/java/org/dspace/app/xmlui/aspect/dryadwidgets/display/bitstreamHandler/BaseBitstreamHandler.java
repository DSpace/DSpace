/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.AbstractGenerator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Nathan Day
 */
public abstract class BaseBitstreamHandler extends AbstractGenerator {
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
    private final String dcSourceVal = "dc.source";
    private final String dcExtentVal = "dc.extent";
    private final String bodyEltName = "body";
    
    // Data-One service response headers
    private static final String DOFid = "DataONE-formatId";
    private static final String ContLen = "Content-Length";
    
    // parameter values
    protected String format = null;
    protected String extent = null;
    
    protected ContentHandler contentHandler;
    protected LexicalHandler lexicalHandler;
    
    public BaseBitstreamHandler(ContentHandler contentHandler, LexicalHandler lexicalHandler) {
        this.contentHandler = contentHandler;
        this.lexicalHandler = lexicalHandler;
    }
    
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String source, Parameters parameters)
            throws ProcessingException, SAXException, IOException
    {

        super.setup(resolver, objectModel, source, parameters);
        try {
            format = parameters.getParameter(DOFid);
            extent = parameters.getParameter(ContLen);
        } catch (ParameterException ex) {
            log.error(ex);
        }

        final Attributes emptyAttr = new AttributesImpl();
        contentHandler.startDocument();
        contentHandler.startPrefixMapping("", NSURI);
        // <xhtml xmlns="http://www.w3.org/1999/xhtml">
        contentHandler.startElement(NSURI, htmlEltName, htmlEltName, emptyAttr);
        /*  <head>
        <meta property="dc.format">plain/text</meta>
        </head> */
        contentHandler.startElement(NSURI, headEltName, headEltName, emptyAttr);
        addMetaElt(dcFormatVal, format);
        addMetaElt(dcSourceVal, source);
        addMetaElt(dcExtentVal, extent);
        contentHandler.endElement(NSURI, headEltName, headEltName);
        // <body>
        contentHandler.startElement(NSURI, bodyEltName, bodyEltName, emptyAttr);
    }

    private void addMetaElt(String prop, String eltVal) throws SAXException {
        AttributesImpl metaAtts = new AttributesImpl();
        metaAtts.addAttribute("", propertyAttName, propertyAttName, "String", prop);
        contentHandler.startElement(NSURI, metaEltName, metaEltName, metaAtts);
        contentHandler.characters(eltVal.toCharArray(), 0, eltVal.length());
        contentHandler.endElement(NSURI, metaEltName, metaEltName);
    }
    
    @Override
    public void recycle() {
        try {
            contentHandler.endElement(NSURI, bodyEltName, bodyEltName);
            contentHandler.endElement(NSURI, htmlEltName, htmlEltName);
            contentHandler.endPrefixMapping("");
            contentHandler.endDocument();
        } catch (SAXException ex) {
            log.error(ex);
        }
        super.recycle();
        this.resolver = null;
        this.objectModel = null;
        this.source = null;
        this.parameters = null;
    }

    public abstract void generate() throws SAXException, IOException, ProcessingException;
}
