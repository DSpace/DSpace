/*
 * Para.java
 *
 * Version: $Revision: 1.6 $
 *
 * Date: $Date: 2006/03/13 17:19:39 $
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
 * A class representing a paragraph, the "p" element.
 * 
 * The p element presents text in paragraph format. Its primary purpose is to
 * display textual data, possibly enhanced with hyperlinks, emphasized blocks of
 * text, images and form fields.
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

public class Para extends RichTextContainer implements StructuralElement
{
    /** The name of the para element */
    public static final String E_PARA = "p";

    /** The para's name */
    private String name;

    /** Any special rendering instructions for the para */
    private String rend;

    /**
     * Construct a new paragraph. Typically names for paragraphs are not
     * assigned by the java developer instead they are automatically generated
     * by the parent container. However the only real constraint is that the be
     * unique among other sibling paragraph elements.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * 
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings. *
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     */
    protected Para(WingContext context, String name, String rend)
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
        {
            attributes.put(A_NAME, name);
            attributes.put(A_ID, context.generateID(E_PARA, name));
        }
        if (rend != null)
            attributes.put("rend", rend);

        startElement(contentHandler, namespaces, E_PARA, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_PARA);
    }

}
