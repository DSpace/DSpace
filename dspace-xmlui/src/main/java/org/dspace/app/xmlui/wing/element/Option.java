/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * 
 * @author Scott Phillips
 */

public class Option extends TextContainer
{
    /** The name of the value element */
    public static final String E_OPTION = "option";

    /** The name of the return value attribute */
    public static final String A_RETURN_VALUE = "returnValue";

    /** The submitted value for this option */
    private final String returnValue;

    /**
     *
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param returnValue
     *            (may be null) The options return value.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Option(WingContext context, String returnValue) throws WingException
    {
        super(context);

        this.returnValue = returnValue;
    }

    /**
     * Translate this element and all contained elements into SAX events. The
     * events should be routed to the contentHandler found in the WingContext.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler where lexical events
     *            (such as CDATA, DTD, etc) should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @throws org.xml.sax.SAXException passed through.
     */
    @Override
    public void toSAX(ContentHandler contentHandler,
            LexicalHandler lexicalHandler, NamespaceSupport namespaces)
            throws SAXException
    {
        AttributeMap attributes = new AttributeMap();
        attributes.put(A_RETURN_VALUE, this.returnValue);
        
        startElement(contentHandler, namespaces, E_OPTION, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_OPTION);
    }
}
