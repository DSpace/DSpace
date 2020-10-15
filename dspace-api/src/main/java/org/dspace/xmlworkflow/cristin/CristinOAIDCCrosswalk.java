package org.dspace.xmlworkflow.cristin;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.core.Context;
import org.jdom.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 *  A crosswalk to be used with the CRISTIN oai_dc document.  This basically
 *  does nothing, as all the CRISTIN work is done by the ORE document ingester
 */
public class CristinOAIDCCrosswalk implements IngestionCrosswalk
{
    /**
     * Ingest the Dublin Core metadata from the OAI-PMH harvest.
     *
     * This does nothing as the metadata for Cristin object comes from one of the
     * related bitstreams
     *
     * @param context
     * @param dSpaceObject
     * @param elements
     * @throws CrosswalkException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void ingest(Context context, DSpaceObject dSpaceObject, List<Element> elements, boolean createMissingMetadataFields)
            throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        // does nothing, the CRISTIN items are not populated by DC
        return;
    }

    /**
     * Ingest the Dublin Core metadata from the OAI-PMH harvest.
     *
     * This does nothing as the metadata for Cristin object comes from one of the
     * related bitstreams
     *
     * @param context
     * @param dSpaceObject
     * @param element
     * @throws CrosswalkException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void ingest(Context context, DSpaceObject dSpaceObject, Element element, boolean createMissingMetadataFields)
            throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        // does nothing, the CRISTIN items are not populated by DC
        return;
    }
}
