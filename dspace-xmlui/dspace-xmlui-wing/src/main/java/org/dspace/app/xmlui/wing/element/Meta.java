/*
 * Meta.java
 *
 * Version: $Revision: 1.6 $
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
 * The body contains any number of divisions (div elements) which group content
 * into interactive and non interactive display blocks.
 * 
 * @author Scott Phillips
 */
public class Meta extends AbstractWingElement implements WingMergeableElement
{
    /** The name of the meta element */
    public static final String E_META = "meta";

    /** The divisions contained within this body */
    private boolean merged = false;

    /** User oriented metadata associated with this document */
    private UserMeta userMeta;

    /** Page oriented metadata associated with this document */
    private PageMeta pageMeta;
    
    /** Repository oriented metadata associated with this document */
    private RepositoryMeta repositoryMeta;
    
    protected Meta(WingContext context) throws WingException
    {
        // FIXME: don't statically assign authenticated status or
        // repositoryIdentifier.
        super(context);
        userMeta = new UserMeta(context);
        pageMeta = new PageMeta(context);
        repositoryMeta = new RepositoryMeta(context);
    }

    /**
     * Set a new user oriented metadata set.
     * 
     * @return The user oriented metadata set.
     */
    public UserMeta setUserMeta() throws WingException
    {
        return this.userMeta;
    }

    /**
     * Set a new page oriented metadata set.
     * 
     * @return The page oriented metadata set.
     */
    public PageMeta setPageMeta() throws WingException
    {
        return this.pageMeta;
    }
    
    /**
     * Set a new repository oriented metadata set.
     * 
     * @return The repository oriented metadata set.
     */
    public RepositoryMeta setRepositoryMeta() throws WingException
    {
        return this.repositoryMeta;
    }
    
    /**
     * Determine if the given SAX event is a Meta element.
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

        if (!E_META.equals(localName))
            return false;
        
        return true;
    }

    /**
     * Merge the given sub-domain of metadata elements.
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
        // User
        if (this.userMeta != null
                && this.userMeta.mergeEqual(namespace, localName, qName,
                        attributes))
        {
            UserMeta userMeta = this.userMeta;
            this.userMeta = null;
            return userMeta;
        }

        // page
        if (this.pageMeta != null
                && this.pageMeta.mergeEqual(namespace, localName, qName,
                        attributes))
        {
            PageMeta pageMeta = this.pageMeta;
            this.pageMeta = null;
            return pageMeta;
        }
        
        // repository
        if (this.repositoryMeta != null
                && this.repositoryMeta.mergeEqual(namespace, localName, qName,
                        attributes))
        {
            RepositoryMeta repositoryMeta = this.repositoryMeta;
            this.repositoryMeta = null;
            return repositoryMeta;
        }
        
        return null;
    }

    /**
     * Notify this element that it is being merged.
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
     * Translate to SAX events
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
            startElement(contentHandler, namespaces, E_META, null);

        if (this.userMeta != null)
            this.userMeta.toSAX(contentHandler, lexicalHandler, namespaces);
        if (this.pageMeta != null)
            this.pageMeta.toSAX(contentHandler, lexicalHandler, namespaces);
        if (this.repositoryMeta != null)
            this.repositoryMeta.toSAX(contentHandler, lexicalHandler, namespaces);
           
        if (!merged)
            endElement(contentHandler, namespaces, E_META);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        if (this.userMeta != null)
            this.userMeta.dispose();
        if (this.pageMeta != null)
            this.pageMeta.dispose();
        if (this.repositoryMeta != null)
            this.repositoryMeta.dispose();
        
        this.userMeta = null;
        this.pageMeta = null;
        this.repositoryMeta = null;
        
        super.dispose();
    }
}
