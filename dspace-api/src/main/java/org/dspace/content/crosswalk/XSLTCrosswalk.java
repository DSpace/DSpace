/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
    protected static final String CONFIG_PREFIX = "crosswalk.";

    private static final String CONFIG_STYLESHEET = ".stylesheet";

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

        List<String> aliasList = new ArrayList<String>();
        Enumeration<String> pe = (Enumeration<String>)ConfigurationManager.propertyNames();

        log.debug("XSLTCrosswalk: Looking for config prefix = "+prefix);
        while (pe.hasMoreElements())
        {
            String key = pe.nextElement();
            if (key.startsWith(prefix) && key.endsWith(suffix))
            {
                log.debug("Getting XSLT plugin name from config line: "+key);
                aliasList.add(key.substring(prefix.length(), key.length()-suffix.length()));
            }
        }
        return aliasList.toArray(new String[aliasList.size()]);
    }

    private XSLTransformer transformer = null;
    private File transformerFile = null;
    private long transformerLastModified = 0;

    /**
     * Initialize the Transformation stylesheet from configured stylesheet file.
     * @param direction the direction of xwalk, either "submission" or
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
