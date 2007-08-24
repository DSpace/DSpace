/*
 * AbstractAdapter.java
 *
 * Version: $Revision: 1.11 $
 *
 * Date: $Date: 2006/06/07 22:13:39 $
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

package org.dspace.app.xmlui.objectmanager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.util.Util;
import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Namespace;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.PluginManager;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;


/**
 * This is the abstract adapter containing all the common elements between
 * the three types of adapters: item, container, and repository. Each adapter
 * translate a given type of DSpace object into a METS document for rendering
 * into the DRI document.
 * 
 * This class provides the chasses for those unique parts of the document to be
 * build upon, there are seven rendering methods that may be overriden for each
 * section of the METS document.
 * 
 * Header
 * Descriptive Section
 * Administrative Section
 * File Section
 * Structure Map
 * Structural Link
 * Behavioral Section
 * 
 * @author Scott Phillips
 */

public abstract class AbstractAdapter
{
    /** Namespace declaration for METS & XLINK */
    public static final String METS_URI = "http://www.loc.gov/METS/";
    public static final Namespace METS = new Namespace(METS_URI);
    public static final String XLINK_URI = "http://www.w3.org/TR/xlink/";
    public static final Namespace XLINK = new Namespace(XLINK_URI);
    public static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";
    public static final Namespace XSI = new Namespace(XSI_URI);
    public static final String DIM_URI = "http://www.dspace.org/xmlns/dspace/dim";
    public static final Namespace DIM = new Namespace(DIM_URI);    
    
    /**
     * A sequence used to generate unquie mets ids.
     */
    private int idSequence = 0;
    
    /**
     * The contextPath of this webapplication, used for generateing urls.
     */
    protected String contextPath;
    
    /**
     * The SAX handlers for content and lexical events. Also the support 
     * element for namespaces which knows the prefixes for each declared 
     * namespace.
     */
    protected ContentHandler contentHandler;
    protected LexicalHandler lexicalHandler;
    protected NamespaceSupport namespaces;
    
    /**
     * Construct a new adapter, implementers must use call this method so
     * the approprate internal values are insured to be set correctly.
     * 
     * @param contextPath
     *            The contextPath of this web application.
     */
    public AbstractAdapter(String contextPath)
    {
        this.contextPath = contextPath;
    }

    /** The variables that dicatacte what part of the METS document to render */
    List<String> sections = new ArrayList<String>();
    List<String> dmdTypes = new ArrayList<String>();
    List<String> amdTypes = new ArrayList<String>();
    List<String> fileGrpTypes = new ArrayList<String>();
    List<String> structTypes = new ArrayList<String>();
    
    /**
     * A comma seperated list of METS sections to render. If no value 
     * is provided then all METS sections are rendered.
     * 
     * @param sections Comma seperated list of METS sections.
     */
    public void setSections(String sections)
    {
    	if (sections == null)
    		return;

    	for (String section : sections.split(","))
    	{
    		this.sections.add(section);
    	}
    }
    
    /**
     * A comma seperated list of METS descriptive metadata formats to 
     * render. If no value is provided then only the DIM format is used.
     * 
     * @param sections Comma seperated list of METS metadata types.
     */
    public void setDmdTypes(String dmdTypes)
    {
    	if (dmdTypes == null)
    		return;

    	for (String dmdType : dmdTypes.split(","))
    	{
    		this.dmdTypes.add(dmdType);
    	}
    }
    
    /**
     * A comma seperated list of METS administrative metadata formats to 
     * render. 
     * 
     * @param sections Comma seperated list of METS metadata types.
     */
    public void setAmdTypes(String amdTypes)
    {
    	if (amdTypes == null)
    		return;

    	for (String amdType : amdTypes.split(","))
    	{
    		this.amdTypes.add(amdType);
    	}
    }
    
    /**
     * A comma seperated list of METS fileGrps to render. If no value
     * is provided then all groups are rendered.
     * 
     * @param sections Comma seperated list of METS file groups.
     */
    public void setFileGrpTypes(String fileGrpTypes)
    {
    	if (fileGrpTypes == null)
    		return;

    	for (String fileGrpType : fileGrpTypes.split(","))
    	{
    		this.fileGrpTypes.add(fileGrpType);
    	}
    }
    
    /**
     * A comma seperated list of METS structural types to render. If no 
     * value is provided then only the DIM format is used.
     * 
     * @param sections Comma seperated list of METS structure types.
     */
    public void setStructTypes(String structTypes)
    {
    	if (structTypes == null)
    		return;

    	for (String structType : structTypes.split(","))
    	{
    		this.structTypes.add(structType);
    	}
    }
	
    
    /**
     * 
     * 
     * 
     * 
     * 
     * METS methods
     * 
     * 
     * 
     * 
     * 
     * 
     */
    
    
    /**
     * @return the URL for this item in the interface
     */
    protected abstract String getMETSOBJID() throws WingException;

    /**
     * @return Return the URL for editing this item
     */
    protected abstract String getMETSOBJEDIT();

    /**
     * @return the METS ID of the mets document.
     */
    protected abstract String getMETSID() throws WingException;

    /**
     * @return The Profile this METS document conforms too.
     */
    protected abstract String getMETSProfile() throws WingException;

    /**
     * @return The label of this METS document.
     */
    protected abstract String getMETSLabel() throws WingException;

    
	/**
	 * Render the complete METS document.
	 */
    public void renderMETS(ContentHandler contentHandler, LexicalHandler lexicalHandler) throws WingException, SAXException, CrosswalkException, IOException, SQLException 
    {
    		this.contentHandler = contentHandler;
    		this.lexicalHandler = lexicalHandler;
    		this.namespaces = new NamespaceSupport();
    	
    	
    		// Declare our namespaces
    		namespaces.pushContext();
    		namespaces.declarePrefix("mets", METS.URI);
    		namespaces.declarePrefix("xlink", XLINK.URI);
    		namespaces.declarePrefix("xsi", XSI.URI);
    		namespaces.declarePrefix("dim", DIM.URI);
    		contentHandler.startPrefixMapping("mets", METS.URI);
    		contentHandler.startPrefixMapping("xlink", XLINK.URI);
    		contentHandler.startPrefixMapping("xsi", XSI.URI);
    		contentHandler.startPrefixMapping("dim", DIM.URI);
    		
    		// Send the METS element
    		AttributeMap attributes = new AttributeMap();
    		attributes.put("ID", getMETSID());
    		attributes.put("PROFILE", getMETSProfile());
    		attributes.put("LABEL", getMETSLabel());
    		String objid = getMETSOBJID();
    		if (objid != null)
    			attributes.put("OBJID", objid);

            // Include the link for editing the item
            objid = getMETSOBJEDIT();
            if (objid != null)
                attributes.put("OBJEDIT", objid);

    		startElement(METS,"METS",attributes);

    		// If the user requested no specefic sections then render them all.
    		boolean all = (sections.size() == 0);
    		
    		if (all || sections.contains("metsHdr"))
    			renderHeader();
    		if (all || sections.contains("dmdSec"))
    			renderDescriptiveSection();
    		if (all || sections.contains("amdSec"))
    			renderAdministrativeSection();
    		if (all || sections.contains("fileSec"))
    			renderFileSection();
    		if (all || sections.contains("structMap"))
    			renderStructureMap();
    		if (all || sections.contains("structLink"))
    			renderStructuralLink();
    		if (all || sections.contains("behaviorSec"))
    			renderBehavioralSection();
    		
    		// FIXME: this is not a met's section, it should be removed
    		if (all || sections.contains("extraSec"))
    			renderExtraSections();
    		
    		endElement(METS,"METS");
    		contentHandler.endPrefixMapping("mets");
    		contentHandler.endPrefixMapping("xlink");
    		contentHandler.endPrefixMapping("dim");
    		namespaces.popContext();

    }
	
    /**
     * Each of the METS sections
     */
	protected void renderHeader() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderDescriptiveSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderAdministrativeSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderFileSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderStructureMap() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderStructuralLink() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderBehavioralSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderExtraSections() throws WingException, SAXException, CrosswalkException, SQLException, IOException {}
    
    
	
	/**
     * Generate a METS file element for a given bitstream.
     * 
     * @param item
     *            If the bitstream is associated with an item provid the item
     *            otherwise leave null.
     * @param bitstream
     *            The bitstream to build a file element for.
     * @param fileID
     *            The unique file id for this file.
     * @param groupID
     *            The group id for this file, if it is derived from another file
     *            then they should share the same groupID.
     * @return The METS file element.
     */
	protected void renderFile(Item item, Bitstream bitstream, String fileID, String groupID) throws SAXException 
	{
		AttributeMap attributes;
		
		// //////////////////////////////
    	// Determine the file attributes
        BitstreamFormat format = bitstream.getFormat();
        String mimeType = null;
        if (format != null)
            mimeType = format.getMIMEType();
        String checksumType = bitstream.getChecksumAlgorithm();
        String checksum = bitstream.getChecksum();
        long size = bitstream.getSize();
    	
        // ////////////////////////////////
        // Start the actual file
        attributes = new AttributeMap();
        attributes.put("ID", fileID);
        attributes.put("GROUP_ID",groupID);
        if (mimeType != null)
        	attributes.put("MIMETYPE", mimeType);
        if (checksumType != null && checksumType != null)
        {
        	attributes.put("CHECKSUM", checksum);
        	attributes.put("CHECKSUMTYPE", checksumType);
        }
        attributes.put("SIZE", String.valueOf(size));
        startElement(METS,"file",attributes);
        
        
        // ////////////////////////////////////
        // Determine the file location attributes
        String name = bitstream.getName();
        String description = bitstream.getDescription();

        
        // If possible refrence this bitstream via a handle, however this may
        // be null if a handle has not yet been assigned. In this case refrence the
        // item its internal id. In the last case where the bitstream is not associated
        // with an item (such as a community logo) then refrence the bitstreamID directly.
        String identifier = null;
        if (item != null && item.getHandle() != null)
        	identifier = "handle/"+item.getHandle();
        else if (item != null)
        	identifier = "item/"+item.getID();
        else
        	identifier = "id/"+bitstream.getID();
        
        
        String url = contextPath + "/bitstream/"+identifier+"/";
        
        // If we can put the pretty name of the bitstream on the end of the URL
        try
        {
        	if (bitstream.getName() != null)
        		url += Util.encodeBitstreamName(bitstream.getName(), "UTF-8");
        }
        catch (UnsupportedEncodingException uee)
        {
            // just ignore it, we don't have to have a pretty
            // name on the end of the url because the sequence id will 
        	// locate it. However it means that links in this file might
        	// not work....
        }
        
        url += "?sequence="+bitstream.getSequenceID();
        
        
        // //////////////////////
        // Start the file location
        attributes = new AttributeMap();
        AttributeMap attributesXLINK = new AttributeMap();
        attributesXLINK.setNamespace(XLINK);
        attributes.put("LOCTYPE", "URL");
        attributesXLINK.put("type","locator");
        attributesXLINK.put("title", name);
        if (description != null)
        	attributesXLINK.put("label",description);
        attributesXLINK.put("href", url);
        startElement(METS,"FLocat",attributes,attributesXLINK);
        

        // ///////////////////////
        // End file location
        endElement(METS,"FLocate");
        
        // ////////////////////////////////
        // End the file
        endElement(METS,"file");
	}
	
	
	/**
     * 
     * Generate a unique METS id. For consistancy, all prefixs should probably
     * end in an underscore, "_".
     * 
     * @param prefix
     *            Prefix to prepend to the id for readability.
     * 
     * @return A unique METS id.
     */
    protected String getGenericID(String prefix)
    {
        return prefix + (idSequence++);
    }
    
    /**
     * Return a dissemination crosswalk for the given name.
     * 
     * @param crosswalkName
     * @return The crosswalk or throw an exception if not found.
     */
    public DisseminationCrosswalk getDisseminationCrosswalk(String crosswalkName) throws WingException 
    {
    	// Fixme add some caching here
    	DisseminationCrosswalk crosswalk = (DisseminationCrosswalk) PluginManager.getNamedPlugin(DisseminationCrosswalk.class, crosswalkName);

	    if (crosswalk == null)
	        throw new WingException("Unable to find named DisseminationCrosswalk: " + crosswalkName);
	    
	    return crosswalk;
    }
    
    /** 
     * The METS defined types of Metadata, if a format is not listed here 
     * then it should use the string "OTHER" and provide additional 
     * attributes describing the metadata type 
     */
    public static String[] METS_DEFINED_TYPES = 
    	{"MARC","MODS","EAD","DC","NISOIMG","LC-AV","VRA","TEIHDR","DDI","FGDC"/*,"OTHER"*/};
    
    /**
     * Determine if the provided metadata type is a stardard METS
     * defined type. If it is not, use the other string.
     * 
     * @param metadataType type name
     * @return True if METS defined
     */
    public boolean isDefinedMETStype(String metadataType)
    {
       for (String definedType : METS_DEFINED_TYPES)
       {
           if (definedType.equals(metadataType))
               return true;
       }
       return false;
    }
    
    
    
    
    
    /**
	 * 
	 * 
	 * SAX Helper methods
	 * 
	 * 
	 *
	 */
	
	/**
     * Send the SAX events to start this element.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @param namespace
     *            (Required) The namespace of this element.
     * @param name
     *            (Required) The local name of this element.
     * @param attributes
     *            (May be null) Attributes for this element
     */
    protected void startElement(Namespace namespace, String name, AttributeMap ... attributes) throws SAXException
    {
        contentHandler.startElement(namespace.URI, name, qName(namespace, name),
                map2sax(namespace,attributes));
    }

    /**
     * Send the SAX event for these plain characters, not wrapped in any
     * elements.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param characters
     *            (May be null) Characters to send.
     */
    protected void sendCharacters(String characters) throws SAXException
    {
        if (characters != null)
        {
            char[] contentArray = characters.toCharArray();
            contentHandler.characters(contentArray, 0, contentArray.length);
        }
    }
    
    /**
     * Send the SAX events to end this element.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @param namespace
     *            (Required) The namespace of this element.
     * @param name
     *            (Required) The local name of this element.
     */
    protected void endElement(Namespace namespace, String name)
            throws SAXException
    {
        contentHandler.endElement(namespace.URI, name, qName(namespace, name));
    }

    /**
     * Build the SAX attributes object based upon Java's String map. This
     * convenience method will build, or add to an existing attributes object,
     * the attributes detailed in the AttributeMap.
     * 
     * @param namespaces
     *            SAX Helper class to keep track of namespaces able to determine
     *            the correct prefix for a given namespace URI.
     * @param attributes
     *            An existing SAX AttributesImpl object to add attributes too.
     *            If the value is null then a new attributes object will be
     *            created to house the attributes.
     * @param attributeMap
     *            A map of attributes and values.
     * @return
     */
    private AttributesImpl map2sax(Namespace elementNamespace, AttributeMap ... attributeMaps)
    {

        AttributesImpl attributes = new AttributesImpl();
        for (AttributeMap attributeMap : attributeMaps)
        {
            boolean diffrentNamespaces = false;
            Namespace attributeNamespace = attributeMap.getNamespace();
            if (attributeNamespace != null)
            {
            	if (!(attributeNamespace.URI.equals(elementNamespace.URI)))
            	{
            		diffrentNamespaces = true;
            	}
            }
            
            // copy each one over.
            for (String name : attributeMap.keySet())
            {
                String value = attributeMap.get(name);
                if (value == null)
                    continue;

                if (diffrentNamespaces)
                	attributes.addAttribute(attributeNamespace.URI, name, 
                			qName(attributeNamespace, name), "CDATA", value);
                else
                    attributes.addAttribute("", name, name, "CDATA", value);
                
            }
        }
        return attributes;
    }
    
    /**
     * Create the qName for the element with the given localName and namespace
     * prefix.
     * 
     * @param prefix
     *            (May be null) The namespace prefix.
     * @param localName
     *            (Required) The element's local name.
     * @return
     */
    private String qName(Namespace namespace, String localName)
    {
    	String prefix = namespaces.getPrefix(namespace.URI);
        if (prefix == null || prefix.equals(""))
            return localName;
        else
            return prefix + ":" + localName;
    }
    
}
