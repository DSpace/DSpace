/*
 * ItemAdapter.java
 *
 * Version: $Revision: 1.7 $
 *
 * Date: $Date: 2006/05/02 01:24:11 $
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

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.SAXOutputter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This is an adapter which translate a DSpace item into a METS document
 * following the DSpace METS profile, err well mostly. At least if you use
 * the proper configuration it will be fully complaint with the profile, 
 * however this adapter will allow you to configure it to be incorrect.
 * 
 * When we are configured to be non-complaint with the profile the MET's
 * profile is changed to reflect the diviation. The DSpace profile states 
 * that metadata should be given in MODS format. However you can configure
 * this adapter to use any metadata crosswalk. When that case is detected we 
 * change the profile to say that we are divating from the standard profile
 * and it lists what metadata has been added.
 * 
 * There are four parts to an item's METS document: descriptive metadata, 
 * file section, structural map, and extra sections.
 * 
 * @author Scott Phillips
 */

public class ItemAdapter extends AbstractAdapter
{
    /** The item this METS adapter represents */
    private Item item;

    /** List of bitstreams which should be publicaly viewable */
    private List<Bitstream> contentBitstreams = new ArrayList<Bitstream>();

    /** The primary bitstream, or null if none specified */
    private Bitstream primaryBitstream;

    /** A space seperated list of descriptive metadata sections */
    private StringBuffer dmdSecIDS;
    
    
    /**
     * Construct a new ItemAdapter
     * 
     * @param item
     *            The DSpace item to adapt.
     * @param contextPath
     *            The contextpath for this webapplication.
     */
    public ItemAdapter(Item item,String contextPath)
    {
        super(contextPath);
        this.item = item;
    }

    /** Return the item */
    public Item getItem()
    {
    	return this.item;
    }
    
    
    
    /**
     * 
     * 
     * 
     * Required abstract methods
     * 
     * 
     * 
     */

    /**
     * Return the URL of this item in the interface
     */
    protected String getMETSOBJID()
    {
    	if (item.getHandle() != null)
    		return contextPath+"/handle/" + item.getHandle();
    	return null;
    }

    /**
     * @return Return the URL for editing this item
     */
    protected String getMETSOBJEDIT()
    {
        return contextPath+"/admin/item?itemID=" + item.getID();
    }

    /**
     * Return the item's handle as the METS ID
     */
    protected String getMETSID()
    {
        if (item.getHandle() == null)
        	return "item:"+item.getID();
        else
        	return "hdl:"+item.getHandle();
    }

    /**
     * Return the official METS SIP Profile.
     */
    protected String getMETSProfile() throws WingException
    {
    	return "DSPACE METS SIP Profile 1.0";
    }

    /**
     * Return a helpfull label that this is a DSpace Item.
     */
    protected String getMETSLabel()
    {
        return "DSpace Item";
    }
    
    /**
     * Return a unique id for a bitstream.
     */
    protected String getFileID(Bitstream bitstream)
    {
    	return "file_" + bitstream.getID();
    }

    /**
     * Return a group id for a bitstream.
     */
    protected String getGroupFileID(Bitstream bitstream)
    {
    	return "group_file_" + bitstream.getID();
    }
    
    
    /**
     * Render the METS descriptive section. This will create a new metadata
     * section for each crosswalk configured. Futher more, a special check 
     * has been aded that will add mods descriptive metadata if it is 
     * available in DSpace.
     * 
     * Example:
     * <dmdSec>
     *  <mdWrap MDTYPE="MODS">
     *    <xmlData>
     *      ... content from the crosswalk ...
     *    </xmlDate>
     *  </mdWrap>
     * </dmdSec
     */
    protected void renderDescriptiveSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException 
    {
		AttributeMap attributes;
    	String groupID = getGenericID("group_dmd_");
    	dmdSecIDS = new StringBuffer();

    	// Add DIM descriptive metadata if it was requested or if no metadata types 
    	// were specified. Further more since this is the default type we also use a 
    	// faster rendering method that the crosswalk API.
    	if(dmdTypes.size() == 0 || dmdTypes.contains("DIM"))
    	{
    		// Metadata element's ID
    		String dmdID = getGenericID("dmd_");
    		// Keep track of all descriptive sections
            dmdSecIDS.append("" + dmdID);
    		
			////////////////////////////////
			// Start a metadata wrapper
			attributes = new AttributeMap();
			attributes.put("ID", dmdID);
			attributes.put("GROUPID", groupID);
			startElement(METS, "dmdSec", attributes);
	
			 ////////////////////////////////
			// Start a metadata wrapper
			attributes = new AttributeMap();
			attributes.put("MDTYPE","OTHER");
			attributes.put("OTHERMDTYPE", "DIM");
			startElement(METS,"mdWrap",attributes);
			
			// ////////////////////////////////
			// Start the xml data
			startElement(METS,"xmlData");
	
			
			// ///////////////////////////////
			// Start the DIM element			
			attributes = new AttributeMap();
			attributes.put("dspaceType", Constants.typeText[item.getType()]);
            if (item.isWithdrawn())
                attributes.put("withdrawn", "y");
            startElement(DIM,"dim",attributes);
			
	        DCValue[] dcvs = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
	        for (DCValue dcv : dcvs)
	        {
	        	// ///////////////////////////////
	        	// Field element for each metadata field.
	        	attributes = new AttributeMap();
	    		attributes.put("mdschema",dcv.schema);
	    		attributes.put("element", dcv.element);
	    		if (dcv.qualifier != null)
	    			attributes.put("qualifier", dcv.qualifier);
	    		if (dcv.language != null)
	    			attributes.put("language", dcv.language);
	    		
	    		startElement(DIM,"field",attributes);
	    		sendCharacters(dcv.value);
	    		endElement(DIM,"field");
	        }
			
	        // ///////////////////////////////
			// End the DIM element
			endElement(DIM,"dim");
			
	        // ////////////////////////////////
	        // End elements
	        endElement(METS,"xmlData");
	        endElement(METS,"mdWrap");
	        endElement(METS, "dmdSec");

    	}
		
    	
    	// Add any extra crosswalks that may configured.
    	for (String dmdType : dmdTypes)
    	{
    		// If DIM was requested then it was generated above without using
    		// the crosswalk API. So we can skip this one.
    		if ("DIM".equals(dmdType))
    			continue;
    		
    		DisseminationCrosswalk crosswalk = getDisseminationCrosswalk(dmdType);
    		
    		if (crosswalk == null)
    			continue;
    		
    		String dmdID = getGenericID("dmd_");
    		 // Add our id to the list.
    		dmdSecIDS.append(" " + dmdID);
    		
    		////////////////////////////////
    		// Start a metadata wrapper
    		attributes = new AttributeMap();
    		attributes.put("ID", dmdID);
    		attributes.put("GROUPID", groupID);
    		startElement(METS, "dmdSec", attributes);

    		 ////////////////////////////////
    		// Start a metadata wrapper
    		attributes = new AttributeMap();
    		if (isDefinedMETStype(dmdType))
    		{
    			attributes.put("MDTYPE", dmdType);
    		}
    		else
    		{
    			attributes.put("MDTYPE","OTHER");
    			attributes.put("OTHERMDTYPE", dmdType);
    		}
    		startElement(METS,"mdWrap",attributes);
    		
    		// ////////////////////////////////
    		// Start the xml data
    		startElement(METS,"xmlData");

    		
    		// ///////////////////////////////
    		// Send the actual XML content
    		try {
	    		Element dissemination = crosswalk.disseminateElement(item);
	
	    		SAXFilter filter = new SAXFilter(contentHandler, lexicalHandler, namespaces);
	    		// Allow the basics for XML
	    		filter.allowElements().allowIgnorableWhitespace().allowCharacters().allowCDATA().allowPrefixMappings();
	    		
	            SAXOutputter outputter = new SAXOutputter();
	            outputter.setContentHandler(filter);
	            outputter.setLexicalHandler(filter);
				outputter.output(dissemination);
			} 
            catch (JDOMException jdome) 
			{
				throw new WingException(jdome);
			}
			catch (AuthorizeException ae)
			{
				// just ignore the authorize exception and continue on with
				//out parsing the xml document.
			}
    		
            
            // ////////////////////////////////
            // End elements
            endElement(METS,"xmlData");
            endElement(METS,"mdWrap");
            endElement(METS, "dmdSec");
    	}


    	// Check to see if there is an in-line MODS document 
    	// stored as a bitstream. If there is then we should also
    	// include these metadata in our METS document. However
    	// we don't really know what the document describes, so we
    	// but it in it's own dmd group.

    	Boolean include = ConfigurationManager.getBooleanProperty("xmlui.bitstream.mods");
    	if (include && dmdTypes.contains("MODS"))
    	{
	    	// Generate a second group id for any extra metadata added.
	    	String groupID2 = getGenericID("group_dmd_");
	    	
	    	Bundle[] bundles = item.getBundles("METADATA");
	    	for (Bundle bundle : bundles)
	    	{
	    		Bitstream bitstream = bundle.getBitstreamByName("MODS.xml");
	
	    		if (bitstream == null)
	    			continue;
	    		
	    		
	    		String dmdID = getGenericID("dmd_");
	    		
	    		
	    		////////////////////////////////
	    		// Start a metadata wrapper
	    		attributes = new AttributeMap();
	    		attributes.put("ID", dmdID);
	    		attributes.put("GROUPID", groupID2);
	    		startElement(METS, "dmdSec", attributes);
	
	    		 ////////////////////////////////
	    		// Start a metadata wrapper
	    		attributes = new AttributeMap();
	    		attributes.put("MDTYPE", "MODS");
	    		startElement(METS,"mdWrap",attributes);
	    		
	    		// ////////////////////////////////
	    		// Start the xml data
	    		startElement(METS,"xmlData");
	    		
	    		
	    		// ///////////////////////////////
	    		// Send the actual XML content
	    		
	    		SAXFilter filter = new SAXFilter(contentHandler, lexicalHandler, namespaces);
	    		// Allow the basics for XML
	    		filter.allowElements().allowIgnorableWhitespace().allowCharacters().allowCDATA().allowPrefixMappings();
	    		
	    		XMLReader reader = XMLReaderFactory.createXMLReader();
	    		reader.setContentHandler(filter);
	    		reader.setProperty("http://xml.org/sax/properties/lexical-handler", filter);
	    		try {
		    		InputStream is = bitstream.retrieve();
		    		reader.parse(new InputSource(is));
	    		} 
	    		catch (AuthorizeException ae)
	    		{
	    			// just ignore the authorize exception and continue on with
	    			//out parsing the xml document.
	    		}
	    		
	    		// ////////////////////////////////
	            // End elements
	            endElement(METS,"xmlData");
	            endElement(METS,"mdWrap");
	            endElement(METS, "dmdSec");
	    	}
    	}
    
    }
    
    /**
     * Render the METS file section. This will contain a list of all bitstreams in the
     * item. Each bundle, even those that are not typically displayed will be listed.
     * 
     * Example:
     * <fileSec>
     *   <fileGrp USE="CONTENT">
     *     <file ... >
     *       <fLocate ... >
     *     </file>
     *   </fileGrp>
     *   <fileGrp USE="TEXT">
     *     <file ... >
     *       <fLocate ... >
     *     </file>
     *   </fileGrp>
     * </fileSec>
     */
    protected void renderFileSection() throws SQLException, SAXException
    {
    	AttributeMap attributes;
    	
        // //////////////////////
        // Start a new file section
    	startElement(METS,"fileSec");
    	
    
    	// Check if the user is requested a specific bundle or
    	// the all bundles.
    	List<Bundle> bundles;
    	if (fileGrpTypes.size() == 0)
    		bundles = Arrays.asList(item.getBundles());
    	else
    	{
    		bundles = new ArrayList<Bundle>();
    		for (String fileGrpType : fileGrpTypes)
    		{
    			for (Bundle newBundle : item.getBundles(fileGrpType))
    			{
    				bundles.add(newBundle);
    			}
    		}
    	}
    	
    	// Loop over all requested bundles
        for (Bundle bundle : bundles)
        {

            // Use the bitstream's name as the use parameter unless we
            // are the original bundle. In this case rename it to
            // content.
            String use = bundle.getName();
            boolean isContentBundle = false; // remember the content bundle.
            boolean isDerivedBundle = false;
            if ("ORIGINAL".equals(use))
            {
                use = "CONTENT";
                isContentBundle = true;
            }
            if ("TEXT".equals(bundle.getName()) || "THUMBNAIL".equals(bundle.getName()))
            {
                isDerivedBundle = true;
            }
            
            if (fileGrpTypes.size() != 0 && !fileGrpTypes.contains(use))
            	// The user requested specific file groups and this is not one of them.
            	continue;
            

            // ///////////////////
            // Start bundle's file group
            attributes = new AttributeMap();
            attributes.put("USE", use);
            startElement(METS,"fileGrp",attributes);
            
            for (Bitstream bitstream : bundle.getBitstreams())
            {
            	// //////////////////////////////
            	// Determine the file's IDs
            	String fileID = getFileID(bitstream);
                
            	Bitstream originalBitstream = null;
                if (isDerivedBundle)
                	originalBitstream = findOriginalBitstream(item, bitstream);
                String groupID = getGroupFileID((originalBitstream == null) ? bitstream : originalBitstream );

                // Render the actual file & flocate elements.
                renderFile(item, bitstream, fileID, groupID);

                // Remember all the viewable content bitstreams for later in the
                // structMap.
                if (isContentBundle)
                {
                    contentBitstreams.add(bitstream);
                    if (bundle.getPrimaryBitstreamID() == bitstream.getID())
                        primaryBitstream = bitstream;
                }
            }
            
            // ///////////////////
            // End the bundle's file group
            endElement(METS,"fileGrp");
        }
        
        // //////////////////////
        // End the file section
    	endElement(METS,"fileSec");
    }

    
    /**
     * Render the item's structural map. This includes a list of
     * content bitstreams, those are bistreams that are typicaly
     * viewable by the end user.
     * 
     * Examlpe:
     * <structMap TYPE="LOGICAL" LABEL="DSpace">
     *   <div TYPE="DSpace Item" DMDID="space seperated list of ids">
     *     <fptr FILEID="primary bitstream"/>
     *     ... a div for each content bitstream.
     *   </div>
     * </structMap>
     */
    protected void renderStructureMap() throws SQLException, SAXException
    {
    	AttributeMap attributes;
    	
    	// ///////////////////////
    	// Start a new structure map
    	attributes = new AttributeMap();
    	attributes.put("TYPE", "LOGICAL");
    	attributes.put("LABEL", "DSpace");
    	startElement(METS,"structMap",attributes);

    	// ////////////////////////////////
    	// Start the special first division
    	attributes = new AttributeMap();
    	attributes.put("TYPE", "DSpace Item");
    	// add references to the Descriptive metadata
    	if (dmdSecIDS != null)
    		attributes.put("DMDID", dmdSecIDS.toString());
    	startElement(METS,"div",attributes);
    	
    	// add a fptr pointer to the primary bitstream.
    	if (primaryBitstream != null)
    	{
    		// ////////////////////////////////
    		// Start & end a refrence to the primary bistream.
    		attributes = new AttributeMap();
    		String fileID = getFileID(primaryBitstream);
    		attributes.put("FILEID", fileID);
    		
    		startElement(METS,"fptr",attributes);
    		endElement(METS,"fptr");
    	}

    	for (Bitstream bitstream : contentBitstreams)
    	{
    		// ////////////////////////////////////
    		// Start a div for each publicaly viewable bitstream
    		attributes = new AttributeMap();
    		attributes.put("ID", getGenericID("div_"));
    		attributes.put("TYPE", "DSpace Content Bitstream");
    		startElement(METS,"div",attributes);

    		// ////////////////////////////////
    		// Start a the actualy pointer to the bitstream
    		attributes = new AttributeMap();
    		String fileID = getFileID(bitstream);
    		attributes.put("FILEID", fileID);
    		
    		startElement(METS,"fptr",attributes);
    		endElement(METS,"fptr");
    		
    		// ///////////////////////////////
    		// End the div
    		endElement(METS,"div");
    	}

    	// ////////////////////////////////
    	// End the special first division
    	endElement(METS,"div");
    	
    	// ///////////////////////
    	// End the structure map
    	endElement(METS,"structMap");	
    }
    


    /**
     * Render any extra METS section. If the item contains a METS.xml document
     * then all of that document's sections are included in this document's
     * METS document.
     */
    protected void renderExtraSections() throws SAXException, SQLException, IOException
    {	
    	Boolean include = ConfigurationManager.getBooleanProperty("xmlui.bitstream.mets");
    	if (!include)
    		return;
    		
    		
    	Bundle[] bundles = item.getBundles("METADATA");

    	for (Bundle bundle : bundles)
    	{
    		Bitstream bitstream = bundle.getBitstreamByName("METS.xml");

    		if (bitstream == null)
    			continue;

    		// ///////////////////////////////
    		// Send the actual XML content
    		try {
	    		SAXFilter filter = new SAXFilter(contentHandler, lexicalHandler, namespaces);
	    		// Allow the basics for XML
	    		filter.allowIgnorableWhitespace().allowCharacters().allowCDATA().allowPrefixMappings();
	    		// Special option, only allow elements below the second level to pass through. This
	    		// will trim out the METS declaration and only leave the actual METS parts to be
	    		// included.
	    		filter.allowElements(1);
	    		
	    		
	    		XMLReader reader = XMLReaderFactory.createXMLReader();
	    		reader.setContentHandler(filter);
	    		reader.setProperty("http://xml.org/sax/properties/lexical-handler", filter);
	    		reader.parse(new InputSource(bitstream.retrieve()));
    		}
			catch (AuthorizeException ae)
			{
				// just ignore the authorize exception and continue on with
				//out parsing the xml document.
			}
    	}
    }

    
    
    
    /**
     * For a bitstream that's a thumbnail or extracted text, find the
     * corresponding bitstream it was derived from, in the ORIGINAL bundle.
     * 
     * @param item
     *            the item we're dealing with
     * @param derived
     *            the derived bitstream
     * 
     * @return the corresponding original bitstream (or null)
     */
    protected static Bitstream findOriginalBitstream(Item item,Bitstream derived) throws SQLException
    {
        // FIXME: this method is a copy of the one found below. However the
        // original method is protected so we can't use it here. I think that
        // perhaps this should be folded into the DSpace bitstream API. Untill
        // then a good final solution can be determined I am just going to copy
        // the method here.
        //
        // return org.dspace.content.packager.AbstractMetsDissemination
        // .findOriginalBitstream(item, derived);

        Bundle[] bundles = item.getBundles();

        // Filename of original will be filename of the derived bitstream
        // minus the extension (last 4 chars - .jpg or .txt)
        String originalFilename = derived.getName().substring(0,
                derived.getName().length() - 4);

        // First find "original" bundle
        for (int i = 0; i < bundles.length; i++)
        {
            if ((bundles[i].getName() != null)
                    && bundles[i].getName().equals("ORIGINAL"))
            {
                // Now find the corresponding bitstream
                Bitstream[] bitstreams = bundles[i].getBitstreams();

                for (int bsnum = 0; bsnum < bitstreams.length; bsnum++)
                {
                    if (bitstreams[bsnum].getName().equals(originalFilename))
                    {
                        return bitstreams[bsnum];
                    }
                }
            }
        }

        // Didn't find it
        return null;
    }
}
