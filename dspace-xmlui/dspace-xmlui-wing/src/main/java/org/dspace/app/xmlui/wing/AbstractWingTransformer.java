/*
 * AbstractWingTransformer.java
 *
 * Version: $Revision: 1.15 $
 *
 * Date: $Date: 2006/06/02 21:48:02 $
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

import java.util.Stack;

import org.apache.cocoon.transformation.AbstractTransformer;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.UserMeta;
import org.dspace.app.xmlui.wing.element.WingDocument;
import org.dspace.app.xmlui.wing.element.WingMergeableElement;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class handles receiving SAX events and translating them into DRI events.
 * These DRI events are then routed to the individual implementing components
 * where they fill in and construct the DRI document. The document they
 * construct is known as the feeder document, this is merged into the main
 * document that was generated from the previous component in the Cocoon
 * pipeline. The merge takes place in accordance with the DRI schema's rules for
 * merging two DRI documents.
 * 
 * 
 * @author Scott Phillips
 */
public abstract class AbstractWingTransformer extends AbstractTransformer
        implements WingTransformer
{
    /**
     * Simple variable to indicate weather a new namespace context is needed. If
     * several namespaces are declared on the same attribute then they are
     * considered in the same 'context'. Each time an element is opened this
     * flag is reset to true, and each time a new namespace is declared it is
     * set to false. Using this information new contexts are opened
     * conservatively.
     */
    private boolean needNewNamespaceContext = true;

    /**
     * The namespace support object keeps track of registered URI prefixes. This
     * is used by the WingElements so that they may attach the correctp prefix
     * when assigning elements to namespaces.
     */
    private NamespaceSupport namespaces;

    /**
     * The feeder document is the document being merged into the main,
     * pre-existing document, that is the result of the previous Cocoon
     * component in the pipeline.
     */
    private WingDocument feederDocument;

    /**
     * The wing context is where the namespace support is stored along with the
     * content and lexical handlers so that the wing elements can have access to
     * them when they perform their toSAX() method.
     */
    private WingContext wingContext;

    /**
     * This is a stack to the current location in the merge while it is in
     * progress.
     */
    private Stack<WingMergeableElement> stack;
    
    /**
     * Set up the transformer so that it can build a feeder Wing document and
     * merge it into the main document
     * 
     * FIXME: Update document: - this method must be called to initialize the
     * framework. It must be called after the component's setup has been called
     * and the implementing object setup.
     * 
     */
    public void setupWing() throws WingException
    {
        this.wingContext = new WingContext();
        this.wingContext.setLogger(this.getLogger());
        this.wingContext.setComponentName(this.getComponentName());
        this.wingContext.setObjectManager(this.getObjectManager());

        feederDocument = this.createWingDocument(wingContext);
        this.stack = new Stack<WingMergeableElement>();
    }

    /**
     * Receive notification of the beginning of a document.
     */
    public void startDocument() throws SAXException
    {
        needNewNamespaceContext = true;
        namespaces = new NamespaceSupport();

        super.startDocument();
    }

    /**
     * Receive notification of the end of a document.
     */
    public void endDocument() throws SAXException
    {
        wingContext.dispose();
        super.endDocument();
    }

    /**
     * Begin the scope of a prefix-URI Namespace mapping.
     * 
     * @param prefix
     *            The Namespace prefix being declared.
     * @param uri
     *            The Namespace URI the prefix is mapped to.
     */
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException
    {
        if (needNewNamespaceContext)
        {
            namespaces.pushContext();
            needNewNamespaceContext = false;
        }
        namespaces.declarePrefix(prefix, uri);

        super.startPrefixMapping(prefix, uri);
    }

    /**
     * End the scope of a prefix-URI mapping.
     * 
     * @param prefix
     *            The prefix that was being mapping.
     */
    public void endPrefixMapping(String prefix) throws SAXException
    {
        if (!needNewNamespaceContext)
        {
            namespaces.popContext();
            needNewNamespaceContext = true;
        }
        super.endPrefixMapping(prefix);
    }

    /**
     * Receive notification of the beginning of an element.
     * 
     * @param namespaceURI
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
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes attributes) throws SAXException
    {
        // Reset the namespace context flag.
        needNewNamespaceContext = true;

        try
        {
            if (stack == null)
            {
                throw new WingException("Stack not initialized.");
            }
            
            // Deal with the stack jump start issue of having a document all
            // ready on the stack.
            if (stack.size() == 0)
            {
                if (feederDocument.mergeEqual(namespaceURI, localName, qName,
                        attributes))
                {
                    attributes = feederDocument.merge(attributes);
                    stack.push(feederDocument);
                }
                else
                {
                    throw new WingException(
                            "Attempting to merge DRI documents but the source document is not compatable with the feeder document.");
                }

            }
            else if (stack.size() > 0)
            {
                WingMergeableElement peek = stack.peek();
                WingMergeableElement child = null;
                if (peek != null)
                {
                    child = peek.mergeChild(namespaceURI, localName, qName,
                            attributes);
                }

                // Check if we should construct a new portion of the document.
                if (child instanceof UserMeta)
                {
                    // Create the UserMeta
                    this.addUserMeta((UserMeta) child);
                }
                else if (child instanceof PageMeta)
                {
                    // Create the PageMeta
                    this.addPageMeta((PageMeta) child);
                }
                else if (child instanceof Body)
                {
                    // Create the Body
                    this.addBody((Body) child);
                }
                else if (child instanceof Options)
                {
                    // Create the Options
                    this.addOptions((Options) child);
                }

                // Update any attributes of this merged element.
                if (child != null)
                    attributes = child.merge(attributes);
                stack.push(child);
            }
            // Send off the event with nothing modified except for the
            // attributes (possibly)
            super.startElement(namespaceURI, localName, qName, attributes);
        }
        catch (SAXException saxe)
        {
            throw saxe;
        }
        catch (Exception e)
        {
            handleException(e);
        }

    }

    /**
     * Receive notification of the end of an element.
     * 
     * @param namespaceURI
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
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException
    {
        try
        {
            if (stack.size() > 0)
            {
                WingMergeableElement poped = stack.pop();
                if (poped != null)
                {
	                poped.toSAX(contentHandler, lexicalHandler, namespaces);
	                poped.dispose();
                }
            }

            // Send the event on unmodified
            super.endElement(namespaceURI, localName, qName);
        }
        catch (SAXException saxe)
        {
            throw saxe;
        }
        catch (Exception e)
        {
            handleException(e);
        }
    }

    /**
     * Handle exceptions that occurred during the document's creation. When
     * errors occur a SAX event is being processed it will be sent through this
     * method. This allows implementing classes to override this method for
     * specific error handling hooks.
     * 
     * @param e
     *            The thrown exception
     */

    protected void handleException(Exception e) throws SAXException
    {
        throw new SAXException(
                "An error was incountered while processing the Wing based component: "
                        + this.getClass().getName(), e);
    }

    /**
     * Construct a new WingDocument.
     * 
     * @param wingContext
     *            The current wing context this transformer is operating under.
     */
    protected WingDocument createWingDocument(WingContext wingContext)
            throws WingException
    {
        return new WingDocument(wingContext);
    }

    /** Abstract implementations of WingTransformer */

    public void addBody(Body body) throws Exception
    {
        // Do nothing
    }

    public void addOptions(Options options) throws Exception
    {
        // do nothing
    }

    public void addUserMeta(UserMeta userMeta) throws Exception
    {
        // Do nothing
    }

    public void addPageMeta(PageMeta pageMeta) throws Exception
    {
        // Do nothing
    }

    /** 
     * Return the ObjectManager associated with this component. If no 
     * objectManager needed then return null.
     */
    public ObjectManager getObjectManager()
    {
        return null;
    }

    /**
     * Return the name of this component. Typicaly the name is just 
     * the class name of the component.
     */
    public String getComponentName()
    {
        return this.getClass().getName();
    }

    /**
     * Return the default i18n message catalogue that should be used 
     * when no others are specified.
     */
    public static String getDefaultMessageCatalogue()
    {
        return "default";
    }
    
    /**
     * This is a short cut method for creating a new message object, this
     * allows them to be created with one simple method call that uses 
     * the default catalogue.
     * 
     * @param key
     *            The catalogue key used to look up a message.
     * @return A new message object.
     */
    public static Message message(String key)
    {
        return message(getDefaultMessageCatalogue(), key);
    }

    /**
     * This is a short cut method for creating a new message object. This
     * version allows the callie to specify a particular catalogue overriding
     * the default catalogue supplied.
     * 
     * @param catalogue
     *            The catalogue where translations will be located.
     * @param key
     *            The catalogue key used to look up a translation within the
     *            catalogue.
     * @return A new message object.
     */
    public static Message message(String catalogue, String key)
    {
        return new Message(catalogue, key);
    }
    
    /**
     * Recyle
     */
    public void recycle() 
    {
        this.namespaces = null;
        this.feederDocument = null;
        this.wingContext=null;
        this.stack =null;
    	super.recycle();
    }

    /**
     * Dispose
     */
    public void dispose() {
        this.namespaces = null;
        this.feederDocument = null;
        this.wingContext=null;
        this.stack =null;
    	//super.dispose(); super dosn't dispose.
    }
    
}
