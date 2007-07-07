/*
 * ReferenceSet.java
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

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Class representing a set of referenced metadata.
 * 
 * @author Scott Phillips
 */

public class ReferenceSet extends AbstractWingElement implements
        StructuralElement
{
    /** The name of the referenceSet element */
    public static final String E_REFERENCE_SET = "referenceSet";

    /** The name of the orderBy attribute */
    public static final String A_ORDER_BY = "orderBy";

    /** The name of the type attribute */
    public static final String A_TYPE = "type";

    /** The possible interactive division methods: get,post, or multipart. */
    public static final String TYPE_SUMMARY_LIST = "summaryList";

    public static final String TYPE_SUMMARY_VIEW = "summaryView";

    public static final String TYPE_DETAIL_LIST = "detailList";

    public static final String TYPE_DETAIL_VIEW = "detailView";

    /** The possible interactive division methods names collected into one array */
    public static final String[] TYPES = { TYPE_SUMMARY_LIST, TYPE_SUMMARY_VIEW, TYPE_DETAIL_LIST, TYPE_DETAIL_VIEW };

    /** The name assigned to this metadata set */
    private String name;

    /** The ordering mechanism to use. */
    private String orderBy;

    /** The reference type, see TYPES defined above */
    private String type;

    /** Special rendering instructions */
    private String rend;

    /** The head label for this referenceset */
    private Head head;

    /** All content of this container, items & lists */
    private java.util.List<AbstractWingElement> contents = new ArrayList<AbstractWingElement>();

    /**
     * Construct a new referenceSet
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @param informationModel
     *            (May be null) The information model the enclosed objects
     *            follow. If no model is given then the default information
     *            model is used. (INFORMATION_MODEL_DEFAULT)
     * @param type
     *            (Required) The type of reference set which determines the level
     *            of detail for the metadata rendered. See TYPES for a list of
     *            available types.
     * @param orderBy
     *            (May be null) Determines the ordering of referenced metadata.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     */
    protected ReferenceSet(WingContext context, boolean childreference, String name, String type, String orderBy, String rend)
            throws WingException
    {
        super(context);
        // Names are only required for parent reference sets.
        if (!childreference)
            require(name, "The 'name' parameter is required for reference sets.");
        restrict(
                type,
                TYPES,
                "The 'method' parameter must be one of these values: 'summaryList', 'summaryView', 'detailList', or 'detailView'.");

        this.name = name;
        this.type = type;
        this.orderBy = orderBy;
        this.rend = rend;
    }

    /**
     * Set the head element which is the label associated with this referenceset.
     * 
     * @param characters
     *            (May be null) Unprocessed characters to be referenced
     */
    public Head setHead() throws WingException
    {
        this.head = new Head(context, null);
        return head;

    }

    /**
     * Set the head element which is the label associated with this referenceset.
     * 
     * @param characters
     *            (May be null) Unprocessed characters to be referenced
     */
    public void setHead(String characters) throws WingException
    {
        Head head = this.setHead();
        head.addContent(characters);

    }

    /**
     * Set the head element which is the label associated with this referenceset.
     * 
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     */
    public void setHead(Message message) throws WingException
    {
        Head head = this.setHead();
        head.addContent(message);
    }

    /**
     * Add an object refrence.
     * 
     * @param object
     *            (Required) The referenced object.
     */
    public Reference addReference(Object object)
            throws WingException
    {
        Reference reference = new Reference(context, object);
        contents.add(reference);
        return reference;
    }

    /**
     * Translate this metadata inclusion set to SAX
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
            attributes.put(A_ID, context.generateID(E_REFERENCE_SET, name));

        attributes.put(A_TYPE, type);
        if (orderBy != null)
            attributes.put(A_ORDER_BY, orderBy);
        if (rend != null)
            attributes.put(A_RENDER, rend);

        startElement(contentHandler, namespaces, E_REFERENCE_SET, attributes);

        if (head != null)
            head.toSAX(contentHandler, lexicalHandler, namespaces);

        for (AbstractWingElement content : contents)
        {
            content.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        endElement(contentHandler, namespaces, E_REFERENCE_SET);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        for (AbstractWingElement content : contents)
            content.dispose();
        if (contents != null)
            contents.clear();
        contents = null;
        super.dispose();
    }
}
