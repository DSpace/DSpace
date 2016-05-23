/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
     * Simple variable to indicate whether a new namespace context is needed. If
     * several namespaces are declared on the same attribute then they are
     * considered in the same 'context'. Each time an element is opened this
     * flag is reset to true, and each time a new namespace is declared it is
     * set to false. Using this information new contexts are opened
     * conservatively.
     */
    private boolean needNewNamespaceContext = true;

    /**
     * The namespace support object keeps track of registered URI prefixes. This
     * is used by the WingElements so that they may attach the correct prefix
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
     * @throws org.dspace.app.xmlui.wing.WingException if setup is impossible.
     */
    public void setupWing() throws WingException
    {
        this.wingContext = new WingContext();
        this.wingContext.setLogger(this.getLogger());
        this.wingContext.setComponentName(this.getComponentName());
        this.wingContext.setObjectManager(this.getObjectManager());

        feederDocument = this.createWingDocument(wingContext);
        this.stack = new Stack<>();
    }

    /**
     * Receive notification of the beginning of a document.
     * @throws org.xml.sax.SAXException if document is malformed.
     */
    @Override
    public void startDocument() throws SAXException
    {
        needNewNamespaceContext = true;
        namespaces = new NamespaceSupport();

        super.startDocument();
    }

    /**
     * Receive notification of the end of a document.
     * @throws org.xml.sax.SAXException if document is malformed.
     */
    @Override
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
     * @throws org.xml.sax.SAXException if document is malformed.
     */
    @Override
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
     * @throws org.xml.sax.SAXException if document is malformed.
     */
    @Override
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
     * @throws org.xml.sax.SAXException if document is malformed.
     */
    @Override
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
            if (stack.isEmpty())
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
                            "Attempting to merge DRI documents but the source document is not compatible with the feeder document.");
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
                {
                    attributes = child.merge(attributes);
                }
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
     * @throws org.xml.sax.SAXException if document is malformed.
     */
    @Override
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
     * @throws org.xml.sax.SAXException unconditionally.
     */

    protected void handleException(Exception e) throws SAXException
    {
        throw new SAXException(
                "An error was encountered while processing the Wing based component: "
                        + this.getClass().getName(), e);
    }

    /**
     * Construct a new WingDocument.
     * 
     * @param wingContext
     *            The current wing context this transformer is operating under.
     * @return an empty document.
     * @throws org.dspace.app.xmlui.wing.WingException if it can't.
     */
    protected WingDocument createWingDocument(WingContext wingContext)
            throws WingException
    {
        return new WingDocument(wingContext);
    }

    /** Abstract implementations of WingTransformer.
     * @param body to be added.
     * @throws java.lang.Exception if something went wrong.
     */
    @Override
    public void addBody(Body body) throws Exception
    {
        // Do nothing
    }

    /**
     * Abstract implementation of WingTransformer.
     * @param options to be added.
     * @throws Exception if something went wrong.
     */
    @Override
    public void addOptions(Options options) throws Exception
    {
        // do nothing
    }

    /**
     * Abstract implementation of WingTransformer.
     * @param userMeta to be added.
     * @throws Exception if something went wrong.
     */
    @Override
    public void addUserMeta(UserMeta userMeta) throws Exception
    {
        // Do nothing
    }

    /**
     * Abstract implementation of WingTransformer.
     * @param pageMeta to be added.
     * @throws Exception if something went wrong.
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws Exception
    {
        // Do nothing
    }

    /** 
     * Return the ObjectManager associated with this component. If no 
     * objectManager needed then return null.
     * @return the ObjectManager, or null.
     */
    public ObjectManager getObjectManager()
    {
        return null;
    }

    /**
     * Return the name of this component. Typically the name is just 
     * the class name of the component.
     * @return the name.
     */
    @Override
    public String getComponentName()
    {
        return this.getClass().getName();
    }

    /**
     * Return the default i18n message catalogue that should be used 
     * when no others are specified.
     * @return the default catalog.
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
     * version allows the callee to specify a particular catalogue overriding
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
     * Recycle.
     */
    @Override
    public void recycle() 
    {
        this.namespaces = null;
        this.feederDocument = null;
        this.wingContext=null;
        this.stack =null;
    	super.recycle();
    }

    /**
     * Dispose.
     */
    public void dispose() {
        this.namespaces = null;
        this.feederDocument = null;
        this.wingContext=null;
        this.stack =null;
    	//super.dispose(); super doesn't dispose.
    }
    
}
