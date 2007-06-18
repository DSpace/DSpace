/*
 * PluginCrosswalk.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
 * @version $Revision$
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
                return outputUgly.outputString(xwalk.disseminateList(item));
            else
                return outputUgly.outputString(xwalk.disseminateElement(item));
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

            throw new CannotDisseminateFormatException(schemaLabel);
        }
    }
}
