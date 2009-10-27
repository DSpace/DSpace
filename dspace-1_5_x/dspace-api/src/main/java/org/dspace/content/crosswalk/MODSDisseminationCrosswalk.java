/*
 * MODSDisseminationCrosswalk.java
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.SelfNamedPlugin;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * Configurable MODS Crosswalk
 * <p>
 * This class supports multiple dissemination crosswalks from DSpace
 * internal data to the MODS XML format
 *  (see <a href="http://www.loc.gov/standards/mods/">http://www.loc.gov/standards/mods/</a>.)
 * <p>
 * It registers multiple Plugin names, which it reads from
 * the DSpace configuration as follows:
 *
 * <h3>Configuration</h3>
 * Every key starting with <code>"crosswalk.mods.properties."</code> describes a
 * MODS crosswalk.  Everything after the last period is the <em>plugin name</em>,
 * and the value is the pathname (relative to <code><em>dspace.dir</em>/config</code>)
 * of the crosswalk configuration file.
 * <p>
 * You can have two names point to the same crosswalk,
 * just add two configuration entries with the same value, e.g.
 * <pre>
 *    crosswalk.mods.properties.MODS = crosswalks/mods.properties
 *    crosswalk.mods.properties.default = crosswalks/mods.properties
 * </pre>
 * The first line creates a plugin with the name <code>"MODS"</code>
 * which is configured from the file <em>dspace-dir</em><code>/config/crosswalks/mods.properties</code>.
 * <p>
 * Since there is significant overhead in reading the properties file to
 * configure the crosswalk, and a crosswalk instance may be used any number
 * of times, we recommend caching one instance of the crosswalk for each
 * name and simply reusing those instances.  The PluginManager does this
 * by default.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class MODSDisseminationCrosswalk extends SelfNamedPlugin
    implements DisseminationCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(MODSDisseminationCrosswalk.class);

    private final static String CONFIG_PREFIX = "crosswalk.mods.properties.";

    /**
     * Fill in the plugin alias table from DSpace configuration entries
     * for configuration files for flavors of MODS crosswalk:
     */
    private static String aliases[] = null;
    static
    {
        List aliasList = new ArrayList();
        Enumeration pe = ConfigurationManager.propertyNames();
        while (pe.hasMoreElements())
        {
            String key = (String)pe.nextElement();
            if (key.startsWith(CONFIG_PREFIX))
                aliasList.add(key.substring(CONFIG_PREFIX.length()));
        }
        aliases = (String[])aliasList.toArray(new String[aliasList.size()]);
    }

    public static String[] getPluginNames()
    {
        return aliases;
    }

    /**
     * MODS namespace.
     */
    public static final Namespace MODS_NS =
        Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

    private static final Namespace XLINK_NS =
        Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    private static final Namespace namespaces[] = { MODS_NS, XLINK_NS };

    /**  URL of MODS XML Schema */
    public static final String MODS_XSD =
        "http://www.loc.gov/standards/mods/v3/mods-3-1.xsd";

    private static final String schemaLocation =
        MODS_NS.getURI()+" "+MODS_XSD;

    private static XMLOutputter outputUgly = new XMLOutputter();
    private static XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
    private static SAXBuilder builder = new SAXBuilder();

    private HashMap modsMap = null;

    /**
     * Container for crosswalk mapping: expressed as "triple" of:
     * 1. QDC field name (really field.qualifier).
     * 2. XML subtree to add to MODS record.
     * 3. XPath expression showing places to plug in the value.
     */
    static class modsTriple
    {
        public String qdc = null;
        public Element xml = null;
        public XPath xpath = null;

        /**
         * Initialize from text versions of QDC, XML and XPath.
         * The DC stays a string; parse the XML with appropriate
         * namespaces; "compile" the XPath.
         */
        public static modsTriple create(String qdc, String xml, String xpath)
        {
            modsTriple result = new modsTriple();

            final String prolog = "<mods xmlns:"+MODS_NS.getPrefix()+"=\""+MODS_NS.getURI()+"\" "+
                            "xmlns:"+XLINK_NS.getPrefix()+"=\""+XLINK_NS.getURI()+"\">";
            final String postlog = "</mods>";
            try
            {
                result.qdc = qdc;
                result.xpath = XPath.newInstance(xpath);
                result.xpath.addNamespace(MODS_NS.getPrefix(), MODS_NS.getURI());
                result.xpath.addNamespace(XLINK_NS);
                Document d = builder.build(new StringReader(prolog+xml+postlog));
                result.xml = (Element)d.getRootElement().getContent(0);
            }
            catch (JDOMException je)
            {
                log.error("Error initializing modsTriple(\""+qdc+"\",\""+xml+"\",\""+xpath+"\"): got "+je.toString());
                return null;
            }
            catch (IOException je)
            {
                log.error("Error initializing modsTriple(\""+qdc+"\",\""+xml+"\",\""+xpath+"\"): got "+je.toString());
                return null;
            }
            return result;
        }
    }

    /**
     * Initialize Crosswalk table from a properties file
     * which itself is the value of the DSpace configuration property
     * "crosswalk.mods.properties.X", where "X" is the alias name of this instance.
     * Each instance may be configured with a separate mapping table.
     *
     * The MODS crosswalk configuration properties follow the format:
     *
     *  {field-name} = {XML-fragment} | {XPath}
     *
     *  1. qualified DC field name is of the form
     *       {MDschema}.{element}.{qualifier}
     *
     *      e.g.  dc.contributor.author
     *
     *  2. XML fragment is prototype of metadata element, with empty or "%s"
     *     placeholders for value(s).  NOTE: Leave the %s's in becaue
     *     it's much easier then to see if something is broken.
     *
     *  3. XPath expression listing point(s) in the above XML where
     *     the value is to be inserted.  Context is the element itself.
     *
     * Example properties line:
     *
     *  dc.description.abstract = <mods:abstract>%s</mods:abstract> | text()
     *
     */
    private void initMap()
        throws CrosswalkInternalException
    {
        if (modsMap != null)
            return;
        String myAlias = getPluginInstanceName();
        if (myAlias == null)
        {
            log.error("Must use PluginManager to instantiate MODSDisseminationCrosswalk so the class knows its name.");
            return;
        }
        String cmPropName = CONFIG_PREFIX+myAlias;
        String propsFilename = ConfigurationManager.getProperty(cmPropName);
        if (propsFilename == null)
        {
            String msg = "MODS crosswalk missing "+
                "configuration file for crosswalk named \""+myAlias+"\"";
            log.error(msg);
            throw new CrosswalkInternalException(msg);
        }
        else
        {
            String parent = ConfigurationManager.getProperty("dspace.dir") +
                File.separator + "config" + File.separator;
            File propsFile = new File(parent, propsFilename);
            Properties modsConfig = new Properties();
            FileInputStream pfs = null;
            try
            {
                pfs = new FileInputStream(propsFile);
                modsConfig.load(pfs);
            }
            catch (IOException e)
            {
                log.error("Error opening or reading MODS properties file: "+propsFile.toString()+": "+e.toString());
                throw new CrosswalkInternalException("MODS crosswalk cannot "+
                    "open config file: "+e.toString());
            }
            finally
            {
                if (pfs != null)
                    try { pfs.close(); } catch (IOException ioe) { }
            }

            modsMap = new HashMap();
            Enumeration pe = modsConfig.propertyNames();
            while (pe.hasMoreElements())
            {
                String qdc = (String)pe.nextElement();
                String val = modsConfig.getProperty(qdc);
                String pair[] = val.split("\\s+\\|\\s+", 2);
                if (pair.length < 2)
                    log.warn("Illegal MODS mapping in "+propsFile.toString()+", line = "+
                            qdc + " = " + val);
                else
                {
                    modsTriple trip = modsTriple.create(qdc, pair[0], pair[1]);
                    if (trip != null)
                        modsMap.put(qdc, trip);
                }
            }
        }
    }

    public Namespace[] getNamespaces()
    {
        return namespaces;
    }

    public String getSchemaLocation()
    {
        return schemaLocation;
    }

    /**
     * Returns object's metadata in MODS format, as List of XML structure nodes.
     */
    public List disseminateList(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        return disseminateListInternal(dso, true);
    }

    public Element disseminateElement(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        Element root = new Element("mods", MODS_NS);
        root.setAttribute("schemaLocation", schemaLocation, XSI_NS);
        root.addContent(disseminateListInternal(dso,false));
        return root;
    }

    private List disseminateListInternal(DSpaceObject dso, boolean addSchema)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        if (dso.getType() != Constants.ITEM)
            throw new CrosswalkObjectNotSupported("MODSDisseminationCrosswalk can only crosswalk an Item.");
        Item item = (Item)dso;
        initMap();

        DCValue[] dc = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        List result = new ArrayList(dc.length);
        for (int i = 0; i < dc.length; i++)
        {
            // Compose qualified DC name - schema.element[.qualifier]
            // e.g. "dc.title", "dc.subject.lcc", "lom.Classification.Keyword"
            String qdc = dc[i].schema+"."+
                         ((dc[i].qualifier == null) ? dc[i].element
                            : (dc[i].element + "." + dc[i].qualifier));

            modsTriple trip = (modsTriple)modsMap.get(qdc);
            if (trip == null)
                log.warn("WARNING: "+getPluginInstanceName()+": No MODS mapping for \"" + qdc+"\"");
            else
            {
                try
                {
                    Element me = (Element)trip.xml.clone();
                    if (addSchema)
                        me.setAttribute("schemaLocation", schemaLocation, XSI_NS);
                    Iterator ni = trip.xpath.selectNodes(me).iterator();
                    if (!ni.hasNext())
                        log.warn("XPath \""+trip.xpath.getXPath()+
                          "\" found no elements in \""+
                          outputUgly.outputString(me)+
                          "\", qdc="+qdc);
                    while (ni.hasNext())
                    {
                        Object what = ni.next();
                        if (what instanceof Element)
                            ((Element)what).setText(dc[i].value);
                        else if (what instanceof Attribute)
                            ((Attribute)what).setValue(dc[i].value);
                        else if (what instanceof Text)
                            ((Text)what).setText(dc[i].value);
                        else
                            log.warn("Got unknown object from XPath, class="+what.getClass().getName());
                    }
                    result.add(me);
                }
                catch (JDOMException je)
                {
                    log.error("Error following XPath in modsTriple: context="+
                        outputUgly.outputString(trip.xml)+
                        ", xpath="+trip.xpath.getXPath()+", exception="+
                        je.toString());
                }
            }
        }
        return result;
    }

    public boolean canDisseminate(DSpaceObject dso)
    {
        return true;
    }

    public boolean preferList()
    {
        return false;
    }
}
