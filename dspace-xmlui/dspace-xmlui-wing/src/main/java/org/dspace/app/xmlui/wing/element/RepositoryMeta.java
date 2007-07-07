/*
 * RepositoryMeta.java
 *
 * Version: $Revision: 1.6 $
 *
 * Date: $Date: 2006/03/20 22:39:42 $
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

import java.util.HashMap;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.ObjectManager;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A class representing a set of all referenced repositories in the DRI document.
 * 
 * @author Scott Phillips
 */
public class RepositoryMeta extends AbstractWingElement implements WingMergeableElement, MetadataElement
{
    /** The name of the ObjectMeta element */
    public static final String E_REPOSITORY_META = "repositoryMeta";

    /** The name of the repository element */
    public static final String E_REPOSITORY = "repository";

    /** The name of this repository identifier attribute*/
    public static final String A_REPOSITORY_ID = "repositoryID";
    
    /** The unique url of this repository */
    public static final String A_REPOSITORY_URL = "url";
    
    /** Has this repositoryMeta element been merged? */
    private boolean merged = false;
    
    /** The registered repositories on this page */
    private HashMap<String,String> repositories = new HashMap<String,String>();

    /**
     * Construct a new RepositoryMeta
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     */
    protected RepositoryMeta(WingContext context) throws WingException
    {
        super(context);
        
        ObjectManager objectManager = context.getObjectManager();

        if (!(objectManager == null))
        {
        	this.repositories = objectManager.getAllManagedRepositories();
        }
    }

    /**
     * Determine if the given SAX event is a ObjectMeta element.
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

        if (!E_REPOSITORY_META.equals(localName))
            return false;
        return true;
    }

    /**
     * Since we will only add to the object set and never modify an existing
     * object we do not merge any child elements.
     * 
     * However we will notify the object manager of each identifier we
     * encounter.
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
        // Check if it's in our name space and an options element.
        if (!WingConstants.DRI.URI.equals(namespace))
            return null;
        
        if (!E_REPOSITORY.equals(localName))
            return null;
        
        // Get the repositoryIdentefier
        String repositoryIdentifier = attributes.getValue(A_REPOSITORY_ID);

        if (repositories.containsKey(repositoryIdentifier))
        {
        	repositories.remove(repositoryIdentifier);
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
    public void toSAX(ContentHandler contentHandler,
            LexicalHandler lexicalHandler, NamespaceSupport namespaces)
            throws SAXException
    {
        if (!merged)
            startElement(contentHandler, namespaces, E_REPOSITORY_META, null); 
    
    	for (String identifier : repositories.keySet())
    	{
    		// add the repository XML
    		AttributeMap attributes = new AttributeMap();
    		attributes.put(A_REPOSITORY_ID, identifier);
    		attributes.put(A_REPOSITORY_URL, repositories.get(identifier));
    		
    		startElement(contentHandler,namespaces,E_REPOSITORY,attributes);
    		endElement(contentHandler,namespaces,E_REPOSITORY);
    	}
       

        if (!merged)
            endElement(contentHandler, namespaces, E_REPOSITORY_META);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        // Nothing to clean up.
        super.dispose();
    }
}
