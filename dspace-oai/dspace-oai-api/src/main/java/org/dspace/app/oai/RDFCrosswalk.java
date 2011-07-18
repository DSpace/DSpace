/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.oai;

import java.util.Properties;
import java.sql.SQLException;

import org.dspace.app.util.Util;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.dspace.core.ConfigurationManager;
import org.dspace.search.HarvestedItemInfo;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

/**
 * An OAICat Crosswalk implementation that extracts 
 * DSpace items into typed RDF format.
 * 
 * @author Richard Rodgers
 * @version $Revision: 5845 $
 */
public class RDFCrosswalk extends Crosswalk
{
	// base URL for thumbnails
	private String baseUrl = null;
	// hostname for rdf URI
	private String hostName = null;
	
    public RDFCrosswalk(Properties properties)
    {
        super(
        "http://www.openarchives.org/OAI/2.0/rdf/ http://www.openarchives.org/OAI/2.0/rdf.xsd");
        baseUrl = ConfigurationManager.getProperty("dspace.url");
        hostName = ConfigurationManager.getProperty("dspace.hostname");
    }

    public boolean isAvailableFor(Object nativeItem)
    {
        // Only implemented for items so far
        return (nativeItem instanceof HarvestedItemInfo);
    }

    public String createMetadata(Object nativeItem)
            throws CannotDisseminateFormatException
    {
    	HarvestedItemInfo itemInfo = (HarvestedItemInfo)nativeItem;
        Item item = itemInfo.item;

        // Get all the DC
        DCValue[] allDC = item.getDC(Item.ANY, Item.ANY, Item.ANY);

        StringBuffer metadata = new StringBuffer();

        /*
        metadata
                .append(
                        "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" ")
                .append("xmlns:dc=\"http://purl.org/dc/elements/1.1/\" ")
                .append(
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append(
                        "xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">");
        */
        
        metadata
                .append(
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" " )
                .append("xmlns:ow=\"http://www.ontoweb.org/ontology/1#\" " )
                .append("xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " )
                .append("xmlns:ds=\"http://dspace.org/ds/elements/1.1/\" " )
                .append(
                "xsi:schemaLocation=\"http://www.w3.org/1999/02/22-rdf-syntax-ns# http://www.openarchives.org/OAI/2.0/rdf.xsd\">");

        
        // construct URI for item
        metadata.append("<ow:Publication rdf:about=\"oai:")
                .append(hostName)
                .append(":")
                .append(item.getHandle())
                .append("\">");

        for (int i = 0; i < allDC.length; i++)
        {
            if (screened(allDC[i]))
            {
                String element = allDC[i].element;

                // contributor.author exposed as 'creator'
                if (allDC[i].element.equals("contributor")
                        && (allDC[i].qualifier != null)
                        && allDC[i].qualifier.equals("author"))
                {
                    element = "creator";
                }

                // Escape XML chars <, > and &
                String value = allDC[i].value;

                // Check for null values
                if (value == null)
                {
                    value = "";
                }

                // First do &'s - need to be careful not to replace the
                // & in "&amp;" again!
                int c = -1;

                while ((c = value.indexOf("&", c + 1)) > -1)
                {
                    value = value.substring(0, c) + "&amp;"
                            + value.substring(c + 1);
                }

                while ((c = value.indexOf("<")) > -1)
                {
                    value = value.substring(0, c) + "&lt;"
                            + value.substring(c + 1);
                }

                while ((c = value.indexOf(">")) > -1)
                {
                    value = value.substring(0, c) + "&gt;"
                            + value.substring(c + 1);
                }

                metadata.append("<dc:").append(element).append(">")
                        .append(value)
                        .append("</dc:").append(element).append(">");
            }
        }
        
        // add extended info - collection, communities, and thumbnail URLs
        Collection[] colls = null;
        Community[] comms = null;
        Bundle[] origBundles = null;
        Bundle[] thumbBundles = null;
        try
        {
        	colls = item.getCollections();
        	comms = item.getCommunities();
        	origBundles = item.getBundles("ORIGINAL");
        	thumbBundles = item.getBundles("THUMBNAIL");
        }
        catch(SQLException sqlE)
        {
        }
        
        // all parent communities map to DC source
        for (int i = 0; i < comms.length; i++)
        {
        	metadata.append("<dc:source>")
        	        .append(comms[i].getMetadata("name"))
        	        .append("</dc:source>");
        }
        // as do collections
        for (int j = 0; j < colls.length; j++)
        {
        	metadata.append("<dc:source>")
        	        .append(colls[j].getMetadata("name"))
        	        .append("</dc:source>");
        }
        
        if (origBundles.length > 0)
        {
        	Bitstream[] bitstreams = origBundles[0].getBitstreams();
        	// add a URL for each original that has a thumbnail
        	for (int j = 0; j < bitstreams.length; j++)
        	{
        		String tName = bitstreams[j].getName() + ".jpg";
				Bitstream tb = null;

                if (thumbBundles.length > 0)
                {
                    tb = thumbBundles[0].getBitstreamByName(tName);
                }

				if (tb != null)
				{
					String thumbUrl = null;
					try
					{
						thumbUrl = baseUrl + "/retrieve/" + tb.getID() + "/" +
								   Util.encodeBitstreamName(tb.getName(),
                				   Constants.DEFAULT_ENCODING);
					}
					catch(Exception e)
					{
					}
					metadata.append("<dc:coverage>")
					        .append(thumbUrl)
					        .append("</dc:coverage>");
				}
        	}
        }
        //metadata.append("</oai_ds:ds>");
        metadata.append("</ow:Publication>");
        metadata.append("</rdf:RDF>");

        return metadata.toString();
    }
    
    /*
     * Exclude Item DC elements unsuitable for harvest
     */
    private boolean screened(DCValue dcValue)
    {
    	// description.providence
        if (isQualified(dcValue, "description", "provenance"))
        {
        	return false;
        }
        // format.extent
        if (isQualified(dcValue, "format", "extent"))
        {
           	return false;
        }
        // date.available is algorithmically identical to date.accessioned
        // suppress one
        if (isQualified(dcValue, "date", "accessioned"))
        {
           	return false;
        }
    	return true;
    }
    
    private boolean isQualified(DCValue dcValue, String elName, String qualName)
    {
        return (dcValue.element.equals(elName) &&
        		dcValue.qualifier != null &&
                dcValue.qualifier.equals(qualName));
    }
}