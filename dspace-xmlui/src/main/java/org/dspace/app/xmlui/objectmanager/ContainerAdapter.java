/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.objectmanager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.ItemCounter;
import org.dspace.browse.ItemCountException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.SAXOutputter;
import org.xml.sax.SAXException;

/**
 * This is an adapter which translates DSpace containers 
 * (communities and collections) into METS documents. This adapter follows
 * the DSpace METS profile, however that profile does not define how a
 * community or collection should be described, but we make the obvious 
 * decisions to deviate when necessary from the profile.
 * 
 * The METS document consists of three parts: descriptive metadata section,
 * file section, and a structural map. The descriptive metadata sections holds
 * metadata about the item being adapted using DSpace crosswalks. This is the 
 * same way the item adapter works.
 * 
 * However, the file section and structural map are a bit different. In these
 * cases the the only files listed is the one logo that may be attached to 
 * a community or collection.
 * 
 * @author Scott Phillips
 */
public class ContainerAdapter extends AbstractAdapter
{
    private static final Logger log = Logger.getLogger(ContainerAdapter.class);

    /** The community or collection this adapter represents. */
    private final DSpaceObject dso;

    /** A space-separated list of descriptive metadata sections */
    private StringBuffer dmdSecIDS;
    
    /** Current DSpace context **/
    private final Context dspaceContext;

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
   	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    
    
    /**
     * Construct a new CommunityCollectionMETSAdapter.
     * 
     * @param context session context.
     * @param dso
     *            A DSpace Community or Collection to adapt.
     * @param contextPath
     *            The contextPath of this webapplication.
     */
    public ContainerAdapter(Context context, DSpaceObject dso,String contextPath)
    {
        super(contextPath);
        this.dso = dso;
        this.dspaceContext = context;
    }

    /** Return the container, community or collection, object
     * @return the contained object.
     */
    public DSpaceObject getContainer()
    {
    	return this.dso;
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
     * @return the URL of this community/collection in the interface.
     */
    @Override
    protected String getMETSOBJID()
    {
    	if (dso.getHandle() != null)
        {
            return contextPath + "/handle/" + dso.getHandle();
        }
    	return null;
    }

    /**
     * @return Return the URL for editing this item
     */
    @Override
    protected String getMETSOBJEDIT()
    {
        return null;
    }
    
    /**
     * Use the handle as the id for this METS document.
     * @return the id.
     */
    @Override
    protected String getMETSID()
    {
    	if (dso.getHandle() == null)
    	{
        	if (dso instanceof Collection)
            {
                return "collection:" + dso.getID();
            }
        	else
            {
                return "community:" + dso.getID();
            }
    	}
        else
        {
            return "hdl:" + dso.getHandle();
        }
    }

    /**
     * Return the profile to use for communities and collections.
     * 
     * @return the constant profile name.
     * @throws org.dspace.app.xmlui.wing.WingException never.
     */
    @Override
    protected String getMETSProfile() throws WingException
    {
    	return "DSPACE METS SIP Profile 1.0";
    }

    /**
     * @return a friendly label for the METS document to say we are a community
     * or collection.
     */
    @Override
    protected String getMETSLabel()
    {
        if (dso instanceof Community)
        {
            return "DSpace Community";
        }
        else
        {
            return "DSpace Collection";
        }
    }

    /**
     * @param bitstream the bitstream to be identified.
     * @return a unique id for the given bitstream.
     */
    protected String getFileID(Bitstream bitstream)
    {
        return "file_" + bitstream.getID();
    }

    /**
     * @param bitstream the bitstream to be queried.
     * @return a group id for the given bitstream.
     */
    protected String getGroupFileID(Bitstream bitstream)
    {
        return "group_file_" + bitstream.getID();
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
     * Render the METS descriptive section. This will create a new metadata
     * section for each crosswalk configured.
     * 
     * <p>Example:
     *
     * <pre>{@code
     * <dmdSec>
     *  <mdWrap MDTYPE="MODS">
     *    <xmlData>
     *      ... content from the crosswalk ...
     *    </xmlDate>
     *  </mdWrap>
     * </dmdSec>
     * } </pre>
     * @throws org.dspace.app.xmlui.wing.WingException on XML errors.
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.dspace.content.crosswalk.CrosswalkException passed through.
     * @throws java.io.IOException if items could not be counted.
     * @throws java.sql.SQLException passed through.
     */
    @Override
    protected void renderDescriptiveSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException 
    {
        AttributeMap attributes;
        
        String groupID = getGenericID("group_dmd_");
        dmdSecIDS = new StringBuffer();

        // Add DIM descriptive metadata if it was requested or if no metadata types 
        // were specified. Furthermore, since this is the default type we also use a 
        // faster rendering method that the crosswalk API.
        if(dmdTypes.isEmpty() || dmdTypes.contains("DIM"))
        {
            // Metadata element's ID
            String dmdID = getGenericID("dmd_");
            
            // Keep track of all descriptive sections
            dmdSecIDS.append(dmdID);
            
            
            // ////////////////////////////////
            // Start a new dmdSec for each crosswalk.
            attributes = new AttributeMap();
            attributes.put("ID", dmdID);
            attributes.put("GROUPID", groupID);
            startElement(METS,"dmdSec",attributes);
            
            // ////////////////////////////////
            // Start metadata wrapper
            attributes = new AttributeMap();
            attributes.put("MDTYPE", "OTHER");
            attributes.put("OTHERMDTYPE", "DIM");
            startElement(METS,"mdWrap",attributes);

            // ////////////////////////////////
            // Start the xml data
            startElement(METS,"xmlData");
            
            
            // ///////////////////////////////
            // Start the DIM element
            attributes = new AttributeMap();
            attributes.put("dspaceType", Constants.typeText[dso.getType()]);
            startElement(DIM,"dim",attributes);

            // Add each field for this collection
            if (dso.getType() == Constants.COLLECTION) 
            {
                Collection collection = (Collection) dso;
                
                String description = collectionService.getMetadata(collection, "introductory_text");
                String description_abstract = collectionService.getMetadata(collection, "short_description");
                String description_table = collectionService.getMetadata(collection, "side_bar_text");
                String identifier_uri = "http://hdl.handle.net/" + collection.getHandle();
                String provenance = collectionService.getMetadata(collection, "provenance_description");
                String rights = collectionService.getMetadata(collection, "copyright_text");
                String rights_license = collectionService.getMetadata(collection, "license");
                String title = collectionService.getMetadata(collection, "name");
                
                createField("dc","description",null,null,description);
                createField("dc","description","abstract",null,description_abstract);
                createField("dc","description","tableofcontents",null,description_table);
                createField("dc","identifier","uri",null,identifier_uri);
                createField("dc","provenance",null,null,provenance);
                createField("dc","rights",null,null,rights);
                createField("dc","rights","license",null,rights_license);
                createField("dc","title",null,null,title);
                
                boolean showCount = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.strengths.show");
                 
                if (showCount)
                {
                    try
                    {	// try to determine Collection size (i.e. # of items)
                        int size = new ItemCounter(this.dspaceContext).getCount(collection);
                        createField("dc","format","extent",null, String.valueOf(size)); 
                    }
                    catch (ItemCountException e)
                    {
                        throw new IOException("Could not obtain Collection item count", e);
                    }
                }
            } 
            else if (dso.getType() == Constants.COMMUNITY) 
            {
                Community community = (Community) dso;
                
                String description = communityService.getMetadata(community, "introductory_text");
                String description_abstract = communityService.getMetadata(community, "short_description");
                String description_table = communityService.getMetadata(community, "side_bar_text");
                String identifier_uri = "http://hdl.handle.net/" + community.getHandle();
                String rights = communityService.getMetadata(community, "copyright_text");
                String title = communityService.getMetadata(community, "name");
                
                createField("dc","description",null,null,description);
                createField("dc","description","abstract",null,description_abstract);
                createField("dc","description","tableofcontents",null,description_table);
                createField("dc","identifier","uri",null,identifier_uri);
                createField("dc","rights",null,null,rights);
                createField("dc","title",null,null,title);
                
                boolean showCount = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.strengths.show");
        		
                if (showCount)
                {
                    try
                    {	// try to determine Community size (i.e. # of items)
                        int size = new ItemCounter(this.dspaceContext).getCount(community);
                        createField("dc","format","extent",null, String.valueOf(size)); 
                    }
                    catch (ItemCountException e)
                    {
                        throw new IOException("Could not obtain Collection item count", e);
                    }
                }
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
        
    	for (String dmdType : dmdTypes)
    	{
    		// If DIM was requested then it was generated above without using
    		// the crosswalk API. So we can skip this one.
    		if ("DIM".equals(dmdType))
            {
                continue;
            }
    		
    		DisseminationCrosswalk crosswalk = getDisseminationCrosswalk(dmdType);
    		
    		if (crosswalk == null)
            {
                continue;
            }
    		
        	String dmdID = getGenericID("dmd_");
	   		// Add our id to the list.
            dmdSecIDS.append(" ").append(dmdID);

            // ////////////////////////////////
            // Start a new dmdSec for each crosswalk.
            attributes = new AttributeMap();
            attributes.put("ID", dmdID);
            attributes.put("GROUPID", groupID);
            startElement(METS,"dmdSec",attributes);
            
            // ////////////////////////////////
            // Start metadata wrapper
            attributes = new AttributeMap();
            if (isDefinedMETStype(dmdType))
            {
            	attributes.put("MDTYPE", dmdType);
            }
            else
            {
            	attributes.put("MDTYPE", "OTHER");
            	attributes.put("OTHERMDTYPE", dmdType);
            }
            startElement(METS,"mdWrap",attributes);

            // ////////////////////////////////
            // Start the xml data
            startElement(METS,"xmlData");
            
            // ///////////////////////////////
            // Send the actual XML content
            try {
	    		Element dissemination = crosswalk.disseminateElement(dspaceContext, dso);
	
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
				// just ignore the authorize exception and continue on
				// without parsing the xml document.
			}
    		
            
            // ////////////////////////////////
            // End elements
            endElement(METS,"xmlData");
            endElement(METS,"mdWrap");
            endElement(METS, "dmdSec");
            
            
            // Record keeping
            if (dmdSecIDS == null)
            {
                dmdSecIDS = new StringBuffer(dmdID);
            }
            else
            {
                dmdSecIDS.append(" ").append(dmdID);

            }
        }
    }

    /**
     * Render the METS file section. If a logo is present for this
     * container then that single bitstream is listed in the 
     * file section.
     *
     * <p>Example:
     *
     * <pre>{@code
     * <fileSec>
     *   <fileGrp USE="LOGO">
     *     <file ... >
     *       <fLocate ... >
     *     </file>
     *   </fileGrp>
     * </fileSec>
     * }</pre>
     * @param context session context.
     * @throws org.xml.sax.SAXException passed through.
     * @throws java.sql.SQLException passed through.
     */
    @Override
    protected void renderFileSection(Context context) throws SAXException, SQLException
    {
    	AttributeMap attributes;
    	
        // Get the Community or Collection logo.
        Bitstream logo = getLogo();

        if (logo != null)
        {
            // ////////////////////////////////
            // Start the file section
        	startElement(METS,"fileSec");

            // ////////////////////////////////
            // Start a new fileGrp for the logo.
            attributes = new AttributeMap();
            attributes.put("USE", "LOGO");
            startElement(METS,"fileGrp",attributes);
            
            // ////////////////////////////////
            // Add the actual file element
            String fileID = getFileID(logo);
            String groupID = getGroupFileID(logo);
            renderFile(context, null, logo, fileID, groupID);
            
            // ////////////////////////////////
            // End th file group and file section
            endElement(METS,"fileGrp");
            endElement(METS,"fileSec");
        }
    }

    /**
     * Render the container's structural map. This includes a reference
     * to the container's logo, if available, otherwise it is an empty 
     * division that just states it is a DSpace community or Collection.
     * 
     * <p>Example:
     *
     * <pre>{@code
     * <structMap TYPE="LOGICAL" LABEL="DSpace">
     *   <div TYPE="DSpace Collection" DMDID="space-separated list of ids">
     *     <fptr FILEID="logo id"/>
     *   </div>
     * </structMap>
     * }</pre>
     * @throws java.sql.SQLException passed through.
     * @throws org.xml.sax.SAXException passed through.
     */
    @Override
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
    	attributes.put("TYPE", getMETSLabel());
    	// add references to the Descriptive metadata
    	if (dmdSecIDS != null)
        {
            attributes.put("DMDID", dmdSecIDS.toString());
        }
    	startElement(METS,"div",attributes);
    	
    	
    	// add a fptr pointer to the logo.
        Bitstream logo = getLogo();
        if (logo != null)
        {
            // ////////////////////////////////
            // Add a reference to the logo as the primary bitstream.
            attributes = new AttributeMap();
            attributes.put("FILEID",getFileID(logo));
            startElement(METS,"fptr",attributes);
            endElement(METS,"fptr");
        
            
            // ///////////////////////////////////////////////
            // Add a div for the publicly viewable bitstreams (i.e. the logo)
            attributes = new AttributeMap();
            attributes.put("ID", getGenericID("div_"));
            attributes.put("TYPE", "DSpace Content Bitstream");
            startElement(METS,"div",attributes);
        	
            // ////////////////////////////////
            // Add a reference to the logo as the primary bitstream.
            attributes = new AttributeMap();
            attributes.put("FILEID",getFileID(logo));
            startElement(METS,"fptr",attributes);
            endElement(METS,"fptr");
        	
            // //////////////////////////
            // End the logo division
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
     * 
     * 
     * 
     * Private helpful methods
     * 
     * 
     * 
     */

    /**
     * Return the logo bitstream associated with this community or collection.
     * If there is no logo then null is returned.
     */
    private Bitstream getLogo()
    {
        if (dso instanceof Community)
        {
            Community community = (Community) dso;
            return community.getLogo();
        }
        else if (dso instanceof Collection)
        {

            Collection collection = (Collection) dso;
            return collection.getLogo();
        }
        return null;
    }
    
    /**
     * Count how many occurrence there is of the given
     * character in the given string.
     * 
     * @param string The string value to be counted.
     * @param character the character to count in the string.
     */
    private int countOccurences(String string, char character)
    {
    	if (string == null || string.length() == 0)
        {
            return 0;
        }
    	
    	int fromIndex = -1;
        int count = 0;
        
        while (true)
        {
        	fromIndex = string.indexOf('>', fromIndex+1);
        	
        	if (fromIndex == -1)
            {
                break;
            }
        	
        	count++;
        }
        
        return count;
    }
    
    /**
     * Check if the given character sequence is located in the given
     * string at the specified index. If it is then return true, otherwise false.
     * 
     * @param string The string to test against
     * @param index The location within the string
     * @param characters The character sequence to look for.
     * @return true if the character sequence was found, otherwise false.
     */
    private boolean substringCompare(String string, int index, char ... characters)
    {
    	// Is the string long enough?
    	if (string.length() <= index + characters.length)
        {
            return false;
        }
    	
    	// Do all the characters match?
    	for (char character : characters)
    	{
    		if (string.charAt(index) != character)
            {
                return false;
            }
    		index++;
    	}
    	
    	return false;
    }
    
    /**
     * Create a new DIM field element with the given attributes.
     * 
     * @param schema The schema the DIM field belongs too.
     * @param element The element the DIM field belongs too.
     * @param qualifier The qualifier the DIM field belongs too.
     * @param language The language the DIM field belongs too.
     * @param value The value of the DIM field.
     * @return A new DIM field element
     * @throws SAXException 
     */
    private void createField(String schema, String element, String qualifier, String language, String value) throws SAXException
    {
    	// ///////////////////////////////
    	// Field element for each metadata field.
    	AttributeMap attributes = new AttributeMap();
		attributes.put("mdschema",schema);
		attributes.put("element", element);
		if (qualifier != null)
        {
            attributes.put("qualifier", qualifier);
        }
		if (language != null)
        {
            attributes.put("language", language);
        }
		startElement(DIM,"field",attributes);
		
		// Only try and add the metadata value, but only if it is non-null.
    	if (value != null)
    	{
    		// First, perform a quick check to see if the value may be XML.
	        int countOpen = countOccurences(value,'<');
	        int countClose = countOccurences(value, '>');
	        
	        // If it passed the quick test, then try and parse the value.
	        Element xmlDocument = null;
	        if (countOpen > 0 && countOpen == countClose)
	        {
	        	// This may be XML, First try and remove any bad entity references.
	        	int amp = -1;
	        	while ((amp = value.indexOf('&', amp+1)) > -1)
	        	{
	        		// Is it an xml entity named by number?
	        		if (substringCompare(value,amp+1,'#'))
                    {
                        continue;
                    }
	        		
	        		// &amp;
	        		if (substringCompare(value,amp+1,'a','m','p',';'))
                    {
                        continue;
                    }
	        		
	        		// &apos;
	        		if (substringCompare(value,amp+1,'a','p','o','s',';'))
                    {
                        continue;
                    }
	        		
	        		// &quot;
	        		if (substringCompare(value,amp+1,'q','u','o','t',';'))
                    {
                        continue;
                    }
	        			
	        		// &lt;
	        		if (substringCompare(value,amp+1,'l','t',';'))
                    {
                        continue;
                    }
	        		
	        		// &gt;
	        		if (substringCompare(value,amp+1,'g','t',';'))
                    {
                        continue;
                    }
	        		
	        		// Replace the ampersand with an XML entity.
	        		value = value.substring(0,amp) + "&amp;" + value.substring(amp+1);
	        	}
	        	
	        	
	        	// Second try and parse the XML into a mini-dom
	        	try {
	        		// Wrap the value inside a root element (which will be trimed out 
	        		// by the SAX filter and set the default namespace to XHTML. 
		        	String xml = "<?xml version='1.0' encoding='UTF-8'?><fragment xmlns=\"http://www.w3.org/1999/xhtml\">"+value+"</fragment>";

		            ByteArrayInputStream inputStream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		     	   
			 	    SAXBuilder builder = new SAXBuilder();
					Document document = builder.build(inputStream);
					
					xmlDocument = document.getRootElement();
	        	} 
	        	catch (JDOMException | IOException e)
				{
                    log.trace("Caught exception", e);
				}
	        }		
					
	        // Third, If we have xml, attempt to serialize the dom.
	        if (xmlDocument != null)
	        {	
	        	SAXFilter filter = new SAXFilter(contentHandler, lexicalHandler, namespaces);
	    		// Allow the basics for XML
	    		filter.allowElements().allowIgnorableWhitespace().allowCharacters().allowCDATA().allowPrefixMappings();
	    		// Special option, only allow elements below the second level to pass through. This
	    		// will trim out the METS declaration and only leave the actual METS parts to be
	    		// included.
	    		filter.allowElements(1);
	    		
	            SAXOutputter outputter = new SAXOutputter();
	            outputter.setContentHandler(filter);
	            outputter.setLexicalHandler(filter);
	            try {
	            	outputter.output(xmlDocument);
	            } 
	            catch (JDOMException jdome)
	            {
	            	// serialization failed so let's just fallback sending the plain characters.
	            	sendCharacters(value);
	            }
	        }
	        else
	        {
	        	// We don't have XML, so just send the plain old characters.
	        	sendCharacters(value);
	        }
    	}
        
        // //////////////////////////////
        // Close out field
        endElement(DIM,"field");
    }  
}
