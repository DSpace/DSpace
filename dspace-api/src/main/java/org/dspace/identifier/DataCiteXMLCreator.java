/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.PluginManager;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 * Provide XML based metadata crosswalk for EZID Identifier provider module.
 * 
 * @author mohideen
 */

public class DataCiteXMLCreator
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DataCiteXMLCreator.class);

    /**
     * Name of crosswalk to convert metadata into DataCite Metadata Scheme.
     */
    protected String CROSSWALK_NAME = "DataCite";

    /**
     * DisseminationCrosswalk to map local metadata into DataCite metadata. The
     * name of the crosswalk is set by {@link setDisseminationCrosswalk(String)
     * setDisseminationCrosswalk} which instantiates the crosswalk.
     */
    protected DisseminationCrosswalk xwalk;

    public String getXMLString(DSpaceObject dso)
    {
        if (dso == null)
        {
            log.info("Invalid object: " + dso);
            return null;
        }

        this.prepareXwalk();

        if (!this.xwalk.canDisseminate(dso))
        {
            log.error("Crosswalk " + this.CROSSWALK_NAME
                    + " cannot disseminate DSO with type " + dso.getType()
                    + " and ID " + dso.getID() + ".");
            return null;
        }

        Element root = null;
        try
        {
            root = xwalk.disseminateElement(dso);
        }
        catch (Exception e)
        {
            log.error(
                    "Exception while crosswolking DSO " + "with type "
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

        this.xwalk = (DisseminationCrosswalk) PluginManager.getNamedPlugin(
                DisseminationCrosswalk.class, this.CROSSWALK_NAME);

        if (this.xwalk == null)
        {
            throw new RuntimeException("Can't find crosswalk '"
                    + CROSSWALK_NAME + "'!");
        }
    }

}
