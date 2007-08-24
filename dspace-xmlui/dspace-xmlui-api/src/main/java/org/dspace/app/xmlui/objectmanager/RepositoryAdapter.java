/*
 * RepositoryAdapter.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/04/26 18:26:46 $
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

import java.sql.SQLException;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Namespace;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

/**
 * This is an an adapter which translates a DSpace repository into a METS 
 * document. Unfortunitaly there is no real definition of what this is. So
 * we just kind of made it up based upon what we saw for the item profile.
 * 
 * The basic structure is simply two parts, the descriptive metadata and a 
 * structural map. The descriptive metadata is a place to put metadata about 
 * the whole repository. The structural map is used to map relationships
 * between communities & collections in dspace. 
 * 
 * @author Scott Phillips
 */
public class RepositoryAdapter extends AbstractAdapter
{

	/** MODS namespace */
    public static String MODS_URI = "http://www.loc.gov/mods/v3";
    public static Namespace MODS = new Namespace(MODS_URI);

	
    /** A space seperated list of descriptive metadata sections */
    private String dmdSecIDS;
    
    /** Dspace context to be able to look up additional objects */
    private Context context;
    
    /**
     * Construct a new RepositoryAdapter
     * 
     * @param context
     * 			  The DSpace context to look up communities / collections.
     * 
     * @param contextPath
     *            The contextPath of this webapplication.
     */
    public RepositoryAdapter(Context context, String contextPath)
    {
        super(contextPath);
        this.context = context;
    }

    /**
     * 
     * 
     * 
     * Abstract methods
     * 
     * 
     * 
     */

    /**
     * Return the handle prefix as the identifier.
     */
    protected String getMETSID()
    {
        return ConfigurationManager.getProperty("handle.prefix");
    }
    
	/**
	 * The OBJID is used to encode the URL to the object, in this
	 * case the repository which is just at the contextPath.
	 */
	protected String getMETSOBJID() throws WingException {
		
		if (contextPath == null)
			return "/";
		else
			return contextPath + "/";
	}

    /**
     * @return  Return the URL for editing this item
     */
    protected String getMETSOBJEDIT()
    {
        return null;
    }

    /**
     * Return the profile this METS document conforms too...
     * 
     * FIXME: It dosn't conform to a profile. This needs to be fixed.
     */
    protected String getMETSProfile()
    {
        return "DRI DSPACE Repository Profile 1.0";
    }

    /**
     * Return a friendly label for the METS document stating that this is a
     * DSpace repository.
     */
    protected String getMETSLabel()
    {
        return "DSpace Repository";
    }

    
    /**
     * 
     * 
     * 
     * METS structural methods
     * 
     * 
     * 
     */

    /**
     * Render the repository's descriptive metadata section.
     *
     * For a the DSPace repository we just grab a few items 
     * from the config file and put them into the descriptive 
     * section, such as the name, hostname, handle prefix, and 
     * default language.
     * 
     */
	protected void renderDescriptiveSection() throws SAXException
    {
    	AttributeMap attributes;
    	
    	// Generate our ids
        String dmdID = getGenericID("dmd_");
        String groupID = getGenericID("group_dmd_");

        // ////////////////////////////////
        // Start a single dmdSec
        attributes = new AttributeMap();
        attributes.put("ID", dmdID);
        attributes.put("GROUPID", groupID);
        startElement(METS,"dmdSec",attributes);

        // ////////////////////////////////
        // Start a metadata wrapper (hardcoded to mods)
        attributes = new AttributeMap();
        attributes.put("MDTYPE", "OTHER");
        attributes.put("OTHERMDTYPE", "DIM");
        startElement(METS,"mdWrap",attributes);

        // ////////////////////////////////
        // Start the xml data
        startElement(METS,"xmlData");
        
        /////////////////////////////////
		// Start the DIM element			
		attributes = new AttributeMap();
		attributes.put("dspaceType", Constants.typeText[Constants.SITE]);
		startElement(DIM,"dim",attributes);
		
		// Entry for dspace.name
		attributes = new AttributeMap();
		attributes.put("mdschema","dspace");
		attributes.put("element", "name");
		startElement(DIM,"field",attributes);
		sendCharacters(ConfigurationManager.getProperty("dspace.name"));
		endElement(DIM,"field");
		
		// Entry for dspace.hostname
		attributes = new AttributeMap();
		attributes.put("mdschema","dspace");
		attributes.put("element", "hostname");
		startElement(DIM,"field",attributes);
		sendCharacters(ConfigurationManager.getProperty("dspace.hostname"));
		endElement(DIM,"field");
		
		// Entry for handle.prefix
		attributes = new AttributeMap();
		attributes.put("mdschema","dspace");
		attributes.put("element", "handle");
		startElement(DIM,"field",attributes);
		sendCharacters(ConfigurationManager.getProperty("handle.prefix"));
		endElement(DIM,"field");
		
		// Entry for default.language
		attributes = new AttributeMap();
		attributes.put("mdschema","dspace");
		attributes.put("element", "default");
		attributes.put("qualifier", "language");
		startElement(DIM,"field",attributes);
		sendCharacters(ConfigurationManager.getProperty("default.language"));
		endElement(DIM,"field");
		
        // ///////////////////////////////
		// End the DIM element
		endElement(DIM,"dim");
        
        // End all the open elements.
        endElement(METS,"xmlData");
        endElement(METS,"mdWrap");
        endElement(METS,"dmdSec");

        // Remember the IDS
        this.dmdSecIDS = dmdID;
    }

    /**
     * Render the repository's structure map. This map will include a refrence to
     * all the community and collection objects showing how they are related to
     * one another. 
     */
	protected void renderStructureMap() throws SQLException, SAXException
    {
    	AttributeMap attributes;
        
    	// //////////////////////////
    	// Start the new struct map
        attributes = new AttributeMap();
        attributes.put("TYPE", "LOGICAL");
        attributes.put("LABEL", "DSpace");
        startElement(METS,"structMap",attributes);
        
        // ////////////////////////////////
        // Start the special first division
        attributes = new AttributeMap();
        attributes.put("TYPE", "DSpace Repository");
        // add references to the Descriptive metadata
        if (dmdSecIDS != null)
            attributes.put("DMDID", dmdSecIDS);
        startElement(METS,"div",attributes);

        // Put each root level node into the document.
        for (Community community : Community.findAllTop(context))
        {
            renderStructuralDiv(community);
        }
        
        
        // //////////////////
        // Close special first division and structural map
        endElement(METS,"div");
        endElement(METS,"structMap");
        
    }

    /**
     * 
     * 
     * 
     * private helpfull methods
     * 
     * 
     * 
     */

    /**
     * Recursively the DSpace hirearchy rendering each container and subcontainers.
     *
     * @param dso
     *            The DSpace Object to be rendered.
     */
    private void renderStructuralDiv(DSpaceObject dso) throws SAXException, SQLException
    {
    	AttributeMap attributes;

        // ////////////////////////////////
        // Start the new div for this repository container
        attributes = new AttributeMap();
        if (dso instanceof Community)
        	attributes.put("TYPE", "DSpace Community");
        else if (dso instanceof Collection)
        	attributes.put("TYPE", "DSpace Collection");
        startElement(METS,"div",attributes);
        
        // //////////////////////////////////
        // Start a metadata pointer for this container
        attributes = new AttributeMap();
        AttributeMap attributesXLINK = new AttributeMap();
        attributesXLINK.setNamespace(XLINK);
        
        attributes.put("LOCTYPE", "URL");
        attributesXLINK.put("href", "/metadata/handle/"+ dso.getHandle() +"/mets.xml");
        startElement(METS,"mptr",attributes,attributesXLINK);
        endElement(METS,"mptr");
        
        // Recurse to insure that our children are also included even if this
        // node allready existed in the div structure.
        if (dso instanceof Community)
        {
        	for (DSpaceObject child : ((Community)dso).getCollections())
        		renderStructuralDiv(child);
        	
        	for (DSpaceObject child : ((Community)dso).getSubcommunities())
        		renderStructuralDiv(child);
        }
        
        // ////////////////////
        // Close division
        endElement(METS,"div");
    }
}
