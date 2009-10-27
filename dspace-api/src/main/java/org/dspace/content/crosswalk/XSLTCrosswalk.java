/*
 * XSLTCrosswalk.java
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

package org.dspace.content.crosswalk;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.SelfNamedPlugin;
import org.jdom.Namespace;
import org.jdom.transform.XSLTransformException;
import org.jdom.transform.XSLTransformer;

/**
 * Configurable XSLT-driven Crosswalk
 * <p>
 * This is the superclass of the XSLT dissemination and submission crosswalks.
 * These classes let you can create many different crosswalks between
 * DSpace internal data and any XML without changing any code, just
 * XSL transformation (XSLT) stylesheets.
 * Each configured stylesheet appears as a new plugin name, although they all
 * share the same plugin implementation class.
 * <p>
 * The XML transformation must produce (for submission) or expect (for
 * dissemination) a document in DIM - DSpace Intermediate Metadata format.
 * See <a href="http://wiki.dspace.org/DspaceIntermediateMetadata">
 * http://wiki.dspace.org/DspaceIntermediateMetadata</a> for details.
 * <h3>Configuration</h3>
 * Prepare your DSpace configuration as follows:
 * <p>
 * A  submission crosswalk is described by a
 * configuration key like
 * <pre>  crosswalk.submission.<i>PluginName</i>.stylesheet = <i>path</i></pre>
 * The <em>alias</em> names the Plugin name,
 * and the <em>path</em> value is the pathname (relative to <code><em>dspace.dir</em>/config</code>)
 * of the crosswalk stylesheet, e.g. <code>"mycrosswalk.xslt"</code>
 * <p>
 * For example, this configures a crosswalk named "LOM" using a stylesheet
 * in <code>config/crosswalks/d-lom.xsl</code> under the DSpace "home" directory:
 * <pre>  crosswalk.submission.stylesheet.LOM = crosswalks/d-lom.xsl</pre>
 * <p>
 * A  dissemination crosswalk is described by a
 * configuration key like
 * <pre>  crosswalk.dissemination.<i>PluginName</i>.stylesheet = <i>path</i></pre>
   The <em>alias</em> names the Plugin name,
 * and the <em>path</em> value is the pathname (relative to <code><em>dspace.dir</em>/config</code>)
 * of the crosswalk stylesheet, e.g. <code>"mycrosswalk.xslt"</code>
 * <p>
 * You can have two names point to the same crosswalk,
 * just add two configuration entries with the same path, e.g.
 * <pre>
 *    crosswalk.submission.MyFormat.stylesheet = crosswalks/myformat.xslt
 *    crosswalk.submission.almost_DC.stylesheet = crosswalks/myformat.xslt
 * </pre>
 * <p>
 * NOTE: This plugin will automatically reload any XSL stylesheet that
 * was modified since it was last loaded.  This lets you edit and test
 * stylesheets without restarting DSpace.
 * <p>
 * You must use the <code>PluginManager</code> to instantiate an
 * XSLT crosswalk plugin, e.g.
 * <pre> IngestionCrosswalk xwalk = PluginManager.getPlugin(IngestionCrosswalk.class, "LOM");</pre>
 * <p>
 * Since there is significant overhead in reading the properties file to
 * configure the crosswalk, and a crosswalk instance may be used any number
 * of times, we recommend caching one instance of the crosswalk for each
 * alias and simply reusing those instances.  The <code>PluginManager</code>
 * does this automatically.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public abstract class XSLTCrosswalk extends SelfNamedPlugin
{
    /** log4j category */
    private static Logger log = Logger.getLogger(XSLTCrosswalk.class);

    /**
     * DSpace XML Namespace in JDOM form.
     */
    public static final Namespace DIM_NS =
        Namespace.getNamespace("dim", "http://www.dspace.org/xmlns/dspace/dim");

    /** Prefix for all lines in the config file for XSLT plugins. */
    protected final static String CONFIG_PREFIX = "crosswalk.";

    private final static String CONFIG_STYLESHEET = ".stylesheet";

    /**
     * Derive list of plugin name from DSpace configuration entries
     * for crosswalks. The <em>direction</em> parameter should be either
     * "dissemination" or "submission", so it looks for keys like
     * <code>crosswalk.submission.{NAME}.stylesheet</code>
     */
    protected static String[] makeAliases(String direction)
    {
        String prefix = CONFIG_PREFIX+direction+".";
        String suffix = CONFIG_STYLESHEET;

        List aliasList = new ArrayList();
        Enumeration pe = ConfigurationManager.propertyNames();

        log.debug("XSLTCrosswalk: Looking for config prefix = "+prefix);
        while (pe.hasMoreElements())
        {
            String key = (String)pe.nextElement();
            if (key.startsWith(prefix) && key.endsWith(suffix))
            {
                log.debug("Getting XSLT plugin name from config line: "+key);
                aliasList.add(key.substring(prefix.length(), key.length()-suffix.length()));
            }
        }
        return (String[])aliasList.toArray(new String[aliasList.size()]);
    }

    private XSLTransformer transformer = null;
    private File transformerFile = null;
    private long transformerLastModified = 0;

    /**
     * Initialize the Transformation stylesheet from configured stylesheet file.
     * @param prefix the direction of xwalk, either "submission" or
     *    "dissemination"
     * @return transformer or null if there was error initializing.
     */
    protected XSLTransformer getTransformer(String direction)
    {
        if (transformerFile == null)
        {
            String myAlias = getPluginInstanceName();
            if (myAlias == null)
            {
                log.error("Must use PluginManager to instantiate XSLTCrosswalk so the class knows its name.");
                return null;
            }
            String cmPropName = CONFIG_PREFIX+direction+"."+myAlias+CONFIG_STYLESHEET;
            String fname = ConfigurationManager.getProperty(cmPropName);
            if (fname == null)
            {
                log.error("Missing configuration filename for XSLT-based crosswalk: no "+
                          "value for property = "+cmPropName);
                return null;
            }
            else
            {
                String parent = ConfigurationManager.getProperty("dspace.dir") +
                    File.separator + "config" + File.separator;
                transformerFile = new File(parent, fname);
            }
        }

        // load if first time, or reload if stylesheet changed:
        if (transformer == null ||
            transformerFile.lastModified() > transformerLastModified)
        {
            try
            {
                log.debug((transformer == null ? "Loading " : "Reloading")+
                          getPluginInstanceName()+" XSLT stylesheet from "+transformerFile.toString());
                transformer = new XSLTransformer(transformerFile);
                transformerLastModified = transformerFile.lastModified();
            }
            catch (XSLTransformException e)
            {
                log.error("Failed to initialize XSLTCrosswalk("+getPluginInstanceName()+"):"+e.toString());
            }
        }
        return transformer;
    }
}
