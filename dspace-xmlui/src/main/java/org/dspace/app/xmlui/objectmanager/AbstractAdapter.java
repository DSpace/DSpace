/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.objectmanager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.util.Util;
import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Namespace;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
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
 * <p>This class provides the chassis for those unique parts of the document to be
 * built upon. There are seven rendering methods that may be overridden for each
 * section of the METS document:
 *
 * <ul>
 * <li>Header</li>
 * <li>Descriptive Section</li>
 * <li>Administrative Section</li>
 * <li>File Section</li>
 * <li>Structure Map</li>
 * <li>Structural Link</li>
 * <li>Behavioral Section</li>
 * </ul>
 *
 * @author Scott Phillips
 */

public abstract class AbstractAdapter
{
    /** Namespace declaration for METS and XLINK */
    public static final String METS_URI = "http://www.loc.gov/METS/";
    public static final Namespace METS = new Namespace(METS_URI);
    public static final String XLINK_URI = "http://www.w3.org/TR/xlink/";
    public static final Namespace XLINK = new Namespace(XLINK_URI);
    public static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";
    public static final Namespace XSI = new Namespace(XSI_URI);
    public static final String DIM_URI = "http://www.dspace.org/xmlns/dspace/dim";
    public static final Namespace DIM = new Namespace(DIM_URI);    
    
    /**
     * A sequence used to generate unique mets ids.
     */
    private int idSequence = 0;
    
    /**
     * The contextPath of this web application, used for generating URLs.
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
     * Construct a new adapter, implementers must call this method so
     * the appropriate internal values are ensured to be set correctly.
     * 
     * @param contextPath
     *            The contextPath of this web application.
     */
    public AbstractAdapter(String contextPath)
    {
        this.contextPath = contextPath;
    }

    /** The variables that dictate what part of the METS document to render */
    List<String> sections = new ArrayList<>();
    List<String> dmdTypes = new ArrayList<>();
    Map<String,List> amdTypes = new HashMap<>();
    List<String> fileGrpTypes = new ArrayList<>();
    List<String> structTypes = new ArrayList<>();
    
    /**
     * A comma-separated list of METS sections to render. If no value 
     * is provided then all METS sections are rendered.
     * 
     * @param sections Comma separated list of METS sections.
     */
    public final void setSections(String sections)
    {
    	if (sections == null)
        {
            return;
        }

    	for (String section : sections.split(","))
    	{
    		this.sections.add(section);
    	}
    }
    
    /**
     * A comma-separated list of METS descriptive metadata formats to 
     * render. If no value is provided then only the DIM format is used.
     * 
     * @param dmdTypes Comma separated list of METS metadata types.
     */
    public final void setDmdTypes(String dmdTypes)
    {
    	if (dmdTypes == null)
        {
            return;
        }

    	for (String dmdType : dmdTypes.split(","))
    	{
    		this.dmdTypes.add(dmdType);
    	}
    }
    
    /**
     * Store information about what will be rendered in the METS administrative
     * metadata section.  HashMap format: keys = amdSec, value = List of mdTypes
     *
     * @param amdSec Section of {@code <amdSec>} where this administrative metadata
     *                will be rendered.
     * @param mdTypes Comma-separated list of METS metadata types.
     */
    public final void setAmdTypes(String amdSec, String mdTypes)
    {
    	if (mdTypes == null)
        {
            return;
        }

        List<String> mdTypeList = new ArrayList<>();
    	for (String mdType : mdTypes.split(","))
    	{
    		mdTypeList.add(mdType);
    	}
        
        this.amdTypes.put(amdSec, mdTypeList);
    }

    /**
     * A comma-separated list of METS technical metadata formats to
     * render.
     *
     * @param techMDTypes Comma-separated list of METS metadata types.
     */
    public final void setTechMDTypes(String techMDTypes)
    {
    	setAmdTypes("techMD", techMDTypes);
    }

    /**
     * A comma-separated list of METS intellectual property rights metadata
     * formats to render.
     *
     * @param rightsMDTypes Comma-separated list of METS metadata types.
     */
    public final void setRightsMDTypes(String rightsMDTypes)
    {
    	setAmdTypes("rightsMD", rightsMDTypes);
    }

    /**
     * A comma-separated list of METS source metadata
     * formats to render.
     *
     * @param sourceMDTypes Comma-separated list of METS metadata types.
     */
    public final void setSourceMDTypes(String sourceMDTypes)
    {
    	setAmdTypes("sourceMD", sourceMDTypes);
    }

    /**
     * A comma-separated list of METS digital provenance metadata
     * formats to render.
     *
     * @param digiprovMDTypes Comma-separated list of METS metadata types.
     */
    public final void setDigiProvMDTypes(String digiprovMDTypes)
    {
    	setAmdTypes("digiprovMD", digiprovMDTypes);
    }
    
    /**
     * A comma-separated list of METS fileGrps to render. If no value
     * is provided then all groups are rendered.
     * 
     * @param fileGrpTypes Comma-separated list of METS file groups.
     */
    public final void setFileGrpTypes(String fileGrpTypes)
    {
    	if (fileGrpTypes == null)
        {
            return;
        }

    	for (String fileGrpType : fileGrpTypes.split(","))
    	{
    		this.fileGrpTypes.add(fileGrpType);
    	}
    }
    
    /**
     * A comma-separated list of METS structural types to render. If no 
     * value is provided then only the DIM format is used.
     * 
     * @param structTypes Comma-separated list of METS structure types.
     */
    public final void setStructTypes(String structTypes)
    {
    	if (structTypes == null)
        {
            return;
        }

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
     * @return the URL for this item in the interface.
     * @throws org.dspace.app.xmlui.wing.WingException on error.
     */
    protected abstract String getMETSOBJID() throws WingException;

    /**
     * @return the URL for editing this item
     */
    protected abstract String getMETSOBJEDIT();

    /**
     * @return the METS ID of the mets document.
     * @throws org.dspace.app.xmlui.wing.WingException on error.
     */
    protected abstract String getMETSID() throws WingException;

    /**
     * @return The Profile this METS document conforms to.
     * @throws org.dspace.app.xmlui.wing.WingException on error.
     */
    protected abstract String getMETSProfile() throws WingException;

    /**
     * @return The label of this METS document.
     * @throws org.dspace.app.xmlui.wing.WingException on error.
     */
    protected abstract String getMETSLabel() throws WingException;

    
	/**
	 * Render the complete METS document.
     * @param context session context.
     * @param contentHandler XML content handler.
     * @param lexicalHandler XML lexical handler.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.dspace.content.crosswalk.CrosswalkException passed through.
     * @throws java.io.IOException passed through.
     * @throws java.sql.SQLException passed through.
	 */
    public final void renderMETS(Context context, ContentHandler contentHandler, LexicalHandler lexicalHandler)
            throws WingException, SAXException, CrosswalkException, IOException, SQLException
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
            {
                attributes.put("OBJID", objid);
            }

            // Include the link for editing the item
            objid = getMETSOBJEDIT();
            if (objid != null)
            {
                attributes.put("OBJEDIT", objid);
            }

    		startElement(METS,"METS",attributes);

    		// If the user requested no specific sections then render them all.
    		boolean all = (sections.isEmpty());
    		
    		if (all || sections.contains("metsHdr"))
            {
                renderHeader();
            }
    		if (all || sections.contains("dmdSec"))
            {
                renderDescriptiveSection();
            }
    		if (all || sections.contains("amdSec"))
            {
                renderAdministrativeSection();
            }
    		if (all || sections.contains("fileSec"))
            {
                renderFileSection(context);
            }
    		if (all || sections.contains("structMap"))
            {
                renderStructureMap();
            }
    		if (all || sections.contains("structLink"))
            {
                renderStructuralLink();
            }
    		if (all || sections.contains("behaviorSec"))
            {
                renderBehavioralSection();
            }
    		
    		// FIXME: this is not a met's section, it should be removed
    		if (all || sections.contains("extraSec"))
            {
                renderExtraSections();
            }
    		
    		endElement(METS,"METS");
    		contentHandler.endPrefixMapping("mets");
    		contentHandler.endPrefixMapping("xlink");
    		contentHandler.endPrefixMapping("dim");
    		namespaces.popContext();

    }
	
    /*
     * Each of the METS sections
     */
	protected void renderHeader() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderDescriptiveSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderAdministrativeSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderFileSection(Context context) throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderStructureMap() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderStructuralLink() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderBehavioralSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException  {}
	protected void renderExtraSections() throws WingException, SAXException, CrosswalkException, SQLException, IOException {}
    


    /**
     * Generate a METS file element for a given bitstream.
     *
     * @param context
     *            Session context.
     * @param item
     *            If the bitstream is associated with an item provide the item
     *            otherwise leave null.
     * @param bitstream
     *            The bitstream to build a file element for.
     * @param fileID
     *            The unique file id for this file.
     * @param groupID
     *            The group id for this file, if it is derived from another file
     *            then they should share the same groupID.
     * @throws org.xml.sax.SAXException passed through.
     * @throws java.sql.SQLException passed through.
     */
	protected final void renderFile(Context context, Item item, Bitstream bitstream, String fileID, String groupID)
            throws SAXException, SQLException
    {
       renderFile(context, item, bitstream, fileID, groupID, null);
    }

	/**
     * Generate a METS file element for a given bitstream.
     *
     * @param context
     *            session context.
     * @param item
     *            If the bitstream is associated with an item, provide the item,
     *            otherwise leave null.
     * @param bitstream
     *            The bitstream to build a file element for.
     * @param fileID
     *            The unique file id for this file.
     * @param groupID
     *            The group id for this file, if it is derived from another file
     *            then they should share the same groupID.
     * @param admID
     *            The IDs of the administrative metadata sections which pertain
     *            to this file
     * @throws org.xml.sax.SAXException passed through.
     * @throws java.sql.SQLException passed through.
     */
	protected final void renderFile(Context context, Item item,
            Bitstream bitstream, String fileID, String groupID, String admID)
            throws SAXException, SQLException
    {
		AttributeMap attributes;
		
		// //////////////////////////////
    	// Determine the file attributes
        BitstreamFormat format = bitstream.getFormat(context);
        String mimeType = null;
        if (format != null)
        {
            mimeType = format.getMIMEType();
        }
        String checksumType = bitstream.getChecksumAlgorithm();
        String checksum = bitstream.getChecksum();
        long size = bitstream.getSize();
    	
        // ////////////////////////////////
        // Start the actual file
        attributes = new AttributeMap();
        attributes.put("ID", fileID);
        attributes.put("GROUPID",groupID);
        if (admID != null && admID.length()>0)
        {
            attributes.put("ADMID", admID);
        }
        if (mimeType != null && mimeType.length()>0)
        {
            attributes.put("MIMETYPE", mimeType);
        }
        if (checksumType != null && checksum != null)
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

        
        // If possible, reference this bitstream via a handle, however this may
        // be null if a handle has not yet been assigned. In this case reference the
        // item its internal id. In the last case where the bitstream is not associated
        // with an item (such as a community logo) then reference the bitstreamID directly.
        String identifier = null;
        if (item != null && item.getHandle() != null)
        {
            identifier = "handle/" + item.getHandle();
        }
        else if (item != null)
        {
            identifier = "item/" + item.getID();
        }
        else
        {
            identifier = "id/" + bitstream.getID();
        }
        
        
        String url = contextPath + "/bitstream/"+identifier+"/";
        
        // If we can, append the pretty name of the bitstream to the URL
        try
        {
        	if (bitstream.getName() != null)
            {
                url += Util.encodeBitstreamName(bitstream.getName(), "UTF-8");
            }
        }
        catch (UnsupportedEncodingException uee)
        {
            // just ignore it, we don't have to have a pretty
            // name at the end of the URL because the sequence id will 
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
        {
            attributesXLINK.put("label", description);
        }
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
     * Generate a unique METS id. For consistency, all prefixes should probably
     * end in an underscore, "_".
     * 
     * @param prefix
     *            Prefix to prepend to the id for readability.
     * 
     * @return A unique METS id.
     */
    protected final String getGenericID(String prefix)
    {
        return prefix + (idSequence++);
    }
    
    /**
     * Return a dissemination crosswalk for the given name.
     * 
     * @param crosswalkName name of crosswalk plugin to be looked up.
     * @return The crosswalk.
     * @throws org.dspace.app.xmlui.wing.WingException if crosswalk not found.
     */
    public final DisseminationCrosswalk getDisseminationCrosswalk(String crosswalkName) throws WingException 
    {
    	// FIXME add some caching here
    	DisseminationCrosswalk crosswalk
                = (DisseminationCrosswalk) CoreServiceFactory.getInstance()
                        .getPluginService()
                        .getNamedPlugin(DisseminationCrosswalk.class, crosswalkName);

	    if (crosswalk == null)
        {
            throw new WingException("Unable to find named DisseminationCrosswalk: " + crosswalkName);
        }
	    
	    return crosswalk;
    }
    
    /** 
     * The METS defined types of Metadata, if a format is not listed here 
     * then it should use the string "OTHER" and provide additional 
     * attributes describing the metadata type 
     */
    public static final String[] METS_DEFINED_TYPES = 
    	{"MARC","MODS","EAD","DC","NISOIMG","LC-AV","VRA","TEIHDR","DDI","FGDC","PREMIS"/*,"OTHER"*/};
    
    /**
     * Determine if the provided metadata type is a standard METS
     * defined type. If it is not, use the other string.
     * 
     * @param metadataType type name
     * @return True if METS defined
     */
    public final boolean isDefinedMETStype(String metadataType)
    {
       for (String definedType : METS_DEFINED_TYPES)
       {
           if (definedType.equals(metadataType))
           {
               return true;
           }
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
     * @param namespace
     *            (Required) The namespace of this element.
     * @param name
     *            (Required) The local name of this element.
     * @param attributes
     *            (May be null) Attributes for this element
     * @throws org.xml.sax.SAXException passed through.
     */
    protected final void startElement(Namespace namespace, String name,
            AttributeMap... attributes) throws SAXException
    {
        contentHandler.startElement(namespace.URI, name, qName(namespace, name),
                map2sax(namespace,attributes));
    }

    /**
     * Send the SAX event for these plain characters, not wrapped in any
     * elements.
     * 
     * @param characters
     *            (May be null) Characters to send.
     * @throws org.xml.sax.SAXException passed through.
     */
    protected final void sendCharacters(String characters) throws SAXException
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
     * @param namespace
     *            (Required) The namespace of this element.
     * @param name
     *            (Required) The local name of this element.
     * @throws org.xml.sax.SAXException passed through.
     */
    protected final void endElement(Namespace namespace, String name)
            throws SAXException
    {
        contentHandler.endElement(namespace.URI, name, qName(namespace, name));
    }

    /**
     * Build the SAX attributes object based upon Java's String map. This
     * convenience method will build, or add to an existing attributes object,
     * the attributes detailed in the AttributeMap.
     * 
     * @param elementNamespace
     *            SAX Helper class to keep track of namespaces able to determine
     *            the correct prefix for a given namespace URI.
     * @param attributes
     *            An existing SAX AttributesImpl object to add attributes to.
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
            boolean differentNamespaces = false;
            Namespace attributeNamespace = attributeMap.getNamespace();
            if (attributeNamespace != null && !(attributeNamespace.URI.equals(elementNamespace.URI)))
            {
                differentNamespaces = true;
            }

            // copy each one over.
            for (Map.Entry<String, String> attr : attributeMap.entrySet())
            {
                if (attr.getValue() == null)
                {
                    continue;
                }

                if (differentNamespaces)
                {
                    attributes.addAttribute(attributeNamespace.URI, attr.getKey(),
                            qName(attributeNamespace, attr.getKey()), "CDATA", attr.getValue());

                }
                else
                {
                    attributes.addAttribute("", attr.getKey(), attr.getKey(), "CDATA", attr.getValue());
                }
            }
        }
        return attributes;
    }
    
    /**
     * Create the qName for the element with the given localName and namespace
     * prefix.
     * 
     * @param namespace
     *            (May be null) The namespace prefix.
     * @param localName
     *            (Required) The element's local name.
     * @return
     */
    private String qName(Namespace namespace, String localName)
    {
    	String prefix = namespaces.getPrefix(namespace.URI);
        if (prefix == null || prefix.equals(""))
        {
            return localName;
        }
        else
        {
            return prefix + ":" + localName;
        }
    }
    
}
