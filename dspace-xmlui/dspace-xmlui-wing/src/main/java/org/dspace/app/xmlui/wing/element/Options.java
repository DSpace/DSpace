/*
 * Options.java
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

import java.util.ArrayList;

import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A class representing a set of navigational options.
 * 
 * @author Scott Phillips
 */
public class Options extends AbstractWingElement implements
        WingMergeableElement
{
    /** The name of the options element */
    public static final String E_OPTIONS = "options";

    /** Has this element been merged? */
    private boolean merged = false;

    /** The lists contained in this Options element */
    private java.util.List<AbstractWingElement> contents = new ArrayList<AbstractWingElement>();

    /**
     * Generate a new Options framework element.
     * 
     * @param context
     *            (Required) The context this element is contained in.
     */
    protected Options(WingContext context) throws WingException
    {
        super(context);
    }

    /**
     * Add a new sublist to this item. Note that an item may contain either
     * characters (with formating & fields) or lists but not both.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * 
     * @param type
     *            (May be null) determines the list type. If this is blank the
     *            list type is infered from the context and use.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return A new sub list.
     */
    public List addList(String name, String type, String rend)
            throws WingException
    {
        List list = new List(context, name, type, rend);
        contents.add(list);
        return list;
    }

    /**
     * Add a new sublist to this item. Note that an item may contain either
     * characters (with formating & fields) or lists but not both.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @return A new sub list.
     */
    public List addList(String name) throws WingException
    {
        return addList(name, null, null);
    }

    /**
     * 
     * Return true if this SAX Event an options element?
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return Return true if this SAX Event an options element?
     */
    public boolean mergeEqual(String namespace, String localName, String qName,
            Attributes attributes)
    {
        // Check if it's in our name space and an options element.
        if (!WingConstants.DRI.URI.equals(namespace))
            return false;
        if (!E_OPTIONS.equals(localName))
            return false;
        return true;
    }

    /**
     * Find the sublist that this SAX event represents.
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element *
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return Return the sublist
     */
    public WingMergeableElement mergeChild(String namespace, String localName,
            String qName, Attributes attributes) throws SAXException,
            WingException
    {
        WingMergeableElement found = null;
        for (AbstractWingElement content : contents)
        {
            if (content instanceof WingMergeableElement)
            {
                WingMergeableElement candidate = (WingMergeableElement) content;
                if (candidate.mergeEqual(namespace, localName, qName,
                        attributes))
                    found = candidate;
            }
        }
        contents.remove(found);
        return found;
    }

    /**
     * Inform the options element that it is being merged with an existing
     * options element.
     * 
     * @return The attributes for this merged element
     */
    public Attributes merge(Attributes attributes) throws SAXException,
            WingException
    {
        this.merged = true;
        return attributes;
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
        if (!merged)
        {
            startElement(contentHandler, namespaces, E_OPTIONS, null);
        }

        for (AbstractWingElement content : contents)
            content.toSAX(contentHandler, lexicalHandler, namespaces);

        if (!merged)
        {
            endElement(contentHandler, namespaces, E_OPTIONS);
        }
    }

    /**
     * dispose
     */
    public void dispose()
    {
        for (AbstractWingElement content : contents)
            content.dispose();
        contents.clear();
        contents = null;
        super.dispose();
    }

}
