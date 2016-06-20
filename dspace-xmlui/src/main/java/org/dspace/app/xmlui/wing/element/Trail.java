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
 * This is a class representing a trail element. Trail elements carry
 * information about the user's current location in the system relative to the
 * repository's root page.
 * 
 * @author Scott Phillips
 */

public class Trail extends TextContainer
{
    /** The name of the trail element */
    public static final String E_TRAIL = "trail";

    /** The name of the target attribute */
    public static final String A_TARGET = "target";

    /** The name of the render attribute */
    public static final String A_RENDER = "rend";

    /** The trail's target */
    private String target;

    /** Any special rendering instructions for the trail. */
    private String rend;

    /**
     * Construct a new trail
     *
     * @param context
     *            document context.
     * @param target
     *            (May be null) The trail's target
     * @param rend
     *            (May be null) Special rendering instructions.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Trail(WingContext context, String target, String rend)
            throws WingException
    {
        super(context);
        this.target = target;
        this.rend = rend;
    }

    /**
     * Translate into SAX events.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler where lexical 
     *            events (such as CDATA, DTD, etc) should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @throws org.xml.sax.SAXException passed through.
     */
    @Override
    public void toSAX(ContentHandler contentHandler, LexicalHandler lexicalHandler,
            NamespaceSupport namespaces) throws SAXException
    {
        AttributeMap attributes = new AttributeMap();
        if (this.target != null)
        {
            attributes.put(A_TARGET, target);
        }
        if (this.rend != null)
        {
            attributes.put(A_RENDER, rend);
        }

        startElement(contentHandler, namespaces, E_TRAIL, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_TRAIL);
    }
}
