/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * "Null" ingestion crosswalk
 * <p>
 * Use this crosswalk to ignore a metadata record on ingest.  It was
 * intended to be used with a package importer such as the METS
 * packager, which may receive metadata records of types for which it
 * hasn't got a crosswalk.  The safest thing to do with these is ignore
 * them.  To do that, use the plugin configuration to map the name
 * of the metadata type to this plugin  (or within the METS ingester,
 * use its metadata-name remapping configuration).
 * <pre>
 * # ignore LOM metadata when it comes up:
 * plugin.named.org.dspace.content.crosswalk.SubmissionCrosswalk = \
 *   org.dspace.content.crosswalk.NullIngestionCrosswalk = NULL, LOM
 * </pre>
 * @author Larry Stone
 * @version $Revision$
 */
public class NullIngestionCrosswalk
    implements IngestionCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(NullIngestionCrosswalk.class);

    private static XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());

    @Override
    public void ingest(Context context, DSpaceObject dso, Element root, boolean createMissingMetadataFields)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        // drop xml on the floor but mention what we're missing for debugging:
        log.debug("Null crosswalk is ignoring this metadata Element: \n"+
                outputPretty.outputString(root));
    }

    @Override
    public void ingest(Context context, DSpaceObject dso, List<Element> ml, boolean createMissingMetadataFields)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        // drop xml on the floor but mention what we're missing for debugging:
        log.debug("Null crosswalk is ignoring this List of metadata: \n"+
                outputPretty.outputString(ml));
    }

    public boolean preferList()
    {
        return false;
    }
}
