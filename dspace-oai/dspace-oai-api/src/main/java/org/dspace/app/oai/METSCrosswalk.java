/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.oai;

import java.util.Properties;

import org.apache.log4j.Logger;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.PluginManager;
import org.dspace.search.HarvestedItemInfo;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * OAICat crosswalk to allow METS to be harvested.
 * 
 * No security or privacy measures in place.
 * 
 * @author Li XiaoYu (Rita)
 * @author Robert Tansley
 * @author Tim Donohue (rewrite to use METS DisseminationCrosswalk)
 */
public class METSCrosswalk extends Crosswalk
{
    private static final Logger log = Logger.getLogger(METSCrosswalk.class);
    
    // JDOM xml output writer - indented format for readability.
    private static XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
    
    public METSCrosswalk(Properties properties)
    {
        super(
                "http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd");
    }

    @Override
    public boolean isAvailableFor(Object nativeItem)
    {
        // We have METS for everything
        return true;
    }

    @Override
    public String createMetadata(Object nativeItem)
            throws CannotDisseminateFormatException
    {
        HarvestedItemInfo hii = (HarvestedItemInfo) nativeItem;

        try
        {
            //Get a reference to our DSpace METS DisseminationCrosswalk
            // (likely this is org.dspace.content.crosswalk.METSDisseminationCrosswalk)
            DisseminationCrosswalk xwalk = (DisseminationCrosswalk)PluginManager.
                                        getNamedPlugin(DisseminationCrosswalk.class, "METS");
            
            //if no crosswalk found, thrown an error
            if(xwalk==null)
                throw new CannotDisseminateFormatException("DSpace cannot disseminate METS format, as no DisseminationCrosswalk is configured which supports 'METS'");
            
            if(xwalk.canDisseminate(hii.item))
            {    
                //disseminate the object to METS
                Element rootElement = xwalk.disseminateElement(hii.item); 
                
                //Return XML results as a formatted String
                return outputter.outputString(rootElement);
            }
            else
                return null; // cannot disseminate this type of object
        }
        catch (Exception e)
        {
            log.error("OAI-PMH METSCrosswalk error", e);
            return null;
        }

    }
}
