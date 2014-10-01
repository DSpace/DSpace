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
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorProfileAdapter extends AbstractAdapter {
    //, Item item,String contextPath
   private Context context;

    private List<AuthorProfileMetadataFilter> authorProfileMetadataFilters = (new DSpace()).getServiceManager().getServicesByType(AuthorProfileMetadataFilter.class);

    private AuthorProfile authorProfile;

    public AuthorProfileAdapter(Context context, AuthorProfile authorProfile, String contextPath) {
        super(contextPath);
        this.context = context;
        this.authorProfile = authorProfile;
    }
    private StringBuffer dmdSecIDS;
    @Override
    protected void renderDescriptiveSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException {
       DCValue val[]=authorProfile.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

       AttributeMap attributes;
        String groupID=getGenericID("group_dmd_");
        dmdSecIDS = new StringBuffer();
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
            attributes.put("dspaceType", Constants.typeText[authorProfile.getType()]);
            startElement(DIM,"dim",attributes);
            for (DCValue dcv : val)
            {
                boolean show = true;
                for(AuthorProfileMetadataFilter filter:authorProfileMetadataFilters){
                    show=filter.includeMetadatum(context,authorProfile,dcv.element,dcv.qualifier);
                }
                if (!MetadataExposure.isHidden(context, dcv.schema, dcv.element, dcv.qualifier)&&show)
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
            endElement(METS, "dmdSec");
        }


    }

    @Override
    protected void renderFileSection() throws WingException, SAXException, CrosswalkException, IOException, SQLException {
      AttributeMap attributes;
        startElement(METS,"fileSec");
      attributes = new AttributeMap();
        startElement(METS,"fileGrp",attributes);


        Bitstream bitstream = authorProfile.getAuthorProfilePicture();
        if(bitstream!=null){
        String fileID = getFileID(bitstream);


        // Render the actual file & flocate elements.
        renderFile(authorProfile, bitstream, fileID);

        // Remember all the viewable content bitstreams for later in the
        // structMap.

        }
        endElement(METS,"fileGrp");

        endElement(METS,"fileSec");

    }

    protected String getFileID(Bitstream bitstream)
    {
        return "file_" + bitstream.getID();
    }

    @Override
    protected void renderStructureMap() throws WingException, SAXException, CrosswalkException, IOException, SQLException {
        startElement(METS,"structMap");
        AttributeMap map=new AttributeMap();
        map.put("id",authorProfile.getID());
            startElement(METS,"div",map);
            endElement(METS,"div");
        endElement(METS,"structMap");
    }


    protected final void renderFile(AuthorProfile item, Bitstream bitstream, String fileID) throws SAXException
    {
        AttributeMap attributes;

        // //////////////////////////////
        // Determine the file attributes
        BitstreamFormat format = bitstream.getFormat();
        String mimeType = null;
        String kind = null;
        if (format != null)
        {
            mimeType = format.getMIMEType();
            kind = format.getDescription();
        }
        String checksumType = bitstream.getChecksumAlgorithm();
        String checksum = bitstream.getChecksum();
        long size = bitstream.getSize();

        // ////////////////////////////////
        // Start the actual file
        attributes = new AttributeMap();
        attributes.put("ID", fileID);
        //attributes.put("GROUPID",groupID);
//        if (admID != null && admID.length()>0)
//        {
//            attributes.put("ADMID", admID);
//        }
        if (mimeType != null && mimeType.length()>0)
        {
            attributes.put("MIMETYPE", mimeType);
        }
        if (kind != null){
            attributes.put("KIND", kind);
        }
        if (checksumType != null && checksum != null && !checksumType.equals(""))
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



    @Override
    protected String getMETSOBJID() throws WingException {
        if (contextPath == null)
        {
            return "/internal/author/"+authorProfile.getID();
        }
        else
        {
            return contextPath + "/internal/author/"+authorProfile.getID();
        }


    }

    @Override
    protected String getMETSOBJEDIT() {
        return null;
    }

    @Override
    protected String getMETSID() throws WingException {
        return null;
    }

    @Override
    protected String getMETSProfile() throws WingException {
        return null;
    }

    @Override
    protected String getMETSLabel() throws WingException {
        return "DSpace author profile";
    }
}
