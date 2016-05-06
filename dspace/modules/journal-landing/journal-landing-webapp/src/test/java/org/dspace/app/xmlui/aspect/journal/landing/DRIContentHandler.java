
package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.cocoon.xml.DefaultLexicalHandler;
import org.dspace.app.xmlui.wing.element.WingElement;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Non-exhaustive DRI document object to jdom2 document converter,
 * for element, attribute and text content. Other node types
 * are dropped (per DefaultHandler2).
 * @author Nathan Day
 */
public class DRIContentHandler extends DefaultHandler2
{

    Document doc;
    Element cur;
    WingElement wingE;

    public DRIContentHandler(WingElement e) {
        this.wingE = e;
        this.doc = new Document();
    }
    public Document getDocument() throws SAXException {
        LexicalHandler lh = new DefaultLexicalHandler();
        NamespaceSupport ns = new NamespaceSupport();
        wingE.toSAX(this, lh, ns);
        return doc;
    }
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        Element child = new Element(localName, uri);
        if (cur == null) {
            doc.setRootElement(child);
            cur = doc.getRootElement();
        } else {
            cur.addContent(child);
            cur = child;
        }
        for (int i = 0; i < atts.getLength(); ++i) {
            child.setAttribute(new Attribute(atts.getLocalName(i), atts.getValue(i)));
        }
    }
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (!cur.equals(doc.getRootElement()))
          cur = cur.getParentElement();
    }
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String content = new String(ch);
        cur.addContent(content);
    }
}

