/*
 * Include.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/04/25 15:29:42 $
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

package org.dspace.app.xmlui.wing;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.components.source.SourceUtil;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.transformation.AbstractTransformer;
import org.apache.cocoon.xml.dom.DOMStreamer;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Meta;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.UserMeta;
import org.dspace.app.xmlui.wing.element.WingDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * The Include class reads an DRI XML file from and merges it into the existing
 * document stream using the Wing framework.
 * 
 * If the file is not present at the source provided a blank document is merged, 
 * allowing the pipeline to continue excecution.  This is also logged as a warning.
 * 
 * @author Scott Phillips
 */

public class Include extends AbstractTransformer implements CacheableProcessingComponent
{

    /**
     * A data structure describing what elements are to be merged and upon what
     * key. The first map's key is the name of a mergeable element's name while
     * the value is a list of all attributes that the element must match on to
     * be considered the same element.
     */
    private final static Map<String, String[]> mergeableMap;

    /** Construct the mergeableMap from constant data */
    static
    {
        Map<String, String[]> buildMap = new HashMap<String, String[]>();

        buildMap.put(WingDocument.E_DOCUMENT, null);

        buildMap.put(Meta.E_META, null);
        buildMap.put(UserMeta.E_USER_META, null);
        buildMap.put(PageMeta.E_PAGE_META, null);
        buildMap.put("artifactmeta", null);
        buildMap.put("repositoryMeta", null);
        buildMap.put("community",
                new String[] { "repositoryIdentifier" });
        buildMap.put("collection",
                new String[] { "repositoryIdentifier" });

        buildMap.put(Body.E_BODY, null);

        buildMap.put(Options.E_OPTIONS, null);
        buildMap.put(org.dspace.app.xmlui.wing.element.List.E_LIST,
                new String[] { org.dspace.app.xmlui.wing.element.List.A_NAME });

        mergeableMap = buildMap;
    }

    /** The source document */
    private Document w3cDocument;

    /** Helper class to stream the w3c DOM into SAX events */
    private DOMStreamer streamer;

    /** Stack of our current location within the document */
    private Stack<Element> stack;
    
    /** The Cocoon source for the included XML document */
    private Source source;
    
    /** The src attribute to the cocoon source */
    private String src;

    /**
     * Read in the given src path into an internal DOM for later processing when
     * needed.
     * 
     * @param sourceResolver
     *            Resolver for cocoon pipelines.
     * @param objectModel
     *            The pipelines's object model.
     * @param src
     *            The source parameter
     * @param parameters
     *            The transformer's parameters.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        this.src = src;
        this.source = resolver.resolveURI(src);
    }

    /**
     * Generate the unique key.
     * This key must be unique inside the space of this component.
     *
     * @return The generated key hashes the src
     */
    public Serializable getKey()
    {
        return this.src;
    }

    /**
     * Generate the validity object.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    public SourceValidity getValidity()
    {
        if (source != null)
        {
        	if (source.exists())
        		// The file exists so return it's validity.
        		return source.getValidity();
        	else
        		// The file does not exist so we will just return always valid. This
        		// will have an nastly side effect that if a file is removed from a
        		// running system the cache will remain valid. However if the other
        		// option is to always invalidate the cache if the file is not present
        		// which is not desirable either.
        		return NOPValidity.SHARED_INSTANCE;
        }
        else
            return null;
    }
    
    
    /**
     * Receive notification of the beginning of a document.
     */
    public void startDocument() throws SAXException
    {
        try
        {
            w3cDocument = SourceUtil.toDOM(source);
        }
        catch (Exception e)
        {   
        	// since we were unable to parce an XML document from the source given we will
        	// simply log the error as a warning and create  a null stack
        	getLogger().warn("File to be included from " + source.toString() +" not found.");
        	
        	stack = null;
            super.startDocument();
            
            return;
        }
        	
        stack = new Stack<Element>();
        streamer = new DOMStreamer(contentHandler, lexicalHandler);
        super.startDocument();
    }

    /**
     * Receive notification of the end of a document.
     */
    public void endDocument() throws SAXException
    {
        stack = null;
        super.endDocument();
    }

    /**
     * Receive notification of the beginning of an element.
     * 
     * @param uri
     *            The Namespace URI, or the empty string if the element has no
     *            Namespace URI or if Namespace processing is not being
     *            performed.
     * @param localName
     *            The local name (without prefix), or the empty string if
     *            Namespace processing is not being performed.
     * @param qName
     *            The raw XML 1.0 name (with prefix), or the empty string if raw
     *            names are not available.
     * @param attributes
     *            The attributes attached to the element. If there are no
     *            attributes, it shall be an empty Attributes object.
     */
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException
    {
        //getLogger().debug("startElement: " + localName);
        if(stack == null){
        	// do nothing fall thru to the call to super.startElement
        	// this means that the document to be read was not parsable
        	// or not found in startDocument()
        }
        else if (stack.size() == 0)
            stack.push(w3cDocument.getDocumentElement());
        else
        {
            Element peek = stack.peek();

            Element foundChild = null;
            for (Element child : getElementList(peek))
                if (isEqual(child, uri, localName, qName, attributes))
                    foundChild = child;

            if (foundChild != null)
                peek.removeChild(foundChild);

            stack.push(foundChild);
        }

        super.startElement(uri, localName, qName, attributes);
    }

    /**
     * Receive notification of the end of an element.
     * 
     * @param uri
     *            The Namespace URI, or the empty string if the element has no
     *            Namespace URI or if Namespace processing is not being
     *            performed.
     * @param localName
     *            The local name (without prefix), or the empty string if
     *            Namespace processing is not being performed.
     * @param qName
     *            The raw XML 1.0 name (with prefix), or the empty string if raw
     *            names are not available.
     */
    public void endElement(String uri, String localName, String qName)
            throws SAXException
    {
        //getLogger().debug("endElement: " + localName);
        
        
        // if the stack is null do nothing fall thru to the call to 
        // super.endElement
    	// this means that the document to be read was not parsable
    	// or not found in startDocument()
        if(stack!=null){
	        Element poped = stack.pop();
	
	        if (poped != null)
	        {
	            //getLogger().debug("startElement: streaming");
	
	            for (Node node : getNodeList(poped))
	                streamer.stream(node);
	        }
        }

        super.endElement(uri, localName, qName);
    }

    /**
     * Receive notification of character data.
     * 
     * @param c
     *            The characters from the XML document.
     * @param start
     *            The start position in the array.
     * @param len
     *            The number of characters to read from the array.
     */
    public void characters(char c[], int start, int len) throws SAXException
    {
        super.characters(c, start, len);
    }

    /**
     * Determine if the given SAX event is the same as the given w3c DOM
     * element. If so then return true, otherwise false.
     * 
     * @param child
     *            W3C DOM element to compare with the SAX event.
     * @param uri
     *            The namespace URI of the SAX event.
     * @param localName
     *            The localName of the SAX event.
     * @param qName
     *            The qualified name of the SAX event.
     * @param attributes
     *            The attributes of the SAX event.
     * @return if equal.
     */
    private boolean isEqual(Element child, String uri, String localName,
            String qName, Attributes attributes)
    {
        if (child == null)
            return false;

        if (uri != null && !uri.equals(child.getNamespaceURI()))
            return false;

        if (localName != null && !localName.equals(child.getLocalName()))
            return false;

        if (!mergeableMap.containsKey(localName))
            return false;

        String[] attributeIdentities = mergeableMap.get(localName);

        if (attributeIdentities != null)
        {
            for (String attributeIdentity : attributeIdentities)
            {
                String testIdentity = attributes.getValue(attributeIdentity);
                String childIdentity = child.getAttribute(attributeIdentity);

                if (childIdentity != null && childIdentity.equals(testIdentity))
                    continue;

                if (childIdentity == null && testIdentity == null)
                    continue;

                return false;
            }
        }

        return true;
    }

    /**
     * DOM Helper method - Get a list of all child elements.
     * 
     * @param element
     *            The parent element
     * @return a list of all child elements.
     */
    private static List<Element> getElementList(Element element)
    {
        if (element == null)
            return new ArrayList<Element>();

        NodeList nodeList = element.getChildNodes();

        List<Element> resultList = new ArrayList<Element>();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE)
                resultList.add((Element) nodeList.item(i));
        }

        return resultList;
    }

    /**
     * DOM Helper method - Get a list of all child nodes.
     * 
     * @param element
     *            The parent element
     * @return a list of all child nodes.
     */
    private static List<Node> getNodeList(Element element)
    {
        if (element == null)
            return new ArrayList<Node>();

        NodeList nodeList = element.getChildNodes();

        List<Node> resultList = new ArrayList<Node>();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            resultList.add((Node) nodeList.item(i));
        }

        return resultList;
    }
    
    /**
     * Recycle
     */
    public void recycle()
    {
        this.w3cDocument = null;
        this.streamer = null;
        this.stack = null;
        this.source = null;
        
        super.recycle();
    }
}