/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.OutputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.PackageUtils;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * Export the object's DSpace deposit license.
 *
 * @author  Larry Stone
 * @version $Revision: 1.0 $
 */
public class LicenseStreamDisseminationCrosswalk
    implements StreamDisseminationCrosswalk
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(LicenseStreamDisseminationCrosswalk.class);
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso)
    {
        try
        {
            return dso.getType() == Constants.ITEM &&
                   PackageUtils.findDepositLicense(context, (Item)dso) != null;
        }
        catch (Exception e)
        {
            log.error("Failed getting Deposit license", e);
            return  false;
        }
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        if (dso.getType() == Constants.ITEM)
        {
            Bitstream licenseBs = PackageUtils.findDepositLicense(context, (Item)dso);
             
            if (licenseBs != null)
            {
                Utils.copy(bitstreamService.retrieve(context, licenseBs), out);
                out.close();
            }
        }
    }

    @Override
    public String getMIMEType()
    {
        return "text/plain";
    }
}
