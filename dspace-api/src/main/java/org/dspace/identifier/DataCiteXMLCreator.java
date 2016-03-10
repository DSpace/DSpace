/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.crosswalk.ParameterizedDisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide XML based metadata crosswalk for EZID Identifier provider module.
 *
 * @author mohideen
 */

public class DataCiteXMLCreator
{
    /** log4j category */
    private static final Logger LOG = LoggerFactory.getLogger(DataCiteXMLCreator.class);

    /**
     * Name of crosswalk to convert metadata into DataCite Metadata Scheme.
     */
    protected String CROSSWALK_NAME = "DataCite";

    /**
     * DisseminationCrosswalk to map local metadata into DataCite metadata. The
     * name of the crosswalk is set by {@link setDisseminationCrosswalk(String)
     * setDisseminationCrosswalk} which instantiates the crosswalk.
     */
    protected ParameterizedDisseminationCrosswalk xwalk;

    public String getXMLString(Context context, DSpaceObject dso)
    {
        if (dso == null)
        {
            LOG.info("Invalid object: {}", dso);
            return null;
        }

        this.prepareXwalk();

        if (!this.xwalk.canDisseminate(dso))
        {
            LOG.error("Crosswalk " + this.CROSSWALK_NAME
                    + " cannot disseminate DSO with type " + dso.getType()
                    + " and ID " + dso.getID() + ".");
            return null;
        }

        // Set the transform's parameters.
        // XXX Should the actual list be configurable?
        ConfigurationService cfg = new DSpace().getConfigurationService();
        Map<String, String> parameters = new HashMap<>();
        if (null != cfg.getProperty("identifier.doi.prefix"))
            parameters.put("prefix", cfg.getProperty("identifier.doi.prefix"));
        if (null != cfg.getProperty("crosswalk.dissemination.DataCite.publisher"))
            parameters.put("publisher", cfg.getProperty("crosswalk.dissemination.DataCite.publisher"));
        if (null != cfg.getProperty("crosswalk.dissemination.DataCite.dataManager"))
            parameters.put("datamanager", cfg.getProperty("crosswalk.dissemination.DataCite.dataManager"));
        if (null != cfg.getProperty("crosswalk.dissemination.DataCite.hostingInstitution"))
            parameters.put("hostinginstitution", cfg.getProperty("crosswalk.dissemination.DataCite.hostingInstitution"));

        // Transform the metadata
        Element root;
        try
        {
            root = xwalk.disseminateElement(context, dso, parameters);
        }
        catch (CrosswalkException | IOException | SQLException | AuthorizeException e)
        {
            LOG.error("Exception while crosswalking DSO with type "
                            + dso.getType() + " and ID " + dso.getID() + ".", e);
            return null;
        }

        XMLOutputter xOut = new XMLOutputter();

        return xOut.outputString(root);
    }

    /**
     * Set the name of the dissemination crosswalk used to convert the metadata
     * into DataCite Metadata Schema. Used by spring dependency injection.
     *
     * @param CROSSWALK_NAME
     *            The name of the dissemination crosswalk to use.
     */
    public void setDisseminationCrosswalkName(String CROSSWALK_NAME)
    {
        this.CROSSWALK_NAME = CROSSWALK_NAME;
    }

    protected void prepareXwalk()
    {
        if (null != this.xwalk)
            return;

        this.xwalk = (ParameterizedDisseminationCrosswalk) CoreServiceFactory
                .getInstance().getPluginService()
                .getNamedPlugin(DisseminationCrosswalk.class, this.CROSSWALK_NAME);

        if (this.xwalk == null)
        {
            throw new RuntimeException("Can't find crosswalk '"
                    + CROSSWALK_NAME + "'!");
        }
    }

    /**
     * Test the operation of this class.
     *
     * @param argv <br>[0]: handle of an object for which to prepare XML metadata.
     * @throws Exception
     */
    public static void main(String[] argv)
        throws Exception
    {
        String handle = argv[0];
        DataCiteXMLCreator instance = new DataCiteXMLCreator();
        Context context = new Context();
        DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService()
                .resolveToObject(context, handle);

        System.out.println(instance.getXMLString(context, dso));
    }
}
