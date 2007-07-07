/*
 * AbstractWingElement.java
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

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Namespace;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.WingInvalidArgument;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class represents a generic element inside the Wing framework.
 * 
 * The primary purpose of this abstract class is to create a generic datatype
 * for storage of any WingElement. Second, this class also provides a set of
 * utilities for easy maintenance of each wing element. There are a set of
 * methods for sending SAX events that handle namespaces and attributes so that
 * each individual wing element does not.
 * 
 * There are also a set of utility methods for checking method parameters.
 * 
 * @author Scott Phillips
 */
public abstract class AbstractWingElement implements WingElement
{

    /**
     * The current context this element is operating under. From the context
     * contentHandler, namespace support, and unique id generation can be found.
     * This context is shared between many elements.
     */
    protected WingContext context;

    /**
     * Construct a new WingElement. All wing elements must have access to a
     * current WingContext.
     * 
     * @param context
     *            (Required) The context this element is contained in.
     * @throws WingException
     */
    protected AbstractWingElement(WingContext context)
    {
        if (context == null)
            throw new NullPointerException("Context may not be null.");
        this.context = context;
    }

    /**
     * Construct a new WingElement without access to the current wing Context.
     * This means that the wing element will not be able to know the component
     * name because the context is not available.
     * 
     * This invocation method is intended for wing element implementations that
     * are outside the wing pacakage.
     */
    protected AbstractWingElement()
    {

    }

    /**
     * Return the currently registered wing context.
     * 
     */
    protected WingContext getWingContext()
    {
        return this.context;
    }

    /**
     * Set the WingContext, note there are potential side effects of changing
     * the WingContext once it has been used. This context should not be changed
     * after elements have been added to a wingElement.
     * 
     * @param context
     *            The new WingContext.
     */
    protected void setWingContext(WingContext context)
    {
        this.context = context;
    }

    /**
     * These methods: require, restrict, greater, lesser, greater, requireFalse,
     * requireTrue are simple methods to describe the restrictions and
     * requirements of attribute values.
     */

    /**
     * Check to make sure the parameter is not null or an empty string.
     * 
     * @param parameter
     *            A non null and none empty string
     * @param message
     *            The exception message thrown if parameter is invalid.
     */
    protected void require(String parameter, String message)
            throws WingInvalidArgument
    {
        if (parameter == null || parameter.equals(""))
            throw new WingInvalidArgument(message);
    }

    /**
     * Check to make sure the parameter is not null or an empty string.
     * 
     * @param parameter
     *            A non null and none empty string
     * @param message
     *            The exception message thrown if parameter is invalid.
     */
    protected void require(Message parameter, String message)
            throws WingInvalidArgument
    {
        if (parameter == null)
            throw new WingInvalidArgument(message);
    }

    /**
     * Check to make sure that the parameter is a member of one of the options.
     * This method will accept null values, if you need to restrict and not
     * allow null values then use the require() method in conjunction with this
     * method.
     * 
     * @param parameter
     *            A null string, or a member of the options array.
     * @param options
     *            A list of possible values for the parameter.
     * @param message
     *            The exception message thrown if the parameter is invalid.
     */
    protected void restrict(String parameter, String[] options, String message)
            throws WingInvalidArgument
    {
        if (parameter == null)
            return;

        for (String test : options)
            if (parameter.equals(test))
                return; // short circuit the method call.

        throw new WingInvalidArgument(message);
    }

    /**
     * Check to make sure that the parameter is GREATER THAN (note: not equal
     * to) the given greater variable.
     * 
     * @param parameter
     *            An int who's value is greater than greater.
     * @param greater
     *            An int who's value is lesser that greater.
     * @param message
     *            The exception message thrown if the parameter is invalid.
     */
    protected void greater(int parameter, int greater, String message)
            throws WingInvalidArgument
    {
        if (parameter <= greater)
            throw new WingInvalidArgument(message);
    }

    /**
     * Check to make sure that the parameter is LESS THAN (note: not equal to)
     * the given lesser variable.
     * 
     * @param parameter
     *            An int who's value is less than lesser.
     * @param greater
     *            An int who's value is greater that lesser.
     * @param message
     *            The exception message thrown if the parameter is invalid.
     */
    protected void lesser(int parameter, int lesser, String message)
            throws WingInvalidArgument
    {
        if (parameter >= lesser)
            throw new WingInvalidArgument(message);
    }

    /**
     * Check to make sure that the boolean test value is false.
     * 
     * @param test
     *            A false value.
     * @param message
     *            The exception message thrown if "test" is invalid.
     */
    protected void requireFalse(boolean test, String message)
            throws WingInvalidArgument
    {
        if (test)
            throw new WingInvalidArgument(message);
    }

    /**
     * Check to make sure that the boolean test value is true.
     * 
     * @param test
     *            A true value.
     * @param message
     *            The exception message thrown if "test" is invalid.
     */
    protected void requireTrue(boolean test, String message)
            throws WingInvalidArgument
    {
        if (!test)
            throw new WingInvalidArgument(message);
    }
    
    /**
     * Send the SAX event to start this element.
     * 
     * Assume the DRI namespace.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @param name
     *            (Required) The element's localName
     * @param attributes
     *            (May be null) Attributes for this element.
     */
    protected void startElement(ContentHandler contentHandler,
            NamespaceSupport namespaces, String name, AttributeMap attributes)
            throws SAXException
    {
        startElement(contentHandler, namespaces, WingConstants.DRI, name,
                attributes);
    }

    /**
     * Send the SAX events to start this element.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @param namespace
     *            (Required) The namespace of this element.
     * @param name
     *            (Required) The local name of this element.
     * @param attributes
     *            (May be null) Attributes for this element
     */
    protected void startElement(ContentHandler contentHandler,
            NamespaceSupport namespaces, Namespace namespace, String name,
            AttributeMap attributes) throws SAXException
    {
        String prefix = namespaces.getPrefix(namespace.URI);
        contentHandler.startElement(namespace.URI, name, qName(prefix, name),
                map2sax(namespace, namespaces, attributes));
    }

    /**
     * Send the SAX event for these plain characters, not wrapped in any
     * elements.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param characters
     *            (May be null) Characters to send.
     */
    protected void sendCharacters(ContentHandler contentHandler,
            String characters) throws SAXException
    {
        if (characters != null)
        {
            char[] contentArray = characters.toCharArray();
            contentHandler.characters(contentArray, 0, contentArray.length);
        }
    }

    /**
     * Send the SAX events to end this element.
     * 
     * Assume the DRI namespace.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @param name
     *            (Required) The localName of this element.
     */
    protected void endElement(ContentHandler contentHandler,
            NamespaceSupport namespaces, String name) throws SAXException
    {
        endElement(contentHandler, namespaces, WingConstants.DRI, name);
    }

    /**
     * Send the SAX events to end this element.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @param namespace
     *            (Required) The namespace of this element.
     * @param name
     *            (Required) The local name of this element.
     */
    protected void endElement(ContentHandler contentHandler,
            NamespaceSupport namespaces, Namespace namespace, String name)
            throws SAXException
    {
        String prefix = namespaces.getPrefix(namespace.URI);
        contentHandler.endElement(namespace.URI, name, qName(prefix, name));
    }

    /**
     * Build the SAX attributes object based upon Java's String Map.
     * 
     * @param namespaces
     *            SAX Helper class to keep track of namespaces able to determine
     *            the correct prefix for a given namespace URI.
     * @param attributeMap
     *            Map of attributes and values.
     * @return SAX Attributes object of the given map.
     */
    private Attributes map2sax(Namespace elementNamespace,NamespaceSupport namespaces,
            AttributeMap attributeMap)
    {
        return map2sax(elementNamespace,namespaces, null, attributeMap);
    }

    /**
     * Build the SAX attributes object based upon Java's String map. This
     * convenience method will build, or add to an existing attributes object,
     * the attributes detailed in the AttributeMap.
     * 
     * @param namespaces
     *            SAX Helper class to keep track of namespaces able to determine
     *            the correct prefix for a given namespace URI.
     * @param attributes
     *            An existing SAX AttributesImpl object to add attributes too.
     *            If the value is null then a new attributes object will be
     *            created to house the attributes.
     * @param attributeMap
     *            A map of attributes and values.
     * @return
     */
    private AttributesImpl map2sax(Namespace elementNamespace,
            NamespaceSupport namespaces, AttributesImpl attributes,
            AttributeMap attributeMap)
    {

        if (attributes == null)
            attributes = new AttributesImpl();
        if (attributeMap != null)
        {
            // Figure out the namespace issue
            Namespace namespace = attributeMap.getNamespace();
            String URI;
            if (namespace != null)
                URI = namespace.URI;
            else
                URI = WingConstants.DRI.URI;

            String prefix = namespaces.getPrefix(URI);

            // copy each one over.
            for (String name : attributeMap.keySet())
            {
                String value = attributeMap.get(name);
                if (value == null)
                    continue;

                // If the indended namespace is the element's namespace then we
                // leave
                // off the namespace declaration because w3c say's its redundent
                // and breaks lots of xsl stuff.
                if (elementNamespace.URI.equals(URI))
                    attributes.addAttribute("", name, name, "CDATA", value);
                else
                    attributes.addAttribute(URI, name, qName(prefix, name),
                            "CDATA", value);
            }
        }
        return attributes;
    }

    /**
     * Create the qName for the element with the given localName and namespace
     * prefix.
     * 
     * @param prefix
     *            (May be null) The namespace prefix.
     * @param localName
     *            (Required) The element's local name.
     * @return
     */
    private String qName(String prefix, String localName)
    {
        if (prefix == null || prefix.equals(""))
            return localName;
        else
            return prefix + ":" + localName;
    }

    /**
     * Dispose
     */
    public void dispose()
    {
        this.context = null;
    }
}
