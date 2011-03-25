/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.oai;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import org.dspace.app.mets.METSExport;
import org.dspace.search.HarvestedItemInfo;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

/**
 * OAICat crosswalk to allow METS to be harvested.
 * 
 * No security or privacy measures in place.
 * 
 * @author Li XiaoYu (Rita)
 * @author Robert Tansley
 */
public class METSCrosswalk extends Crosswalk
{
    public METSCrosswalk(Properties properties)
    {
        super(
                "http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd");
    }

    public boolean isAvailableFor(Object nativeItem)
    {
        // We have METS for everything
        return true;
    }

    public String createMetadata(Object nativeItem)
            throws CannotDisseminateFormatException
    {
        HarvestedItemInfo hii = (HarvestedItemInfo) nativeItem;

        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            METSExport.writeMETS(hii.context, hii.item, baos, true);

            // FIXME: Nasty hack to remove <?xml...?> header that METS toolkit
            // puts there.  Hopefully the METS toolkit itself can be updated
            // to fix this
            String fullXML = baos.toString("UTF-8");
            String head = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n";
            int pos = fullXML.indexOf(head);
            if (pos != -1)
            {
                fullXML = fullXML.substring(pos + head.length());
            }
            
            return fullXML;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }
}
