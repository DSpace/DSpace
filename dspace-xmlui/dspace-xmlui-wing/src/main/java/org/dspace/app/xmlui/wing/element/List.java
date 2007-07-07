/*
 * List.java
 *
 * Version: $Revision: 1.10 $
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
 * A class that represents a List element.
 * 
 * The list element is used to display sets of sequential data. It contains an
 * optional head element, as well as any number of item elements. Items contain
 * textual information or other list elements. An item can also be associated
 * with a label element that annotates an item with a number, a textual
 * description of some sort, or a simple bullet. The list type (ordered,
 * bulletted, gloss, etc.) is then determined either by the content of labels on
 * items or by an explicit value of the "type" attribute. Note that if labels
 * are used in conjunction with any items in a list, all of the items in that
 * list must have a label. It is also recommended to avoid mixing label styles
 * unless an explicit type is specified.
 * 
 * typically rendering types are not predefined, but for lists there is a set of
 * standard rendering options available for the rend attribute to be set too.
 * This is not an exhaustive list.
 * 
 * horizontal: The list should be rendered horizontally.
 * 
 * vertical: The list should be rendered vertically.
 * 
 * columns: The list should be rendered in equal length columns as determined by
 * the theme.
 * 
 * columns2: The list should be rendered in two equal columns.
 * 
 * columns3: The list should be rendered in three equal columns.
 * 
 * alphabet: The list should be rendered as an alphabetical index.
 * 
 * numeric: The list should be rendered as a numeric index.
 * 
 * @author Scott Phillips
 */

import java.util.ArrayList;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

public class List extends AbstractWingElement implements WingMergeableElement,
        StructuralElement
{
    /** The name of the list element */
    public static final String E_LIST = "list";

    /** The name of the type attribute */
    public static final String A_TYPE = "type";

    /** Has this element been merged? */
    private boolean merged = false;
    
    /** Has a child element been merged: head, list, or item */
    private boolean childMerged = false;

    /** The possible list types * */
    public static final String TYPE_SIMPLE = "simple";

    public static final String TYPE_ORDERED = "ordered";

    public static final String TYPE_BULLETED = "bulleted";

    public static final String TYPE_GLOSS = "gloss";

    public static final String TYPE_PROGRESS = "progress";
    
    public static final String TYPE_FORM = "form";

    /** All the possible list types collected into one array */
    public static final String[] TYPES = { TYPE_SIMPLE, TYPE_ORDERED,
            TYPE_BULLETED, TYPE_GLOSS, TYPE_PROGRESS, TYPE_FORM };

    /** The list's name */
    private String name;

    /** The list's type, see types above. * */
    private String type;

    /** Any special rendering instructions * */
    private String rend;

    /** The lists head * */
    private Head head;

    /** All content of this container, items & lists */
    private java.util.List<AbstractWingElement> contents = new ArrayList<AbstractWingElement>();

    /**
     * Construct a new list.
     * 
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param type
     *            (May be null) determines the list type. If this is blank the
     *            list type is inferred from the context and use.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element. There are a set of predefined
     *            rendering values, see the class documentation above.
     */
    protected List(WingContext context, String name, String type, String rend)
            throws WingException
    {
        super(context);
        require(name, "The 'name' parameter is required for all lists.");
        restrict(
                type,
                TYPES,
                "The 'type' parameter must be one of these values: 'simple', 'ordered', 'bulleted', 'gloss', or 'form'.");

        this.name = name;
        this.type = type;
        this.rend = rend;
    }

    /**
     * Set the head element which is the label associated with this list. This
     * method should be called before any other elements have been added to the
     * list.
     * 
     * @param characters
     *            (May be null) Untranslated character data to be included as
     *            the list's head.
     */
    public Head setHead() throws WingException
    {
        Head head = new Head(context, null);
        this.head = head;
        return head;
    }

    /**
     * Set the head element which is the label associated with this list. This
     * method should be called before any other elements have been added to the
     * list.
     * 
     * @param characters
     *            (Required) Untranslated character data to be included as the
     *            list's head.
     */
    public void setHead(String characters) throws WingException
    {
        Head head = setHead();
        head.addContent(characters);
    }

    /**
     * Set the head element which is the label associated with this list. This
     * method should be called before any other elements have been added to the
     * list.
     * 
     * @param key
     *            (Required) Key to the i18n catalogue to translate the content
     *            into the language preferred by the user.
     */
    public void setHead(Message key) throws WingException
    {
        Head head = setHead();
        head.addContent(key);
    }

    /**
     * Add a label element, they are associated with an item and annotates that
     * item with a number, a textual description of some sort, or a simple
     * bullet.
     * 
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     */
    public Label addLabel(String name, String rend) throws WingException
    {
        Label label = new Label(context, name, rend);
        contents.add(label);
        return label;
    }

    /**
     * Add a label element, they are associated with an item and annotates that
     * item with a number, a textual description of some sort, or a simple
     * bullet.
     * 
     * @param characters
     *            (Required) Untranslated character data to be included.
     */
    public void addLabel(String characters) throws WingException
    {
        require(characters,
                "The 'characters' parameter is required for list labels.");

        Label label = new Label(context, null, null);
        label.addContent(characters);
        contents.add(label);
    }

    /**
     * Add a label element, they are associated with an item and annotates that
     * item with a number, a textual description of some sort, or a simple
     * bullet. This version of label provides no textual label but may be used 
     * to indicate some implicit labeling such as ordered lists.
     * 
     */
    public void addLabel() throws WingException
    {
        Label label = new Label(context, null, null);
        contents.add(label);
    }
    
    /**
     * Add a label element, they are associated with an item and annotates that
     * item with a number, a textual description of some sort, or a simple
     * bullet.
     * 
     * @param key
     *            (Required) Key to the i18n catalogue to translate the content
     *            into the language preferred by the user.
     */
    public void addLabel(Message key) throws WingException
    {
        require(key, "The 'key' parameter is required for list labels.");

        Label label = new Label(context, null, null);
        label.addContent(key);
        contents.add(label);
    }

    /**
     * Add an empty unnamed item.
     * 
     * @return a new Item
     */
    public Item addItem() throws WingException
    {
        return addItem(null,null);
    }
    
    /**
     * Add an item element, which serves a dual purpose. It can contain other
     * lists, allowing for hierarchies and recursive lists. Alternatively it can
     * serve as a character container to display textual data, possibly enhanced
     * with hyperlinks, emphasized blocks of text, images and form fields. An
     * item cannot be both a character container and contain a list.
     * 
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element. *
     * @return a new Item
     */
    public Item addItem(String name, String rend) throws WingException
    {
        Item item = new Item(context, name, rend);
        contents.add(item);
        return item;
    }

    /**
     * Add an item element that contains only character content.
     * 
     * @param characters
     *            (Required) Untranslated character data to be included.
     */
    public void addItem(String characters) throws WingException
    {
        require(characters,
                "The 'characters' parameter is required for list items.");

        Item item = this.addItem(null, null);
        item.addContent(characters);
    }

    /**
     * Add an item element that contains only translated content.
     * 
     * @param key
     *            (Required) Key to the i18n catalogue to translate the content
     *            into the language preferred by the user.
     */
    public void addItem(Message key) throws WingException
    {
        require(key, "The 'key' parameter is required for list items.");

        Item item = this.addItem(null, null);
        item.addContent(key);
    }

    /**
     * Add an item to the list that contains a link. The link will consist of
     * the given content and linked to the given target.
     * 
     * @param target
     *            (Required) The link target.
     * @param characters
     *            (Required) Untranslated character data to be included as the
     *            link's body.
     */
    public void addItemXref(String target, String characters)
            throws WingException
    {
        Item item = this.addItem(null, null);
        item.addXref(target, characters);
    }

    /**
     * Add an item to the list that contains a link. The link will consist of
     * the given content and linked to the given target.
     * 
     * @param target
     *            (Required) The link target.
     * @param key
     *            (Required) i18n key for translating content into the user's
     *            preferred language.
     */
    public void addItemXref(String target, Message key) throws WingException
    {

        Item item = this.addItem(null, null);
        item.addXref(target, key);
    }

    /**
     * Add a new sublist to this list.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param type
     *            (May be null) determines the list type. If this is blank the
     *            list type is inferred from the context and use.
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
     * Add a new sublist to this list.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param type
     *            (May be null) determines the list type. If this is blank the
     *            list type is inferred from the context and use.
     * @return A new sub list.
     */
    public List addList(String name, String type)
            throws WingException
    {
        List list = new List(context, name, type, null);
        contents.add(list);
        return list;
    }
    
    /**
     * Add a new sublist to this list.
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
     * Determine if the given SAX startElement event is equivalent to this list.
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return True if this list is equivalent to the given SAX Event.
     */
    public boolean mergeEqual(String namespace, String localName, String qName,
            Attributes attributes)
    {
        // Check if it's in our name space and an options element.
        if (!WingConstants.DRI.URI.equals(namespace))
            return false;
        if (!E_LIST.equals(localName))
            return false;
        String name = attributes.getValue(A_NAME);
        if (name == null)
            return false;
        if (!name.equals(this.name))
            return false;
        return true;
    }

    /**
     * Merge the given SAX startElement event into this list's child. If this
     * SAX event matches a child element of this list then it should be removed
     * from the internal book keep of this element and returned. Typically this
     * is accomplished by looping through all children elements and returned the
     * first one that returns true for the mergeEqual method.
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element *
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return The child element
     */
    public WingMergeableElement mergeChild(String namespace, String localName,
            String qName, Attributes attributes) throws SAXException,
            WingException
    {
    	this.childMerged = true;
    	
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
     * Inform this list that it is being merged with an existing element.
     * Practically this means that when this method is being transformed to SAX
     * it should assume that the element's SAX events have all ready been sent.
     * In this case the element would only need to transform to SAX the children
     * of this element.
     * 
     * Further more if the element needs to add any attributes to the SAX
     * startElement event it may modify the attributes object passed to make
     * changes.
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

        if (this.merged == false)
        {
            AttributeMap attributes = new AttributeMap();
            attributes.put(A_NAME, this.name);
            attributes.put(A_ID, this.context.generateID(E_LIST, this.name));
            if (this.type != null)
                attributes.put(A_TYPE, this.type);
            if (this.rend != null)
                attributes.put(A_RENDER, this.rend);

            startElement(contentHandler, namespaces, E_LIST, attributes);
            
        }
            
        if (childMerged == false && head != null)
        	head.toSAX(contentHandler, lexicalHandler, namespaces);

        for (AbstractWingElement content : contents)
            content.toSAX(contentHandler, lexicalHandler,  namespaces);

        if (this.merged == false)
            endElement(contentHandler, namespaces, E_LIST);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        if (head != null)
            head.dispose();
        head = null;
        for (AbstractWingElement content : contents)
            content.dispose();
        contents.clear();
        contents = null;
        super.dispose();
    }
}
