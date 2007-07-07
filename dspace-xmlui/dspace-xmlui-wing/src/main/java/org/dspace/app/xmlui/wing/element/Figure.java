/*
 * Figure.java
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

    /** The figure's source */
    private String source;

    /** The figure's xref target */
    private String target;

    /** Special rendering hints */
    private String rend;

    /**
     * Construct a new figure.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param Source
     *            (Required) The figure's image source.
     * @param target
     *            (May be null) The figure's external reference, if present then
     *            the figure is also a link.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @throws WingException
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
        attributes.put(A_SOURCE, this.source);
        if (this.target != null)
            attributes.put(A_TARGET, this.target);
        if (this.rend != null)
            attributes.put(A_RENDER, this.rend);

        startElement(contentHandler, namespaces, E_FIGURE, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_FIGURE);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        super.dispose();
    }
}
