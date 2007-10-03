/*
 * Xref.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/03/13 17:19:39 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
     */
    protected Xref(WingContext context, String target, String rend) throws WingException
    {
        super(context);
        
        // Instead of validating the target field for null values we'll just assume that the calle 
        // ment to have a "/" but didn't quite handle this case. Ideal no one should call this 
        // method with a null value but sometimes it happens. What we are seing is that it is 
        // common for developers to just call it using something like <ContextPath + "/link">. 
        // However in the case where the calle just wants a link back to DSpace home they just pass
        // <ContextPath>, in cases where we are installed at the root of the servlet's url path this
        // is null. To correct for this common mistake we'll just change all null values to a plain 
        // old "/", assuming they ment the root.
        
        //require(target, "The 'target' parameter is required for all xrefs.");
        
        if (target == null)
        	target = "/";
        
        this.target = target;
        this.rend = rend;
    }
    
    /**
     * Construct a new xref link.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param target
     *            (Required) A target URL for the references a destination for
     *            the xref.
     */
    protected Xref(WingContext context, String target) throws WingException
    {
    	this(context,target,null);
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
     */

    public void toSAX(ContentHandler contentHandler, LexicalHandler lexicalHandler, 
            NamespaceSupport namespaces) throws SAXException
    {
        AttributeMap attributes = new AttributeMap();
        attributes.put(A_TARGET, target);
           
        if(this.rend!=null){
        	attributes.put(A_RENDER, this.rend);
        }

        startElement(contentHandler, namespaces, E_XREF, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_XREF);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        super.dispose();
    }
}
