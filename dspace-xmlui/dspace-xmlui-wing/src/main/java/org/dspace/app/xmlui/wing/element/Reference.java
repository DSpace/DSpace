/*
 * Reference.java
 *
 * Version: $Revision: 1.2 $
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

import java.util.ArrayList;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.ObjectManager;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Refrence a repository object that is referenced in the ObjectMeta portion of
 * the DRI document. These are internal refrences distinct from the xref element
 * which is used for extrenal refrences.
 * 
 * @author Scott Phillips
 */

public class Reference extends AbstractWingElement implements
        StructuralElement
{
    /** The name of the reference element */
    public static final String E_REFERENCE = "reference";

    /** The name of the repositoryIdentifier attribute */
    public static final String A_REPOSITORY_ID = "repositoryID";

    /** An optional type of the refrenced object. */
    public static final String A_TYPE = "type";
    
    /** The name of the objectIdentifier attribute */
    public static final String A_URL = "url";
    
    /** The unique identifier of the repository this object is identified with */
    private String repository;

    /** The unique identifier of the object within the specified repository */
    private String url;
    
    /** An optional type of the referenced object */
    private String type;

    /** All content of this container */
    private java.util.List<AbstractWingElement> contents = new ArrayList<AbstractWingElement>();

    /**
     * Construct a new object reference.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * 
     * @param object
     *            (Required) The referenced object.
     */
    protected Reference(WingContext context, Object object)
            throws WingException
    {
        super(context);

        ObjectManager objectManager = context.getObjectManager();

        if (objectManager == null)
            throw new WingException(
                    "Unable to reference object because no object manager has been defined.");
        
        if (!objectManager.manageObject(object))
            throw new WingException(
                    "The available object manager is unable to manage the give object.");

        this.url = objectManager.getObjectURL(object);
        this.repository = objectManager.getRepositoryIdentifier(object);
        this.type = objectManager.getObjectType(object);
        
    }

    /**
     * Add a nested reference set.
     * 
     * @param type
     *            (required) The reference type, see referenceSet.TYPES
     * @param orderBy
     *            (May be null) A statement of ordering for reference sets.
     * @param render
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     */
    public ReferenceSet addReferenceSet(String type, String orderBy, String render)
            throws WingException
    {
        ReferenceSet referenceSet = new ReferenceSet(context, true, null, type,
                orderBy, render);
        contents.add(referenceSet);
        return referenceSet;
    }

    /**
     * Add a nested reference set
     * 
     * @param type
     *            (required) The reference type, see referenceSet.TYPES
     * @param orderBy
     *            (May be null) A statement of ordering for reference sets.
     */
    public ReferenceSet addReferenceSet(String type, String orderBy)
            throws WingException
    {
        return addReferenceSet(type, orderBy, null);
    }

    /**
     * Add a nested include set
     * 
     * @param type
     *            (required) The include type, see includeSet.TYPES
     */
    public ReferenceSet addReferenceSet(String type) throws WingException
    {
        return addReferenceSet(type, null, null);
    }

    /**
     * Translate this metadata inclusion set to SAX
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
        AttributeMap attributes = new AttributeMap();
        attributes.put(A_REPOSITORY_ID, this.repository);
        attributes.put(A_URL, this.url);
        if (type != null)
        	attributes.put(A_TYPE, type);
        
        startElement(contentHandler, namespaces, E_REFERENCE, attributes);

        for (AbstractWingElement content : contents)
        {
            content.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        endElement(contentHandler, namespaces, E_REFERENCE);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        for (AbstractWingElement content : contents)
            content.dispose();
        if (contents != null)
            contents.clear();
        contents = null;
        super.dispose();
    }
}
