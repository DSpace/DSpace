/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
 * This is a class representing an individual metadata field in pseudo-dublin core
 * format. The metadata elements carries generic metadata information in the
 * form of an attribute-value pair.
 * 
 * @author Scott Phillips
 */

public class Metadata extends TextContainer implements MetadataElement
{
    /** The name of the metadata element */
    public static final String E_METADATA = "metadata";

    /** The name of the element attribute */
    public static final String A_ELEMENT = "element";

    /** The name of the qualifier attribute */
    public static final String A_QUALIFIER = "qualifier";

    /** The name of the language attribute */
    public static final String A_LANGUAGE = "lang";

    /** The metadata's element */
    private final String element;

    /** The metadata's qualifier */
    private final String qualifier;

    /** The metadata's language */
    private final String language;
    
    /** 
     * Determine the additive model for the metadata, should 
     * the metadata always be added to the document or only if 
     * it does not already exist?
     */
    private final boolean allowMultiple;

    /**
	 * Construct a new metadata.
	 * 
     * @param context
     *            (Required) The request context.
	 * @param element
	 *            (Required) The element of this metadata
	 * @param qualifier
	 *            (May be null) The qualifier of this metadata
	 * @param language
	 *            (May be null) The language of this metadata
	 * @param allowMultiple
	 *            (Required) Are multiple metadata elements with the same element,
	 *            qualifier, and language allowed?
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
	 */
    protected Metadata(WingContext context, String element, String qualifier,
            String language, boolean allowMultiple) throws WingException
    {
        super(context);
        this.element = element;
        this.qualifier = qualifier;
        this.language = language;
        this.allowMultiple = allowMultiple;
    }

    /**
     * If an metadata with the same element, qualifier, and language exist
     * within the document should this metadata element be added into the
     * document or should only one metadata be allowed.
     *
     * @return true if multiple values are allowed.
     */
    protected boolean allowMultiple()
    {
    	return this.allowMultiple;
    }
    
    /**
     * Determine if the given element, qualifier, and lang are equal to this
     * metadata.
     * 
     * @param element
     *            (Required) The element of this metadata
     * @param qualifier
     *            (May be null) The qualifier of this metadata
     * @param language
     *            (May be null) The language of this metadata
     * @return True if the given parameters are equal to this metadata.
     */
    protected boolean equals(String element, String qualifier, String language)
    {
        // Element should never be null.
        if (this.element == null || element == null)
        {
            return false;
        }

        if (!stringEqualsWithNulls(this.element, element))
        {
            return false;
        }
        if (!stringEqualsWithNulls(this.qualifier, qualifier))
        {
            return false;
        }
        if (!stringEqualsWithNulls(this.language, language))
        {
            return false;
        }

        // Element, qualifier, and language are equal.
        return true;
    }

    /**
     * This is just a silly private method to make the method above easier to
     * read. Compare the two parameters to see if they are equal while taking
     * into account nulls. So if both values are null that is considered an
     * equals.
     * 
     * The method is meant to replace the syntax current.equals(test) so that it
     * works when current is null.
     * 
     * @param current
     *            (May be null) The current value.
     * @param test
     *            (May be null) The value to be compared to current
     * @return If the strings are equal.
     */
    private boolean stringEqualsWithNulls(String current, String test)
    {
        if (current == null)
        {
            return (test == null);
        }
        else
        {
            return current.equals(test);
        }
    }

    /**
     * Translate into SAX events.
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
     * @throws org.xml.sax.SAXException passed through.
     */
    @Override
    public void toSAX(ContentHandler contentHandler, LexicalHandler lexicalHandler,
            NamespaceSupport namespaces) throws SAXException
    {
        AttributeMap attributes = new AttributeMap();
        attributes.put(A_ELEMENT, element);
        if (this.qualifier != null)
        {
            attributes.put(A_QUALIFIER, qualifier);
        }
        if (this.language != null)
        {
            attributes.put(A_LANGUAGE, language);
        }

        startElement(contentHandler, namespaces, E_METADATA, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_METADATA);
    }
}
