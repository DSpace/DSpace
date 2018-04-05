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
 * This class represents a figure element used to embed a reference to an image
 * or graphic element. Any text within the element will be used as an
 * alternative descriptor or a caption.
 * 
 * @author Scott Phillips
 */

public class Figure extends TextContainer implements StructuralElement
{
    /** The name of the figure element */
    public static final String E_FIGURE = "figure";

    /** The name of the source attribute */
    public static final String A_SOURCE = "source";

    /** The name of the target attribute */
    public static final String A_TARGET = "target";


    /** The name of the title attribute */
    public static final String A_TITLE = "title";

    /** The figure's source */
    private final String source;

    /** The figure's xref target */
    private final String target;


    /** The figure's xref title */
    private String title;

    /** Special rendering hints */
    private final String rend;

    /**
     * Construct a new figure.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param source
     *            (Required) The figure's image source.
     * @param target
     *            (May be null) The figure's external reference, if present then
     *            the figure is also a link.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @throws WingException passed through.
     */
    protected Figure(WingContext context, String source, String target,
            String rend) throws WingException
    {
        super(context);
        require(source, "The 'source' parameter is required for all figures.");
        this.source = source;
        this.target = target;
        this.rend = rend;
    }

    protected Figure(WingContext context, String source, String target,
            String title, String rend) throws WingException
    {
        super(context);
        require(source, "The 'source' parameter is required for all figures.");
        this.source = source;
        this.target = target;
        this.title 	  = title;
        this.rend 	  = rend;
    }

    /**
     * Translate this element and all contained elements into SAX events. The
     * events should be routed to the contentHandler found in the WingContext.
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
        attributes.put(A_SOURCE, this.source);
        if (this.target != null)
            attributes.put(A_TARGET, this.target);
        if (this.title != null)
            attributes.put(A_TITLE, this.title);
        if (this.rend != null)
            attributes.put(A_RENDER, this.rend);

        startElement(contentHandler, namespaces, E_FIGURE, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_FIGURE);
    }
}
