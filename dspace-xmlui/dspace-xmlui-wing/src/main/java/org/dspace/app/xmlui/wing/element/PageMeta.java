/*
 * PageMeta.java
 *
 * Version: $Revision: 1.6 $
 *
 * Date: $Date: 2006/08/08 21:59:25 $
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
import java.util.List;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A class representing a set of metadata about the page being generated.
 * 
 * @author Scott Phillips
 */
public class PageMeta extends AbstractWingElement implements
        WingMergeableElement, MetadataElement
{
    /** Name of the pageMeta element */
    public final static String E_PAGE_META = "pageMeta";

    /** Has this PageMeta element been merged? */
    private boolean merged = false;


    /** 
     * A page meta may hold two types of elements, trails or 
     * metadata. Each of these types are seperated so that 
     * we can search through each time as we merge documents.
     */
    private List<Metadata> metadatum = new ArrayList<Metadata>();
    private List<Trail> trails = new ArrayList<Trail>();

    /**
     * Construct a new pageMeta
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     */
    protected PageMeta(WingContext context) throws WingException
    {
        super(context);
    }

    /**
	 * Add metadata about this page.
	 * 
	 * @param element
	 *            (Required) The metadata element.
	 * @param qualifier
	 *            (May be null) The metadata qualifier.
	 * @param language
	 *            (May be null) The metadata's language
	 * @param allowMultiple
	 *            (Required) determine if multipe metadata element with the same
	 *            element, qualifier and language are allowed.
	 * @return A new metadata
	 */
    public Metadata addMetadata(String element, String qualifier,
            String language, boolean allowMultiple) throws WingException
    {
        Metadata metadata = new Metadata(context, element, qualifier, language, allowMultiple);
        metadatum.add(metadata);
        return metadata;
    }

    /**
	 * Add metadata about this page.
	 * 
	 * @param element
	 *            (Required) The metadata element.
	 * @param qualifier
	 *            (May be null) The metadata qualifier.
	 * @param language
	 *            (May be null) The metadata's language
	 * @return A new metadata
	 */
    public Metadata addMetadata(String element, String qualifier, String language)
            throws WingException
    {
        return addMetadata(element, qualifier, language, false);
    }
    
    /**
     * Add metadata about this page.
     * 
     * @param element
     *            (Required) The metadata element.
     * @param qualifier
     *            (May be null) The metadata qualifier.
     * @return A new metadata
     */
    public Metadata addMetadata(String element, String qualifier)
            throws WingException
    {
        return addMetadata(element, qualifier, null, false);
    }

    /**
     * Add metadata about this page.
     * 
     * @param element
     *            (Required) The metadata element.
     * @return A new metadata
     */
    public Metadata addMetadata(String element) throws WingException
    {
        return addMetadata(element, null, null, false);
    }

    /**
     * Add a new trail to the page.
     * 
     * @param target
     *            (May be null) Target URL for this trail item.
     * @param rend
     *            (May be null) special rendering instructions
     * @return a new trail
     */
    public Trail addTrail(String target, String rend)
            throws WingException
    {
        Trail trail = new Trail(context, target, rend);
        trails.add(trail);
        return trail;
    }

    /**
     * Add a new trail to the page without a link or render attribute.
     * 
     * @return a new trail
     */
    public Trail addTrail()
            throws WingException
    {
        return addTrail(null,null);
    }
    
    /**
     * Add a new trail link to the page.
     * 
     * @param target
     *            (May be null) The Target URL for this trail item.
     * @param characters
     *            (May be null) The textual contents of this trail item.
     */
    public void addTrailLink(String target, String characters)
            throws WingException
    {
        Trail trail = addTrail(target, null);
        trail.addContent(characters);
    }

    /**
     * Add a new trail link to the page.
     * 
     * @param target
     *            (May be null) The Target URL for this trail item.
     * @param message
     *            (Required) The textual contents of this trail item to be
     *            translated
     */
    public void addTrailLink(String target, Message message)
            throws WingException
    {
        Trail trail = addTrail(target, null);
        trail.addContent(message);
    }

    /**
     * Determine if the given SAX event is a PageMeta element.
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
    public boolean mergeEqual(String namespace, String localName, String qName,
            Attributes attributes) throws SAXException, WingException
    {

        if (!WingConstants.DRI.URI.equals(namespace))
            return false;

        if (!E_PAGE_META.equals(localName))
            return false;
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
    public WingMergeableElement mergeChild(String namespace, String localName,
            String qName, Attributes attributes) throws SAXException,
            WingException
    {
    	// We don't merge our children but we do have one special optimization, 
    	// if a metadata or trail is allready in the document then we do not add 
    	// our own trail or metadata for that particular item.
    	if (WingConstants.DRI.URI.equals(namespace) && Trail.E_TRAIL.equals(localName))
    	{
            for (Trail trail : trails)
                trail.dispose();
    		trails.clear();
    	}
    	
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
    public Attributes merge(Attributes attributes) throws SAXException,
            WingException
    {
        this.merged = true;
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
    public void toSAX(ContentHandler contentHandler, LexicalHandler lexicalHandler, 
            NamespaceSupport namespaces) throws SAXException
    {
        if (!merged)
        {
            startElement(contentHandler, namespaces, E_PAGE_META, null);
        }

        for (Metadata metadata : metadatum)
            metadata.toSAX(contentHandler, lexicalHandler, namespaces);
        
        for (Trail trail : trails)
            trail.toSAX(contentHandler, lexicalHandler, namespaces);

        if (!merged)
            endElement(contentHandler, namespaces, E_PAGE_META);
    }

    /**
     * dispose
     */
    public void dispose()
    {
    	for (Metadata metadata : metadatum)
    		metadata.dispose();
    	
    	for (Trail trail : trails)
    		trail.dispose();
    	
    	trails.clear();
    	trails = null;
    	metadatum.clear();
    	metadatum = null;
    	
        super.dispose();
    }
}
