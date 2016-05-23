/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
    private java.util.List<AbstractWingElement> contents = new ArrayList<>();

    /**
     * Generate a new Options framework element.
     * 
     * @param context
     *            (Required) The context this element is contained in.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Options(WingContext context) throws WingException
    {
        super(context);
    }

    /**
     * Add a new sublist to this item. Note that an item may contain either
     * characters (with formating and fields) or lists but not both.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * 
     * @param type
     *            (May be null) determines the list type. If this is blank the
     *            list type is inferred from the context and use.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return A new sub list.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
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
     * characters (with formating and fields) or lists but not both.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @return A new sub list.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
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
    @Override
    public boolean mergeEqual(String namespace, String localName, String qName,
            Attributes attributes)
    {
        // Check if it's in our name space and an options element.
        if (!WingConstants.DRI.URI.equals(namespace))
        {
            return false;
        }
        return E_OPTIONS.equals(localName);
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
    @Override
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
                if (candidate.mergeEqual(namespace, localName, qName, attributes))
                {
                    found = candidate;
                }
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
    @Override
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
    @Override
    public void toSAX(ContentHandler contentHandler, LexicalHandler lexicalHandler,
            NamespaceSupport namespaces) throws SAXException
    {
        if (!merged)
        {
            startElement(contentHandler, namespaces, E_OPTIONS, null);
        }

        for (AbstractWingElement content : contents)
        {
            content.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        if (!merged)
        {
            endElement(contentHandler, namespaces, E_OPTIONS);
        }
    }

    /**
     * dispose
     */
    @Override
    public void dispose()
    {
        for (AbstractWingElement content : contents)
        {
            content.dispose();
        }
        contents.clear();
        contents = null;
        super.dispose();
    }

}
