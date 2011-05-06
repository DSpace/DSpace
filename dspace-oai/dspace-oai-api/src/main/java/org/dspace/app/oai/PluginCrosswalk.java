/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.oai;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.PluginManager;
import org.dspace.search.HarvestedItemInfo;
import org.jdom.output.XMLOutputter;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

/**
 * An OAICat Crosswalk implementation that calls, in turn, on
 * <code>DisseminationCrosswalk</code> plugins.
 * It is configured so its "OAI schema label" matches the name of a
 * <code>DisseminationCrosswalk</code> plugin.  This class
 * will then recognize its name and invoke the correct crosswalk
 * to produce the results it sends out.
 * <p>
 * <b>Configuration:</b>
 * In the OAICat configuration file (e.g. <code>oaicat.properties</code>
 * add a line like this for each plugin crosswalk you wish to provide:
 * <pre>
 *   Crosswalks.<cite>Plugin-Name</cite>=org.dspace.app.oai.PluginCrosswalk
 *
 *    e.g.
 *
 *   Crosswalks.DC=org.dspace.app.oai.PluginCrosswalk
 * </pre>
 * This creates an OAI metadata prefix "DC" which is implemented
 * by the dissemination crosswalk plugin that answers to the name "DC".
 * It, in turn, could be found in the DSpace configuration in a line like:
 * <pre>
 *  plugin.named.org.dspace.content.crosswalk.DisseminationCrosswalk = \
 *    org.dspace.content.crosswalk.SimpleDCDisseminationCrosswalk = DC
 * </pre>
 *
 * <p>
 * Note that all OAI crosswalks are instances of this same class, since
 * the instance gets bound to a specific <code>DisseminationCrosswalk</code>
 * when it is created.
 * <p>
 *  WARNING: This requires at the OAICAT java library version 1.5.38.
 *    It does NOT work with some older versions.
 *
 * @author Larry Stone
 * @version $Revision: 5845 $
 */
public class PluginCrosswalk extends Crosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(PluginCrosswalk.class);

    private DisseminationCrosswalk xwalk = null;

    // preserve the label from config property for diagnostics.
    private String schemaLabel = null;

    private static XMLOutputter outputUgly = new XMLOutputter();

    /**
     * Prepare a "schema location" string for the oaicat Crosswalk
     * class's initialization.  This is a string consisting of the
     * namespace URI, a space, and the schema URL, for the XML
     * element to be included in the OAI report.  This is not documented
     * in oaicat's manuals so we mention it here.
     *
     * Since this gets called by the constructor, we can't initialize the
     * xwalk field so the plugin gets thrown away.
     */
    private static String makeSchemaLocation(String schemaLabel)
    {
        DisseminationCrosswalk xwalk = (DisseminationCrosswalk)
                    PluginManager.getNamedPlugin(DisseminationCrosswalk.class,
                                                 schemaLabel);
        if (xwalk != null)
        {
            String schemaLoc = xwalk.getSchemaLocation();

            // initialize the oaicat Crosswalk with a "schemalocation" string,
            // which is "{namespace-URI} {schema-URL}"  (space separated)
            if (schemaLoc != null)
            {
                log.debug("Initialized schemaLabel="+schemaLabel+" with schemaLocation = \""+schemaLoc+"\"");
                return schemaLoc;
            }
            log.error("makeSchemaLocation:  crosswalk cannot provide schemaLocation, label="+schemaLabel);
            return "Error No-schemaLocation-for-"+schemaLabel;
        }
        log.error("No crosswalk found, makeSchemaLocation giving up, label="+schemaLabel);
        return "Error No-crosswalk-for-"+schemaLabel;
    }

    /**
     * Constructor; called by
     * ORG.oclc.oai.server.crosswalk.Crosswalks, which tries first with
     * args (String schemaLabel, Properties properties).  This is
     * convenient since it lets us use that label to initialize this
     * instance of the plugin with the DisseminationCrosswalk crosswalk
     * corresponding to that schemaLabel, instead of creating a subclass
     * for each one.
     * <p>
     *  WARNING: This requires at the OAICAT java library version 1.5.37.
     *    It does NOT work with some older versions.
     */
    public PluginCrosswalk(String schemaLabel, Properties properties)
    {
        super(makeSchemaLocation(schemaLabel));
        xwalk = (DisseminationCrosswalk)PluginManager.getNamedPlugin(
                    DisseminationCrosswalk.class, schemaLabel);
        this.schemaLabel = schemaLabel;
    }

    /**
     * @return true if this dissemination is available for the item.
     */
    public boolean isAvailableFor(Object nativeItem)
    {
        Item item = ((HarvestedItemInfo) nativeItem).item;
        return xwalk.canDisseminate(item);
    }

    /**
     * Do the crosswalk.  Returns serialized XML in a string.
     */
    public String createMetadata(Object nativeItem)
            throws CannotDisseminateFormatException
    {
        Item item = ((HarvestedItemInfo) nativeItem).item;
        try
        {
            log.debug("OAI plugin, schema="+schemaLabel+", preferList="+String.valueOf(xwalk.preferList()));
            if (xwalk.preferList())
            {
                return outputUgly.outputString(xwalk.disseminateList(item));
            }
            else
            {
                return outputUgly.outputString(xwalk.disseminateElement(item));
            }
        }
        catch (Exception e)
        {
            log.error(this.getClass().getName()+
                    ": hiding exception in CannotDisseminateFormatException:"+
                    e.toString());

                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                log.error("*** Stack trace follows:");
                log.error(sw.toString());

            // Stack loss as exception does not support cause
            throw new CannotDisseminateFormatException(schemaLabel);
        }
    }
}
