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
 * This class represents a xref link to an external document. The text within
 * the tag itself will be used as part of the link's visual body.
 * 
 * @author Scott Phillips
 */

public class Xref extends TextContainer implements StructuralElement
{
    /** The name of the xref element */
    public static final String E_XREF = "xref";

    /** The name of the target attribute */
    public static final String A_TARGET = "target";

    /** The link's target */
    private String target;
    
    /** Special rendering instructions for this link */
    private String rend;
    
    /** The link's name */
    private String name;

    /**
     * Construct a new xref link.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param target
     *            (Required) A target URL for the references a destination for
     *            the xref.
     * @param rend
     *            (May be null) A special rendering instruction for this xref.
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @throws org.dspace.app.xmlui.wing.WingException never.
     */
    protected Xref(WingContext context, String target, String rend, String name) throws WingException
    {
        super(context);
        
        // Instead of validating the target field for null values we'll just assume that the caller 
        // meant to have a "/" but didn't quite handle this case. Ideal no one should call this 
        // method with a null value but sometimes it happens. What we are seeing is that it is 
        // common for developers to just call it using something like <ContextPath + "/link">. 
        // However in the case where the caller just wants a link back to DSpace home they just pass
        // <ContextPath>, in cases where we are installed at the root of the servlet's url path this
        // is null. To correct for this common mistake we'll just change all null values to a plain 
        // old "/", assuming they meant the root.
        
        //require(target, "The 'target' parameter is required for all xrefs.");
        
        if (target == null)
        {
            target = "/";
        }
        
        this.target = target;
        this.rend = rend;
        this.name = name;
    }
    
    /**
     * Construct a new xref link.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param target
     *            (Required) A target URL for the references a destination for
     *            the xref.
     * @param rend
     *            (May be null) A special rendering instruction for this xref.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Xref(WingContext context, String target, String rend) throws WingException
    {
        this(context, target, rend, null);
    }
    
    /**
     * Construct a new xref link.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param target
     *            (Required) A target URL for the references a destination for
     *            the xref.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Xref(WingContext context, String target) throws WingException
    {
    	this(context, target, null, null);
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
        attributes.put(A_TARGET, target);
           
        if (name != null)
        {
            attributes.put(A_NAME, name);
            attributes.put(A_ID, context.generateID(E_XREF, name));
        }
        
        if(this.rend!=null){
        	attributes.put(A_RENDER, this.rend);
        }

        startElement(contentHandler, namespaces, E_XREF, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_XREF);
    }
}
