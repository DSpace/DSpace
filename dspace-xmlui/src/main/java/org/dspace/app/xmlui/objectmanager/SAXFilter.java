/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.objectmanager;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;


/** 
 * This is a Swiss army like SAX Filter, its purpose is to filter out 
 * undesirable SAX events from the stream. The primary application of 
 * this is for inserting SAX fragment into an existing SAX pipeline, 
 * under this scenario you would not want new startDocument or 
 * endDocument events interfering with the existing pipeline thus 
 * this class can filter those out.
 * 
 * The Swiss army part comes in because it's configurable. Instead of 
 * defining a static set of events that are filled out by default all
 * events are filled out and must be turned on to allow each type
 * individually.
 * 
 * Primarily you can filter events based upon their type, i.e. start/end 
 * elements or start/end documents. However there is one special control,
 * and that is to only allow elements below a minimum level.
 * . 
 * 
 * @author Scott Phillips
 */
public class SAXFilter implements ContentHandler, LexicalHandler
{
	
	/** Control for which type of SAX events to allow */
	private boolean allowDocuments = false;
	private boolean allowDocumentLocators = false;
	private boolean allowProcessingInstructions = false;
	private boolean allowPrefixMappings = false;
	private boolean allowElements = false;
	private boolean allowIgnorableWhitespace = false;
	private boolean allowSkippedEntities = false;
	private boolean allowCharacters = false;

	private boolean allowDTDs = false;
	private boolean allowEntities = false;
	private boolean allowCDATA = false;
	private boolean allowComments = false;
	
	
	/** The minimum level an element must be before it will be allowed */
	private int minimumElementLevel = -1;
	
	/** 
	 * The current XML level, each time start element is encountered this 
	 * is increased, and each time an end element is encountered it is 
	 * decreased. 
	 */
	private int currentElementLevel = 0;
	
	
	/**
	 * If no URI is provided then substitute this default prefix and URI:
	 */
	private String defaultURI;
	
	/** The SAX handlers and namespace support */
	private ContentHandler contentHandler;
	private LexicalHandler lexicalHandler;
	private NamespaceSupport namespaces;

	/**
	 * Construct a new SAXFilter such that the allowed events will be routed
	 * to the corresponding content and lexical handlers.
	 * 
	 * @param contentHandler The SAX content handler.
	 * @param lexicalHandler The SAX lexical handler.
	 * @param namespaces Namespace support which records what prefixes have been defined.
	 */
	public SAXFilter(ContentHandler contentHandler, LexicalHandler lexicalHandler, NamespaceSupport namespaces)
	{
		this.contentHandler = contentHandler;
		this.lexicalHandler = lexicalHandler;
		this.namespaces = namespaces;
	}

	/** Allow start/end document events */
	public SAXFilter allowDocuments() {
		this.allowDocuments = true;
		return this;
	}
	
	/** Allow document locator events */
	public SAXFilter allowDocumentLocators() {
		this.allowDocumentLocators = true;
		return this;
	}
	
	/** Allow processing instruction events */
	public SAXFilter allowProcessingInstructions() {
		this.allowProcessingInstructions = true;
		return this;
	}
	
	/** allow start/end prefix mapping events */
	public SAXFilter allowPrefixMappings() {
		this.allowPrefixMappings = true;
		return this;
	}
	
	/** allow start/end element events */
	public SAXFilter allowElements() {
		this.allowElements = true;
		return this;
	}
	
	/**
	 * Allow start/end element events.
	 * 
	 * However only allow those start / end events if
	 * they are below the given XML level. I.e. each nested 
	 * element is a new level.
	 * 
	 * @param minimumElementLevel 
	 * 				the minimum level required.
	 * @return this SAXFilter instance.
	 */
	public SAXFilter allowElements(int minimumElementLevel)
	{
		this.allowElements = true;
		this.minimumElementLevel = minimumElementLevel;
		return this;
	}
	
	/** Allow ignorable whitespace events */
	public SAXFilter allowIgnorableWhitespace() {
		this.allowIgnorableWhitespace = true;
		return this;
	}
	
	/** Allow start / end events for skipped entities */
	public SAXFilter allowSkippedEntities() {
		this.allowSkippedEntities = true;
		return this;
	}
	
	/** Allow character events */
	public SAXFilter allowCharacters() {
		this.allowCharacters = true;
		return this;
	}
	
	/** Allow DTD events */
	public SAXFilter allowDTDs() {
		this.allowDTDs = true;
		return this;
	}
	
	/** Allow XML entities events */
	public SAXFilter allowEntities() {
		this.allowEntities = true;
		return this;
	}
	
	/** Allow CDATA events */
	public SAXFilter allowCDATA() {
		this.allowCDATA = true;
		return this;
	}
	
	/** Allow comment events */
	public SAXFilter allowComments() {
		this.allowComments = true;
		return this;
	}
	
	/**
	 * Add a default namespace is none is provided. The namespace 
	 * should have already been declared (add added to the 
	 * namespace support object
	 * 
	 * @param uri the default namespace uri.
	 */
	public SAXFilter setDefaultNamespace(String uri)
	{
		this.defaultURI = uri;
		return this;
	}

	/**
	 * SAX Content events
	 */
	
	public void startDocument() throws SAXException
	{
		if (allowDocuments)
        {
            contentHandler.startDocument();
        }
	}

	public void endDocument() throws SAXException
	{	
		if (allowDocuments)
        {
            contentHandler.endDocument();
        }
	}
	
	public void setDocumentLocator(Locator locator)
	{
		if (allowDocumentLocators)
        {
            contentHandler.setDocumentLocator(locator);
        }
	}
	
	public void processingInstruction(String target, String data)
	throws SAXException
	{
		if (allowProcessingInstructions)
        {
            contentHandler.processingInstruction(target, data);
        }
	}
	
	public void startPrefixMapping(String prefix, String uri)
	throws SAXException
	{
		if (allowPrefixMappings)
        {
            contentHandler.startPrefixMapping(prefix, uri);
        }
	}
	
	public void endPrefixMapping(String prefix) throws SAXException
	{
		if (allowPrefixMappings)
        {
            contentHandler.endPrefixMapping(prefix);
        }
	}
	
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
	{
		if (allowElements)
		{	
			currentElementLevel++;
			// check if we are past the minimum level requirement
			if (minimumElementLevel < currentElementLevel)
			{
				if (defaultURI != null && (uri == null || "".equals(uri)))
				{
					// No namespace provided, use the default namespace.
					String prefix = namespaces.getPrefix(defaultURI);
					
					if (!(prefix == null || "".equals(prefix)))
					{
						qName = prefix+":"+localName;
					}
					
					contentHandler.startElement(defaultURI, localName, qName, atts);
				}
				else
				{
					// let the event pass through unmodified.
					contentHandler.startElement(uri, localName, localName, atts);
				}
			}
		}
	}

	public void endElement(String uri, String localName, String qName)
	throws SAXException
	{
		if (allowElements)
		{	
			// check if we are past the minimum level requirements
			if (minimumElementLevel < currentElementLevel)
			{
				if (defaultURI != null && (uri == null || "".equals(uri)))
				{
					// No namespace provided, use the default namespace.
					String prefix = namespaces.getPrefix(defaultURI);
					
					if (!(prefix == null || "".equals(prefix)))
					{
						qName = prefix+":"+localName;
					}
					
					contentHandler.endElement(defaultURI, localName, qName);
				}
				else
				{
					// Let the event pass through unmodified.
					contentHandler.endElement(uri, localName, localName);
				}
			}
			currentElementLevel--;
		}
	}

	public void ignorableWhitespace(char[] ch, int start, int length)
	throws SAXException
	{
		if (allowIgnorableWhitespace)
        {
            contentHandler.ignorableWhitespace(ch, start, length);
        }
	}

	public void skippedEntity(String name) throws SAXException
	{
		if (allowSkippedEntities)
        {
            contentHandler.skippedEntity(name);
        }
	}

	public void characters(char[] ch, int start, int length)
	throws SAXException
	{
		if (allowCharacters)
        {
            contentHandler.characters(ch, start, length);
        }
	}
	
	/**
	 * SAX Lexical events
	 */

	public void startDTD(String name, String publicId, String systemId)
	throws SAXException
	{
		if (allowDTDs)
        {
            lexicalHandler.startDTD(name, publicId, systemId);
        }
	}

	public void endDTD() throws SAXException
	{
		if (allowDTDs)
        {
            lexicalHandler.endDTD();
        }
	}

	public void startEntity(String name)
	throws SAXException
	{
		if (allowEntities)
        {
            lexicalHandler.startEntity(name);
        }
	}

	public void endEntity(String name)
	throws SAXException
	{
		if (allowEntities)
        {
            lexicalHandler.endEntity(name);
        }
	}

	public void startCDATA()
	throws SAXException
	{
		if (allowCDATA)
        {
            lexicalHandler.startCDATA();
        }
	}

	public void endCDATA()
	throws SAXException
	{
		if (allowCDATA)
        {
            lexicalHandler.endCDATA();
        }
	}

	public void comment(char[] ch, int start, int length)
	throws SAXException
	{
		if (allowComments)
        {
            lexicalHandler.comment(ch, start, length);
        }
	}
}



