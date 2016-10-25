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
import org.dspace.content.Item;
import org.dspace.content.Bitstream;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.license.CreativeCommons;

/**
 * Export the object's Creative Commons license, text form.
 *
 * @author  Larry Stone
 * @version $Revision: 1.0 $
 * 
 * @deprecated to make uniforme JSPUI and XMLUI approach the bitstream with the license in the textual format it is no longer stored see https://jira.duraspace.org/browse/DS-2604 
 */
@Deprecated
public class CreativeCommonsTextStreamDisseminationCrosswalk
    implements StreamDisseminationCrosswalk
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(CreativeCommonsTextStreamDisseminationCrosswalk.class);

    public boolean canDisseminate(Context context, DSpaceObject dso)
    {
        try
        {
            return dso.getType() == Constants.ITEM &&
                   CreativeCommons.getLicenseTextBitstream((Item)dso) != null;
        }
        catch (Exception e)
        {
            log.error("Failed getting CC license", e);
            return  false;
        }
    }

    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        if (dso.getType() == Constants.ITEM)
        {
            Bitstream cc = CreativeCommons.getLicenseTextBitstream((Item)dso);
            if (cc != null)
            {
                Utils.copy(cc.retrieve(), out);
                out.close();
            }
        }
    }

    public String getMIMEType()
    {
        return "text/plain";
    }
}
