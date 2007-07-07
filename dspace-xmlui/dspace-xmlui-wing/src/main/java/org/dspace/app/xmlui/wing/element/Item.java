/*
 * Item.java
 *
 * Version: $Revision: 1.7 $
 *
 * Date: $Date: 2006/07/13 23:21:06 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

/**
 * A class representing a list item.
 * 
 * The item element in a list serves a dual purpose. It can contain other lists,
 * allowing for hierarchies and recursive lists. Alternatively it can serve as a
 * character container to display textual data, possibly enhanced with
 * hyperlinks, emphasized blocks of text, images and form fields. An item cannot
 * be both a character container and contain a list.
 * 
 * @author Scott Phillips
 */

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

public class Item extends RichTextContainer implements StructuralElement
{
    /** The name of the item element */
    public static final String E_ITEM = "item";

    /** the item's name */
    private String name;

    /** Special rendering hints for this item */
    private String rend;

    /**
     * Construct a new item.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     */
    protected Item(WingContext context, String name, String rend)
            throws WingException
    {
        super(context);
        this.name = name;
        this.rend = rend;
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
        if (name != null)
            attributes.put(A_NAME, name);
        if (name != null)
            attributes.put(A_ID, context.generateID(E_ITEM, name));
        if (rend != null)
            attributes.put(A_RENDER, rend);

        startElement(contentHandler, namespaces, E_ITEM, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_ITEM);
    }
    
    /**
     * dispose
     */
    public void dispose() {
        super.dispose();
    }

}
