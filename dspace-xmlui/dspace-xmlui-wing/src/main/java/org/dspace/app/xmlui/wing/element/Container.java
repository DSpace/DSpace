/*
 * Container.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/07/05 21:40:01 $
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
 * This class represents a generic Wing Container. The Container class adds a
 * simlpe contents list which may be modified by the extending concret classes.
 * When it comes time to process the element a toSAX method is provided that
 * will iterate over the contents. 
 * 
 * @author Scott Phillips
 */

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

public abstract class Container extends AbstractWingElement
{

    /** The internal contents of this container */
    protected List<AbstractWingElement> contents = new ArrayList<AbstractWingElement>();

    /**
     * @param context
     *            (Required) The context this element is contained in.
     */
    protected Container(WingContext context) throws WingException
    {
        super(context);
    }

    /**
     * Translate this element and all contained elements into SAX events. The
     * events should be routed to the contentHandler found in the WingContext.
     * 
     * This method does not create an inclosing block, the implementors of
     * container class need to implement a method of toSAX() that provides the
     * surrounding element block for that specific application.
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
     */
    public void toSAX(ContentHandler contentHandler,
            LexicalHandler lexicalHandler, NamespaceSupport namespaces)
            throws SAXException
    {
        for (AbstractWingElement content : contents)
        {
            content.toSAX(contentHandler, lexicalHandler, namespaces);
        }
    }

    /**
     * Dispose
     */
    public void dispose()
    {

        if (this.contents != null)
        {
            for (AbstractWingElement element : contents)
                element.dispose();
            this.contents.clear();
        }
        this.contents = null;
        super.dispose();
    }

}
