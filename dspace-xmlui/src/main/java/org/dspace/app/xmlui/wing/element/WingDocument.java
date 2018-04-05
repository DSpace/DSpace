/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A class representing a WingDocument.
 * 
 * Documents contain three elements and they are all mandatory: meta, body, and
 * options. Because they are all mandatory they are created at construction
 * time.
 * 
 * Note: We called the class "WingDocument" instead of just plain old "Document"
 * so that it won't conflict with all the other documents out there like DOM's
 * document.
 * 
 * @author Scott Phillips
 */
public class WingDocument extends AbstractWingElement implements
        WingMergeableElement
{
    /** The name of the document element */
    public static final String E_DOCUMENT = "document";

    /** The name of the version attribute */
    public static final String A_VERSION = "version";

    /** The document version Wing prefer */
    public static final String DOCUMENT_VERSION = "1.1";

    /** The divisions contained within this body */
    private boolean merged = false;

    /** The meta element */
    private Meta meta;

    /** The body element */
    private Body body;

    /** The options element */
    private Options options;

    /**
     * Generate a new wing document element.
     * 
     * @param context
     *            (Required) The context this element is contained in.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public WingDocument(WingContext context) throws WingException
    {
        super(context);

        // These are all required so we just create them now.
        this.meta = new Meta(context);
        this.body = new Body(context);
        this.options = new Options(context);
    }

    /**
     * Set the meta element of this Document containing all the metadata
     * associated with this document.
     * 
     * @return The Meta element
     * @throws org.dspace.app.xmlui.wing.WingException never.
     */
    public Meta setMeta() throws WingException
    {
        return this.meta;
    }

    /**
     * Set the body element containing the structural elements associated with
     * this document.
     * 
     * @return The Body element.
     * @throws org.dspace.app.xmlui.wing.WingException never.
     */
    public Body setBody() throws WingException
    {
        return this.body;
    }

    /**
     * Set the Options element containing the structural navigational structure
     * associated with this document.
     * 
     * @return The Options element.
     * @throws org.dspace.app.xmlui.wing.WingException never.
     */
    public Options setOptions() throws WingException
    {
        return this.options;
    }

    /**
     * Is this document the same as the given SAX event.
     * 
     * Note: this method will throw an error if the given event's document
     * version number is out of bounds for this implementation of Wing.
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return True if this WingElement is equivalent to the given SAX Event.
     */
    @Override
    public boolean mergeEqual(String namespace, String localName, String qName,
            Attributes attributes) throws SAXException, WingException
    {
        if (!WingConstants.DRI.URI.equals(namespace))
        {
            return false;
        }
        if (!E_DOCUMENT.equals(localName))
        {
            return false;
        }
        
        
        String version = attributes.getValue(A_VERSION);
        if (!(DOCUMENT_VERSION.equals(version)))
        {
            throw new WingException("Incompatible DRI versions, " + DOCUMENT_VERSION + " != " + version);
        }
        
        return true;
    }

    /**
     * Merge the given event into this document.
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
    @Override
    public WingMergeableElement mergeChild(String namespace, String localName,
            String qName, Attributes attributes) throws SAXException,
            WingException
    {
        if (this.meta != null && this.meta.mergeEqual(namespace, localName, qName, attributes))
        {
            Meta child = this.meta;
            this.meta = null;
            return child;
        }

        if (this.body != null && this.body.mergeEqual(namespace, localName, qName, attributes))
        {
            Body child = this.body;
            this.body = null;
            return child;
        }

        if (this.options != null && this.options.mergeEqual(namespace, localName, qName, attributes))
        {
            Options options = this.options;
            this.options = null;
            return options;
        }

        return null;
    }

    /**
     * Notify the element that this document is being merged.
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
     * Translate this document to SAX events.
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
            NamespaceSupport namespaces)
            throws SAXException
    {
        if (!this.merged)
        {
            AttributeMap attributes = new AttributeMap();
            attributes.put(A_VERSION, DOCUMENT_VERSION);
            startElement(contentHandler, namespaces, E_DOCUMENT, attributes);
        }

        if (this.meta != null)
        {
            meta.toSAX(contentHandler, lexicalHandler, namespaces);
        }
        if (this.body != null)
        {
            body.toSAX(contentHandler, lexicalHandler, namespaces);
        }
        if (this.options != null)
        {
            options.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        if (!this.merged)
        {
            endElement(contentHandler, namespaces, E_DOCUMENT);
        }
    }

    /**
     * dispose
     */
    @Override
    public void dispose()
    {
        if (this.meta != null)
        {
            meta.dispose();
        }
        if (this.body != null)
        {
            body.dispose();
        }
        if (this.options != null)
        {
            options.dispose();
        }
        this.meta = null;
        this.body = null;
        this.options = null;
        super.dispose();
    }
//
//    /**
//     * Check the version string and make sure the given version is within the
//     * minimum and maximum allowed document version. If it is outside these
//     * bounds then a WingException is thrown.
//     * 
//     * @param version
//     *            The DRI version to test against.
//     */
//    private void checkVersionString(String version) throws WingException
//    {
//        try
//        {
//            double version_double = Double.valueOf(version);
//            if (version_double < DOCUMENT_VERSION_MINIMUM
//                    || version_double >= DOCUMENT_VERSION_MAXIMUM)
//                throw new WingException(
//                        "Incomptable DRI document merge, unable to merge '"
//                                + version + "' into '"
//                                + DOCUMENT_VERSION_STRING + "'.");
//        }
//        catch (RuntimeException re)
//        {
//            throw new WingException(
//                    "Unable to verrify the DRI document version number.", re);
//        }
//    }

}
