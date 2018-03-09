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

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A class representing a set of metadata about the user generating this page.
 * 
 * @author Scott Phillips
 */
public class UserMeta extends AbstractWingElement implements
        WingMergeableElement, MetadataElement
{
    /** The name of the userMeta element */
    public static final String E_USER_META = "userMeta";

    /** The name of the authenticated attribute */
    public static final String A_AUTHENTICATED = "authenticated";

    /** Has this UserMeta element been merged? */
    private boolean merged = false;

    /** Has this user been authenticated? */
    private boolean authenticated = false;

    /** The metadata contents of this UserMeta element */
    private List<Metadata> metadatum = new ArrayList<Metadata>();

    /**
     * Construct a new userMeta
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected UserMeta(WingContext context) throws WingException
    {
        super(context);
    }

    /**
     * Set the user described in the meta object as authenticated.
     * 
     * @param authenticated
     *            (Required) True if the user is authenticated, false otherwise.
     */
    public void setAuthenticated(boolean authenticated)
    {
        this.authenticated = authenticated;
    }

    /**
	 * Add metadata about the requesting user to the document.
	 * 
	 * @param element
	 *            (Required) The metadata element.
	 * @param qualifier
	 *            (May be null) The metadata qualifier.
	 * @param language
	 *            (May be null) The metadata's language
	 * @param allowMultiple
	 *            (Required) determine if multiple metadata elements with the same
	 *            element, qualifier and language are allowed.
	 * @return A new metadata
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
	 */
    public Metadata addMetadata(String element, String qualifier,
            String language, boolean allowMultiple) throws WingException
    {
        Metadata metadata = new Metadata(context, element, qualifier, language, allowMultiple);
        metadatum.add(metadata);
        return metadata;
    }

    /**
     * Add metadata about the requesting user to the document.
     * 
     * @param element
     *            (Required) The metadata element.
     * @param qualifier
     *            (May be null) The metadata qualifier.
     * @param language
     *            (May be null) The metadata's language
     * @return A new metadata
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Metadata addMetadata(String element, String qualifier,
            String language) throws WingException
    {
    	 return addMetadata(element, qualifier, null, false);
    }
    
    /**
     * Add metadata about the requesting user to the document.
     * 
     * @param element
     *            (Required) The metadata element.
     * @param qualifier
     *            (May be null) The metadata qualifier.
     * @return A new metadata
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Metadata addMetadata(String element, String qualifier)
            throws WingException
    {
        return addMetadata(element, qualifier, null, false);
    }

    /**
     * Add metadata about the requesting user to the document.
     * 
     * @param element
     *            (Required) The metadata element.
     * @return A new metadata
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Metadata addMetadata(String element) throws WingException
    {
        return addMetadata(element, null, null, false);
    }

    /**
     * Determine if the given SAX event is a UserMeta element.
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

        if (!E_USER_META.equals(localName))
        {
            return false;
        }
        return true;
    }

    /**
     * Since metadata can not be merged there are no mergeable children. This
     * just return's null.
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
    	// We don't merge our children but we do have one special optimization: 
    	// if a metadata is already in the document and it is tagged as not allowing 
    	// multiples then we do not add the new metadata to the document.
    	
    	if (WingConstants.DRI.URI.equals(namespace) && Metadata.E_METADATA.equals(localName))
    	{
    		String element = attributes.getValue(Metadata.A_ELEMENT);
    		String qualifier = attributes.getValue(Metadata.A_QUALIFIER);
    		String language = attributes.getValue(Metadata.A_LANGUAGE);
    		
    		List<Metadata> remove = new ArrayList<Metadata>();
    		for (Metadata metadata : metadatum)
    		{
    			if (metadata.equals(element,qualifier,language) && !metadata.allowMultiple())
    			{
    				remove.add(metadata);
    			}
    		}
    		
    		// Remove all the metadata elements we found.
    		for (Metadata metadata : remove)
    		{
    			metadata.dispose();
    			metadatum.remove(metadata);
    		}
    	}
    	
        return null;
    }

    /**
     * Inform this element that it is being merged with an existing element.
     */
    @Override
    public Attributes merge(Attributes attributes) throws SAXException,
            WingException
    {
        this.merged = true;

        String mergedAuthenticated = attributes.getValue(A_AUTHENTICATED);

        if ("yes".equals(mergedAuthenticated))
        {
            // The user has already been set to authenticated.
            // Do nothing.
        }
        else if ("no".equals(mergedAuthenticated))
        {
            // No authenticated user yet.
            if (this.authenticated)
            {
                // Original no, but we've been told that the user is
                // authenticated.
                AttributesImpl attributesImpl = new AttributesImpl(attributes);
                int index = attributesImpl.getIndex(A_AUTHENTICATED);
                if (index >= 0)
                {
                	attributesImpl.setValue(index,"yes");
                } 
                else
                {
	                attributesImpl.addAttribute("",
	                        A_AUTHENTICATED, A_AUTHENTICATED, "CDATA", "yes");
                }
                attributes = attributesImpl;
            }
        }
        else
        {
            // Authenticated value does not conform to the schema.
            AttributesImpl attributesImpl = new AttributesImpl(attributes);
            attributesImpl.addAttribute("", A_AUTHENTICATED,
                    A_AUTHENTICATED, "CDATA", (this.authenticated ? "yes" : "no"));
            attributes = attributesImpl;

        }

        return attributes;
    }

    /**
     * Translate this element into SAX events.
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
            AttributeMap attributes = new AttributeMap();
            if (authenticated)
            {
                attributes.put(A_AUTHENTICATED, "yes");
            }
            else
            {
                attributes.put(A_AUTHENTICATED, "no");
            }
            startElement(contentHandler, namespaces, E_USER_META, attributes);
        }

        for (Metadata metadata : metadatum)
        {
            metadata.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        if (!merged)
        {
            endElement(contentHandler, namespaces, E_USER_META);
        }
    }

    /**
     * dispose
     */
    @Override
    public void dispose()
    {
        for (AbstractWingElement content : metadatum)
        {
            content.dispose();
        }
        metadatum.clear();
        metadatum = null;
        super.dispose();
    }
}
