/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A class representing the page division.
 *
 * <p>The body contains any number of divisions ({@code div} elements) which
 * group content into interactive and non interactive display blocks.
 *
 * @author Scott Phillips
 */
public class Body extends AbstractWingElement implements WingMergeableElement
{
    /** The name of the body element */
    public static final String E_BODY = "body";

    /** Has this element been merged */
    private boolean merged = false;

    /** The divisions contained within this body */
    private final List<Division> divisions = new ArrayList<>();

    /**
     * Generate a new Body framework element. This method will NOT open or close
     * a body element instead it expects that those events are being handled by
     * the caller. It is important to note that other divisions (div elements)
     * may precede or follow the divisions created through this object.
     * 
     * @param context
     *            (Required) The context this element is contained in.
     * @throws org.dspace.app.xmlui.wing.WingException
     *            passed through.
     */
    protected Body(WingContext context) throws WingException
    {
        super(context);
    }

    /**
     * Append a new division (div element) to the document's body. The division
     * created is not interactive meaning that it may not contain any form
     * elements, to create an interactive division use addInteractiveDivision().
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return a new division.
     * @throws org.dspace.app.xmlui.wing.WingException
     *            passed through.
     */
    public Division addDivision(String name, String rend) throws WingException
    {
        Division div = new Division(context, name, rend);
        divisions.add(div);
        return div;
    }

    /**
     * Append a new division (div element) to the document's body. This is a
     * short cut method for divisions without special rendering instructions.
     * The division created is not interactive meaning that it may not contain
     * any form elements, to create an interactive division use
     * addInteractiveDivision().
     * 
     * @param name
     *            a local identifier used to differentiate the element from its
     *            siblings
     * @return A new division.
     * @throws org.dspace.app.xmlui.wing.WingException
     *            passed through.
     */
    public Division addDivision(String name) throws WingException
    {
        return this.addDivision(name, null);
    }

    /**
     * Append a new interactive division (div element) to the document's body.
     * An interactive division is able to contain form elements as. The extra
     * parameters required such as action and method dictate where and how the
     * form data should be processed.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param action
     *            (Required) determines where the form information should be
     *            sent for processing.
     * @param method
     *            (Required) determines the method used to pass gathered field
     *            values to the handler specified by the action attribute. The
     *            multipart method should be used if there are any file fields
     *            used within the division.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return A new division.
     * @throws org.dspace.app.xmlui.wing.WingException
     *            passed through.
     */
    public Division addInteractiveDivision(String name, String action,
            String method, String rend) throws WingException
    {
        Division div = new Division(context, name, action, method, rend);
        divisions.add(div);
        return div;
    }

    /**
     * Is this SAX event equivalent to this body?
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return True if it is equivalent.
     * @throws org.xml.sax.SAXException
     *            passed through.
     * @throws org.dspace.app.xmlui.wing.WingException
     *            passed through.
     */
    @Override
    public boolean mergeEqual(String namespace, String localName, String qName,
            Attributes attributes) throws SAXException, WingException
    {
        if (!WingConstants.DRI.URI.equals(namespace))
        {
            return false;
        }
        return E_BODY.equals(localName);
    }

    /**
     * Merge this SAX event into the body.
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return the matching Division, or null.
     * @throws org.xml.sax.SAXException
     *            passed through.
     * @throws org.dspace.app.xmlui.wing.WingException
     *            passed through.
     */
    @Override
    public WingMergeableElement mergeChild(String namespace, String localName,
            String qName, Attributes attributes)
	throws SAXException, WingException
    {
        Division found = null;
        for (Division candidate : divisions)
        {
            if (candidate.mergeEqual(namespace, localName, qName, attributes))
            {
                found = candidate;
            }

        }
        divisions.remove(found);
        return found;
    }

    /**
     * Inform this element that it is being merged with an existing body.
     * 
     * @param attributes
     *            The to-be-merged attributes
     * @throws org.xml.sax.SAXException never.
     * @throws org.dspace.app.xmlui.wing.WingException never.
     */
    @Override
    public Attributes merge(Attributes attributes) throws SAXException,
            WingException
    {
        this.merged = true;
        return attributes;
    }

    /**
     * Translate into SAX
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
    @Override
    public void toSAX(ContentHandler contentHandler,
            LexicalHandler lexicalHandler, NamespaceSupport namespaces)
            throws SAXException
    {
        if (!merged)
        {
            startElement(contentHandler, namespaces, E_BODY, null);
        }

        for (Division division : divisions)
        {
            division.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        if (!merged)
        {
            endElement(contentHandler, namespaces, E_BODY);
        }
    }

    /**
     * dispose
     */
    @Override
    public void dispose()
    {
        for (Division division : divisions)
        {
            division.dispose();
        }
        super.dispose();
    }

}
