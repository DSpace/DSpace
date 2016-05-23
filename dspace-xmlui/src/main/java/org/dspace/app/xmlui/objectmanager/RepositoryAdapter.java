/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.objectmanager;

import java.sql.SQLException;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Namespace;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.xml.sax.SAXException;

/**
 * This is an an adapter which translates a DSpace repository into a METS 
 * document. Unfortunately, there is no real definition of what this is. So
 * we just kind of made it up based upon what we saw for the item profile.
 * 
 * The basic structure is simply two parts:  the descriptive metadata and a
 * structural map. The descriptive metadata is a place to put metadata about 
 * the whole repository. The structural map is used to map relationships
 * between communities and collections in DSpace.
 * 
 * @author Scott Phillips
 */
public class RepositoryAdapter extends AbstractAdapter
{

	/** MODS namespace */
    public static final String MODS_URI = "http://www.loc.gov/mods/v3";
    public static final Namespace MODS = new Namespace(MODS_URI);

	
    /** A space separated list of descriptive metadata sections */
    private String dmdSecIDS;
    
    /** Dspace context to be able to look up additional objects */
    private final Context context;

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    /**
     * Construct a new RepositoryAdapter
     * 
     * @param context
     *            The DSpace context to look up communities / collections.
     * 
     * @param contextPath
     *            The context Path of this web application.
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
     * @return the handle prefix as the identifier.
     */
    @Override
    protected String getMETSID()
    {
        return handleService.getPrefix();
    }
    
	/**
	 * The OBJID is used to encode the URL to the object, in this
	 * case the repository which is just at the contextPath.
     * @return local path to the object.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
	 */
    @Override
	protected String getMETSOBJID() throws WingException {
		
		if (contextPath == null)
        {
            return "/";
        }
		else
        {
            return contextPath + "/";
        }
	}

    /**
     * @return  Return the URL for editing this item
     */
    @Override
    protected String getMETSOBJEDIT()
    {
        return null;
    }

    /**
     * @return the profile this METS document conforms to...
     *
     * FIXME: It doesn't conform to a profile. This needs to be fixed.
     */
    @Override
    protected String getMETSProfile()
    {
        return "DRI DSPACE Repository Profile 1.0";
    }

    /**
     * @return a friendly label for the METS document stating that this is a
     * DSpace repository.
     */
    @Override
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
     * For a the DSpace repository we just grab a few items 
     * from the config file and put them into the descriptive 
     * section, such as the name, hostname, handle prefix, and 
     * default language.
     * 
     * @throws org.xml.sax.SAXException passed through.
     */
    @Override
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
		sendCharacters(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.name"));
		endElement(DIM,"field");
		
		// Entry for dspace.hostname
		attributes = new AttributeMap();
		attributes.put("mdschema","dspace");
		attributes.put("element", "hostname");
		startElement(DIM,"field",attributes);
		sendCharacters(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.hostname"));
		endElement(DIM,"field");
		
		// Entry for handle.prefix
		attributes = new AttributeMap();
		attributes.put("mdschema","dspace");
		attributes.put("element", "handle");
		startElement(DIM,"field",attributes);
		sendCharacters(handleService.getPrefix());
		endElement(DIM,"field");
		
		// Entry for default.language
		attributes = new AttributeMap();
		attributes.put("mdschema","dspace");
		attributes.put("element", "default");
		attributes.put("qualifier", "language");
		startElement(DIM,"field",attributes);
		sendCharacters(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("default.language"));
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
     * Render the repository's structure map. This map will include a reference to
     * all the community and collection objects showing how they are related to
     * one another. 
     * @throws java.sql.SQLException passed through.
     * @throws org.xml.sax.SAXException passed through.
     */
    @Override
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
        {
            attributes.put("DMDID", dmdSecIDS);
        }
        startElement(METS,"div",attributes);

        // Put each root level node into the document.
        for (Community community : communityService.findAllTop(context))
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
     * private helpful methods
     * 
     * 
     * 
     */

    /**
     * Recursively walk the DSpace hierarchy, rendering each container and subcontainer.
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
        {
            attributes.put("TYPE", "DSpace Community");
        }
        else if (dso instanceof Collection)
        {
            attributes.put("TYPE", "DSpace Collection");
        }
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
        
        // Recurse to ensure that our children are also included even if this
        // node already existed in the div structure.
        if (dso instanceof Community)
        {
        	for (DSpaceObject child : ((Community)dso).getCollections())
            {
                renderStructuralDiv(child);
            }
        	
        	for (DSpaceObject child : ((Community)dso).getSubcommunities())
            {
                renderStructuralDiv(child);
            }
        }
        
        // ////////////////////
        // Close division
        endElement(METS,"div");
    }
}
