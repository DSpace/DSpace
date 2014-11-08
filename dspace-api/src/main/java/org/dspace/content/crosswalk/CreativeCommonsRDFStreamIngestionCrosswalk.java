/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.license.factory.LicenseServiceFactory;
import org.dspace.license.service.CreativeCommonsService;

/**
 * Ingest a Creative Commons license, RDF form.
 * <p>
 * Note that this is NOT needed when ingesting a DSpace AIP, since the
 * CC license is stored as a Bitstream (or two) in a dedicated Bundle;
 * the normal apparatus of ingestig the AIP will restore that Bitstream
 * with its proper name and thus the presence of the CC license.
 * <p>
 * This crosswalk should only be used when ingesting other kinds of SIPs.
 *
 * @author  Larry Stone
 * @version $Revision: 1.0 $
 */
public class CreativeCommonsRDFStreamIngestionCrosswalk
    implements StreamIngestionCrosswalk
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(CreativeCommonsRDFStreamIngestionCrosswalk.class);

    protected CreativeCommonsService creativeCommonsService = LicenseServiceFactory.getInstance().getCreativeCommonsService();


    @Override
    public void ingest(Context context, DSpaceObject dso, InputStream in, String MIMEType)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        // If package includes a Creative Commons license, add that:
        if (dso.getType() == Constants.ITEM)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Reading a Creative Commons license, MIMEtype=" + MIMEType);
            }

            creativeCommonsService.setLicense(context, (Item) dso, in, MIMEType);
        }
    }


    public String getMIMEType()
    {
        return "text/rdf";
    }
}
