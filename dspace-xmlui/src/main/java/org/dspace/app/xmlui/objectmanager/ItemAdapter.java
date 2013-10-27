/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.objectmanager;

import org.dspace.app.util.MetadataExposure;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.SAXOutputter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.*;

import org.dspace.content.DSpaceObject;


/**
 * This is an adapter which translates a DSpace item into a METS document
 * following the DSpace METS profile, err well mostly. At least if you use
 * the proper configuration it will be fully compliant with the profile,
 * however this adapter will allow you to configure it to be incorrect.
 *
 * When we are configured to be non-compliant with the profile, the MET's
 * profile is changed to reflect the deviation. The DSpace profile states
 * that metadata should be given in MODS format. However, you can configure
 * this adapter to use any metadata crosswalk. When that case is detected we
 * change the profile to say that we are deviating from the standard profile
 * and it lists what metadata has been added.
 *
 * There are four parts to an item's METS document: descriptive metadata,
 * file section, structural map, and extra sections.
 * 
 * Request item-support
 * Original Concept, JSPUI version:    Universidade do Minho   at www.uminho.pt
 * Sponsorship of XMLUI version:    Instituto Oceanográfico de España at www.ieo.es
 * 
 * @author Scott Phillips
 * @author Adán Román Ruiz at arvo.es (for request item support) 
 */

public class ItemAdapter extends AbstractAdapter
{
    /** The item this METS adapter represents */
    private Item item;

    /** List of bitstreams which should be publicly viewable */
    private List<Bitstream> contentBitstreams = new ArrayList<Bitstream>();

    /** The primary bitstream, or null if none specified */
    private Bitstream primaryBitstream;

    /** A space-separated list of descriptive metadata sections */
    private StringBuffer dmdSecIDS;

    /** A space-separated list of administrative metadata sections (for item)*/
    private StringBuffer amdSecIDS;
    
    /** A hashmap of all Files and their corresponding space separated list of
        administrative metadata sections */
    private Map<String,StringBuffer> fileAmdSecIDs = new HashMap<String,StringBuffer>();

    // DSpace DB context
    private Context context;

    /**
     * Construct a new ItemAdapter
     *
     * @param item
     *            The DSpace item to adapt.
     * @param contextPath
     *            The contextpath for this webapplication.
     */
    public ItemAdapter(Context context, Item item,String contextPath)
    {
        super(contextPath);
        this.item = item;
        this.context = context;
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
        {
            return contextPath + "/handle/" + item.getHandle();
        }
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
        {
            return "item:" + item.getID();
        }
        else
        {
            return "hdl:" + item.getHandle();
        }
    }

    /**
     * Return the official METS SIP Profile.
     */
    protected String getMETSProfile() throws WingException
    {
        return "DSPACE METS SIP Profile 1.0";
    }

    /**
     * Return a helpful label that this is a DSpace Item.
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
     * Return a techMD id for a bitstream.
     */
    protected String getAmdSecID(String admSecName, String mdType, DSpaceObject dso)
    {
        if (dso.getType() == Constants.BITSTREAM)
        {
            return admSecName + "_" + getFileID((Bitstream) dso) + "_" + mdType;
        }
        else
        {
            return admSecName + "_" + dso.getID() + "_" + mdType;
        }
    }

    /**
     * Render the METS descriptive section. This will create a new metadata
     * section for each crosswalk configured. Furthermore, a special check
     * has been added that will add MODS descriptive metadata if it is
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
        // were specified. Furthermore, since this is the default type we also use a
        // faster rendering method that the crosswalk API.
        if(dmdTypes.size() == 0 || dmdTypes.contains("DIM"))
        {
                // Metadata element's ID
                String dmdID = getGenericID("dmd_");
                // Keep track of all descriptive sections
                dmdSecIDS.append(dmdID);
                
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
            {
                attributes.put("withdrawn", "y");
            }
            startElement(DIM,"dim",attributes);
                        
                DCValue[] dcvs = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
                for (DCValue dcv : dcvs)
                {
                        if (!MetadataExposure.isHidden(context, dcv.schema, dcv.element, dcv.qualifier))
                        {
                        // ///////////////////////////////
                        // Field element for each metadata field.
                        attributes = new AttributeMap();
                        attributes.put("mdschema",dcv.schema);
                        attributes.put("element", dcv.element);
                        if (dcv.qualifier != null)
                        {
                            attributes.put("qualifier", dcv.qualifier);
                        }
                        if (dcv.language != null)
                        {
                            attributes.put("language", dcv.language);
                        }
                        if (dcv.authority != null || dcv.confidence != Choices.CF_UNSET)
                        {
                                attributes.put("authority", dcv.authority);
                                attributes.put("confidence", Choices.getConfidenceText(dcv.confidence));
                        }
                        startElement(DIM,"field",attributes);
                        sendCharacters(dcv.value);
                        endElement(DIM,"field");
                }
                }
                        
                // ///////////////////////////////
                        // End the DIM element
                        endElement(DIM,"dim");
                        
                // ////////////////////////////////
                // End elements
                endElement(METS,"xmlData");
                endElement(METS,"mdWrap");
                endElement(METS,"dmdSec");

        }
                
        
        // Add any extra crosswalks that may configured.
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
                                // just ignore the authorize exception and continue on
                                // without parsing the xml document.
                        }
                
            
            // ////////////////////////////////
            // End elements
            endElement(METS,"xmlData");
            endElement(METS,"mdWrap");
            endElement(METS,"dmdSec");
        }


        // Check to see if there is an in-line MODS document
        // stored as a bitstream. If there is then we should also
        // include these metadata in our METS document. However,
        // we don't really know what the document describes, so we
        // but it in its own dmd group.

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
                        {
                            continue;
                        }
                        
                        
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
                                // just ignore the authorize exception and continue on
                                // without parsing the xml document.
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
     * Render the METS administrative section.
     *
     * Example:
     * <amdSec>
     *  <mdWrap MDTYPE="OTHER" OTHERMDTYPE="METSRights">
     *    <xmlData>
     *      ... content from the crosswalk ...
     *    </xmlDate>
     *  </mdWrap>
     * </amdSec>
     */
    protected void renderAdministrativeSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException
    {
        AttributeMap attributes;
        String groupID;

        // Only create an <amdSec>, if we have amdTypes (or sub-sections) specified...
        // (this keeps our METS file small, by default, and hides all our admin metadata)
        if(amdTypes.size() > 0)
        {
          ////////////////////////////////
          // Start an administrative wrapper

          // Administrative element's ID
          String amdID = getGenericID("amd_");
          attributes = new AttributeMap();
          attributes.put("ID", amdID);
          startElement(METS, "amdSec", attributes);

          groupID = getGenericID("group_amd_");
          attributes.put("GROUPID", groupID);
        }

        // For each administrative metadata section specified
        for (String amdSecName : amdTypes.keySet())
        {
          // get a list of metadata crosswalks which will be used to build
          // this administrative metadata section
          List<String> mdTypes = amdTypes.get(amdSecName);

          // For each crosswalk
          for (String mdType : mdTypes)
          {
            // get our dissemination crosswalk
            DisseminationCrosswalk crosswalk = getDisseminationCrosswalk(mdType);

            // skip, if we cannot find this crosswalk in config file
            if (crosswalk == null)
            {
                continue;
            }

            // First, check if this crosswalk can handle disseminating Item-level Administrative metadata
            if(crosswalk.canDisseminate(item))
            {
              // Since this crosswalk works with items, first render a section for entire item
              renderAmdSubSection(amdSecName, mdType, crosswalk, item);
            }

            // Next, we'll try and render Bitstream-level administrative metadata
            // (Although, we're only rendering this metadata for the bundles specified)
            List<Bundle> bundles = findEnabledBundles();
            for (Bundle bundle : bundles)
            {
              Bitstream[] bitstreams = bundle.getBitstreams();

              // Create a sub-section of <amdSec> for each bitstream in bundle
              for(Bitstream bitstream : bitstreams)
              {
                 // Only render the section if crosswalk works with bitstreams
                 if(crosswalk.canDisseminate(bitstream))
                 {
                    renderAmdSubSection(amdSecName, mdType, crosswalk, bitstream);
                 }
              } // end for each bitstream
            } // end for each bundle
          } // end for each crosswalk
        } // end for each amdSec
        
        if(amdTypes.size() > 0)
        {
          //////////////////////////////////
          // End administrative section
          endElement(METS,"amdSec");
        }
    }

    /**
     * Render a sub-section of the administrative metadata section.
     * Valid sub-sections include: techMD, rightsMD, sourceMD, digiprovMD
     *
     * Example:
     * <techMD>
     *   <mdWrap MDTYPE="PREMIS">
     *     <xmlData>
     *       [PREMIS content ... ]
     *     </xmlData>
     *   </mdWrap>
     * </techMD>
     *
     * @param amdSecName Name of administrative metadata section
     * @param mdType Type of metadata section (e.g. PREMIS)
     * @param crosswalk The DisseminationCrosswalk to use to generate this section
     * @param dso The current DSpace object to use the crosswalk on
     */
    protected void renderAmdSubSection(String amdSecName, String mdType, DisseminationCrosswalk crosswalk, DSpaceObject dso)
            throws WingException, SAXException, CrosswalkException, IOException, SQLException
    {
        /////////////////////////////////
        // Start administrative metadata section wrapper
        String amdSecID = getAmdSecID(amdSecName, mdType, dso);
        AttributeMap attributes = new AttributeMap();
        attributes.put("ID", amdSecID);
        startElement(METS, amdSecName, attributes);

        // If this is a bitstream
        if (dso.getType() == Constants.BITSTREAM)
        {
          // Add this to our list of each file's administrative section IDs
          String fileID = getFileID((Bitstream) dso);
          if(fileAmdSecIDs.containsKey(fileID))
          {
              fileAmdSecIDs.get(fileID).append(" " + amdSecID);
          }
          else
          {
              fileAmdSecIDs.put(fileID, new StringBuffer(amdSecID));
          }
        } // else if an Item
        else if (dso.getType() == Constants.ITEM)
        {
           // Add this to our list of item's administrative section IDs
           if(amdSecIDS==null)
           {
               amdSecIDS = new StringBuffer(amdSecID);
           }
           else
           {
               amdSecIDS.append(" ").append(amdSecID);
           }
        }

        ////////////////////////////////
        // Start a metadata wrapper
        attributes = new AttributeMap();
        if (isDefinedMETStype(mdType))
        {
            attributes.put("MDTYPE", mdType);
        }
        else
        {
            attributes.put("MDTYPE","OTHER");
            attributes.put("OTHERMDTYPE", mdType);
        }
        startElement(METS,"mdWrap",attributes);

        //////////////////////////////////
        // Start the xml data
        startElement(METS,"xmlData");

        /////////////////////////////////
        // Send the actual XML content,
        // using the PREMIS crosswalk for each bitstream
        try {
            Element dissemination = crosswalk.disseminateElement(dso);

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
        endElement(METS,amdSecName);
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
        List<Bundle> bundles = findEnabledBundles();

        // Suppress license?
        Boolean showLicense = ConfigurationManager.getBooleanProperty("webui.licence_bundle.show");
        
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
            if ("LICENSE".equals(bundle.getName()) && ! showLicense)
            {
                continue;
            }

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
                {
                    originalBitstream = findOriginalBitstream(item, bitstream);
                }
                String groupID = getGroupFileID((originalBitstream == null) ? bitstream : originalBitstream );

                // Check if there were administrative metadata sections corresponding to this file
                String admIDs = null;
                if(fileAmdSecIDs.containsKey(fileID))
                {
                    admIDs = fileAmdSecIDs.get(fileID).toString();
                }
  
                // Render the actual file & flocate elements.
                renderFileWithAllowed(item, bitstream, fileID, groupID, admIDs);

                // Remember all the viewable content bitstreams for later in the
                // structMap.
                if (isContentBundle)
                {
                    contentBitstreams.add(bitstream);
                    if (bundle.getPrimaryBitstreamID() == bitstream.getID())
                    {
                        primaryBitstream = bitstream;
                    }
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
     * content bitstreams, those are bitstreams that are typically
     * viewable by the end user.
     *
     * Example:
     * <structMap TYPE="LOGICAL" LABEL="DSpace">
     *   <div TYPE="DSpace Item" DMDID="space-separated list of ids">
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
        {
            attributes.put("DMDID", dmdSecIDS.toString());
        }
        // add references to the Administrative metadata
        if (amdSecIDS != null)
        {
            attributes.put("AMDID", amdSecIDS.toString());
        }
        startElement(METS,"div",attributes);
        
        // add a fptr pointer to the primary bitstream.
        if (primaryBitstream != null)
        {
                // ////////////////////////////////
                // Start & end a reference to the primary bitstream.
                attributes = new AttributeMap();
                String fileID = getFileID(primaryBitstream);
                attributes.put("FILEID", fileID);
                
                startElement(METS,"fptr",attributes);
                endElement(METS,"fptr");
        }

        for (Bitstream bitstream : contentBitstreams)
        {
                // ////////////////////////////////////
                // Start a div for each publicly viewable bitstream
                attributes = new AttributeMap();
                attributes.put("ID", getGenericID("div_"));
                attributes.put("TYPE", "DSpace Content Bitstream");
                startElement(METS,"div",attributes);

                // ////////////////////////////////
                // Start a the actualy pointer to the bitstream FIXME: what?
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
        {
            return;
        }
                
                
        Bundle[] bundles = item.getBundles("METADATA");

        for (Bundle bundle : bundles)
        {
                Bitstream bitstream = bundle.getBitstreamByName("METS.xml");

                if (bitstream == null)
                {
                    continue;
                }

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
                                // just ignore the authorize exception and continue on
                                // without parsing the xml document.
                        }
        }
    }

    
    /**
     * Checks which Bundles of current item a user has requested.
     * If none specifically requested, then all Bundles are returned.
     *
     * @return List of enabled bundles
     */
    protected List<Bundle> findEnabledBundles() throws SQLException
    {
        // Check if the user is requested a specific bundle or
        // the all bundles.
        List<Bundle> bundles;
        if (fileGrpTypes.size() == 0)
        {
            bundles = Arrays.asList(item.getBundles());
        }
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
    
        return bundles;
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
        // FIXME: this method is a copy of the one found below. However, the
        // original method is protected so we can't use it here. I think that
        // perhaps this should be folded into the DSpace bitstream API. Until
        // when a good final solution can be determined I am just going to copy
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
    /**
     * Generate a METS file element for a given bitstream.
     * 
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
     * @param admID
     *            The IDs of the administrative metadata sections which pertain
     *            to this file
     */
    
    // FIXME: this method is a copy of the one inherited. However the
    // original method is final so we must rename it.
	protected void renderFileWithAllowed(Item item, Bitstream bitstream, String fileID, String groupID, String admID) throws SAXException
	{
		AttributeMap attributes;
		
		// //////////////////////////////
    	// Determine the file attributes
        BitstreamFormat format = bitstream.getFormat();
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

        
        // If possible reference this bitstream via a handle, however this may
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
        
        // If we can put the pretty name of the bitstream on the end of the URL
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
            // name on the end of the URL because the sequence id will 
        	// locate it. However it means that links in this file might
        	// not work....
        }
        
        url += "?sequence="+bitstream.getSequenceID();

	// Test if we are allowed to see this item
	String isAllowed = "n";
	try {
	    if (AuthorizeManager.authorizeActionBoolean(context, bitstream, Constants.READ)) {
		isAllowed = "y";
	    }
	} catch (SQLException e) {/* Do nothing */}
	
	url += "&isAllowed=" + isAllowed;

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
}
