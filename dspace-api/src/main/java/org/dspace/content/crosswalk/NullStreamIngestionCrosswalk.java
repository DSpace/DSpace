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

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * A crosswalk to ignore and dispose of the ingested material.
 * <p>
 * Specify this crosswalk in the mapping of e.g. METS metadata field
 * types to crosswalks when you wish to ignore a redundant or unknown
 * type of metadata.  For example, when ingesting a DSpace AIP with an
 * AIP ingester, it is best to ignore the rightsMD fields since they
 * are already going to be ingested as member bitstreams anyway.
 *
 * @author  Larry Stone
 * @version $Revision: 1.0 $
 */
public class NullStreamIngestionCrosswalk
    implements StreamIngestionCrosswalk
{
    @Override
    public void ingest(Context context, DSpaceObject dso, InputStream in, String MIMEType)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        in.close();
    }

    public String getMIMEType()
    {
        return "text/plain";
    }
}
