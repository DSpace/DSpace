/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler;

import org.apache.cocoon.xml.XMLConsumer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * This class implements an XMLConsumer to be used to pass into objects that
 * generate SAX events. This consumer simply wraps handlers for SAX events,
 * with the exception of startDocument()/endDocument(), which it suppresses,
 * since the XMLConsumer receiving these events will have already had the
 * document opened, and will be closing the document itself, due to the wrapping
 * of XML content that occurs in BaseBitstreamHandler.
 * 
 * @author Nathan Day
 */
public class BitstreamXMLConsumer implements XMLConsumer {

    private ContentHandler contentHandler;
    private LexicalHandler lexicalHandler;
    
    public BitstreamXMLConsumer(ContentHandler contentHandler, LexicalHandler lexicalHandler) {
        this.contentHandler = contentHandler;
        this.lexicalHandler = lexicalHandler;
    }
    
    @Override
    public void setDocumentLocator(Locator locator) {
        contentHandler.setDocumentLocator(locator);
    }

    @Override
    // already called in BaseBitstreamHandler
    public void startDocument() throws SAXException {}

    @Override
    // already called in BaseBitstreamHandler
    public void endDocument() throws SAXException {}

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        contentHandler.startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        contentHandler.endPrefixMapping(prefix);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        contentHandler.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        contentHandler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        contentHandler.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        contentHandler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        contentHandler.processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        contentHandler.skippedEntity(name);
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        lexicalHandler.startDTD(name, publicId, systemId);
    }

    @Override
    public void endDTD() throws SAXException {
        lexicalHandler.endDTD();
    }

    @Override
    public void startEntity(String name) throws SAXException {
        lexicalHandler.startEntity(name);
    }

    @Override
    public void endEntity(String name) throws SAXException {
        lexicalHandler.endEntity(name);
    }

    @Override
    public void startCDATA() throws SAXException {
        lexicalHandler.startCDATA();
    }

    @Override
    public void endCDATA() throws SAXException {
        lexicalHandler.endCDATA();
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        lexicalHandler.comment(ch, start, length);
    }
    
}
