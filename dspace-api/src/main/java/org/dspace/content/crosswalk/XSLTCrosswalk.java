/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.dspace.core.SelfNamedPlugin;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * The <em>alias</em> names the Plugin name,
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
 * You must use the <code>PluginService</code> to instantiate an
 * XSLT crosswalk plugin, e.g.
 * <pre> IngestionCrosswalk xwalk = CoreServiceFactory.getInstance().getPluginService().getPlugin(IngestionCrosswalk
 * .class, "LOM");</pre>
 * <p>
 * Since there is significant overhead in reading the properties file to
 * configure the crosswalk, and a crosswalk instance may be used any number
 * of times, we recommend caching one instance of the crosswalk for each
 * alias and simply reusing those instances.  The <code>PluginService</code>
 * does this automatically.
 *
 * @author Larry Stone
 */
public abstract class XSLTCrosswalk extends SelfNamedPlugin {
    /**
     * log4j category
     */
    private static final Logger LOG = LoggerFactory.getLogger(XSLTCrosswalk.class);

    /**
     * DSpace XML Namespace in JDOM form.
     */
    public static final Namespace DIM_NS =
        Namespace.getNamespace("dim", "http://www.dspace.org/xmlns/dspace/dim");

    /**
     * Prefix for all lines in the configuration file for XSLT plugins.
     */
    protected static final String CONFIG_PREFIX = "crosswalk.";

    private static final String CONFIG_STYLESHEET = ".stylesheet";

    /**
     * Derive list of plugin name from DSpace configuration entries
     * for crosswalks.
     *
     * @param direction "dissemination" or "submission", so it looks for keys like
     *                  <code>crosswalk.submission.{NAME}.stylesheet</code>
     * @return names to be given to the plugins of that direction.
     */
    protected static String[] makeAliases(String direction) {
        String prefix = CONFIG_PREFIX + direction + ".";
        String suffix = CONFIG_STYLESHEET;

        List<String> aliasList = new ArrayList<>();
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        List<String> configKeys = configurationService.getPropertyKeys(prefix);

        LOG.debug("XSLTCrosswalk: Looking for config prefix = {}", prefix);
        for (String key : configKeys) {
            if (key.endsWith(suffix)) {
                LOG.debug("Getting XSLT plugin name from config line: {}", key);
                aliasList.add(key.substring(prefix.length(), key.length() - suffix.length()));
            }
        }
        return aliasList.toArray(new String[aliasList.size()]);
    }

    private Transformer transformer = null;
    private File transformFile = null;
    private long transformLastModified = 0;

    /**
     * Initialize the Transformation stylesheet from configured stylesheet file.
     *
     * @param direction the direction of xwalk, either "submission" or
     *                  "dissemination"
     * @return transformer or null if there was error initializing.
     */
    protected Transformer getTransformer(String direction) {
        if (transformFile == null) {
            String myAlias = getPluginInstanceName();
            if (myAlias == null) {
                LOG.error("Must use PluginService to instantiate XSLTCrosswalk so the class knows its name.");
                return null;
            }
            String cmPropName = CONFIG_PREFIX + direction + "." + myAlias + CONFIG_STYLESHEET;
            ConfigurationService configurationService
                    = DSpaceServicesFactory.getInstance().getConfigurationService();
            String fname = configurationService.getProperty(cmPropName);
            if (fname == null) {
                LOG.error("Missing configuration filename for XSLT-based crosswalk: no " +
                              "value for property = {}", cmPropName);
                return null;
            } else {
                String parent = configurationService.getProperty("dspace.dir") +
                    File.separator + "config" + File.separator;
                transformFile = new File(parent, fname);
            }
        }

        // load if first time, or reload if stylesheet changed:
        if (transformer == null ||
            transformFile.lastModified() > transformLastModified) {
            try {
                LOG.debug(
                    (transformer == null ? "Loading {} XSLT stylesheet from {}" : "Reloading {} XSLT stylesheet from " +
                        "{}"),
                    getPluginInstanceName(), transformFile.toString());

                Source transformSource
                    = new StreamSource(new FileInputStream(transformFile));
                TransformerFactory transformerFactory
                    = TransformerFactory.newInstance();
                transformer = transformerFactory.newTransformer(transformSource);
                transformLastModified = transformFile.lastModified();
            } catch (TransformerConfigurationException | FileNotFoundException e) {
                LOG.error("Failed to initialize XSLTCrosswalk({}):  {}",
                          getPluginInstanceName(), e.toString());
            }
        }
        return transformer;
    }
}
