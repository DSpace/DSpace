/*
 * XHTMLHeadDisseminationCrosswalk.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2006, Hewlett-Packard Company and Massachusetts
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

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.SelfNamedPlugin;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Verifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Crosswalk for creating appropriate &lt;meta&gt; elements to appear in the
 * item display page for a particular item, for improving automated processing
 * of the page (e.g. by search engines). The metadata included should be as rich
 * yet standards-compliant as possible.
 * <P>
 * The configuration file
 * <code>${dspace.dir}/config/xhtml-head-item.properties</code> contains the
 * relevant mappings. Note: where there is a custom qualifier for which no
 * corresponding mapping exists, the crosswalk will remove the qualifier and try
 * again with just the element.
 * <P>
 * e.g. if a field exists in the database "dc.contributor.editor", and there is
 * no dc.contributor.editor property below, the mapping for "dc.contributor"
 * will be used. If an element in the item metadata record does not appear in
 * the configuration, it is simply ignored; the emphasis here is on exposing
 * standards-compliant metadata.
 * <P>
 * TODO: This may usefully be extended later to work with communities and
 * collections.
 *
 * @version $Revision$
 * @author Robert Tansley
 */
public class XHTMLHeadDisseminationCrosswalk extends SelfNamedPlugin implements
        DisseminationCrosswalk
{
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(XHTMLHeadDisseminationCrosswalk.class);

    /** Location of config file */
    private final String config = ConfigurationManager
            .getProperty("dspace.dir")
            + File.separator
            + "config"
            + File.separator
            + "crosswalks"
            + File.separator + "xhtml-head-item.properties";

    private final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";

    /**
     * Maps DSpace metadata field to name to use in XHTML head element, e.g.
     * dc.creator or dc.description.abstract
     */
    private Map<String, String> names;

    /** Maps DSpace metadata field to scheme for that field, if any */
    private Map<String, String> schemes;

    /** Schemas to add -- maps schema.NAME to schema URL */
    private Map<String, String> schemaURLs;

    public XHTMLHeadDisseminationCrosswalk() throws IOException
    {
        names = new HashMap<String, String>();
        schemes = new HashMap<String, String>();
        schemaURLs = new HashMap<String, String>();

        // Read in configuration
        Properties crosswalkProps = new Properties();
        FileInputStream fis = new FileInputStream(config);
        try
        {
            crosswalkProps.load(fis);
        }
        finally
        {
            if (fis != null)
                try { fis.close(); } catch (IOException ioe) { }
        }

        Enumeration e = crosswalkProps.keys();
        while (e.hasMoreElements())
        {
            String prop = (String) e.nextElement();

            if (prop.startsWith("schema."))
            {
                schemaURLs.put(prop, crosswalkProps.getProperty(prop));
            }
            else
            {
                String[] s = ((String) crosswalkProps.get(prop)).split(",");

                if (s.length == 2)
                {
                    schemes.put(prop, s[1]);
                }

                if (s.length == 1 || s.length == 2)
                {
                    names.put(prop, s[0]);
                } else
                {
                    log.warn("Malformed parameter " + prop + " in " + config);
                }
            }
        }
    }

    public boolean canDisseminate(DSpaceObject dso)
    {
        return (dso.getType() == Constants.ITEM);
    }

    /**
     * This generates a &lt;head&gt; element around the metadata; in general
     * this will probably not be used
     */
    public Element disseminateElement(DSpaceObject dso)
            throws CrosswalkException, IOException, SQLException,
            AuthorizeException
    {
        Element head = new Element("head", XHTML_NAMESPACE);
        head.addContent(disseminateList(dso));

        return head;
    }

    /**
     * Return &lt;meta&gt; elements that can be put in the &lt;head&gt; element
     * of an XHTML document.
     */
    public List disseminateList(DSpaceObject dso) throws CrosswalkException,
            IOException, SQLException, AuthorizeException
    {
        if (dso.getType() != Constants.ITEM)
        {
            String h = dso.getHandle();
            throw new CrosswalkObjectNotSupported(
                    "Can only support items; object passed in with DB ID "
                            + dso.getID() + ", type "
                            + Constants.typeText[dso.getType()] + ", handle "
                            + (h == null ? "null" : h));
        }

        Item item = (Item) dso;
        String handle = item.getHandle();
        List<Element> metas = new ArrayList<Element>();
        DCValue[] values = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        // Add in schema URLs e.g. <link rel="schema.DC" href="...." />
        Iterator<String> schemaIterator = schemaURLs.keySet().iterator();
        while (schemaIterator.hasNext())
        {
            String s = schemaIterator.next();
            Element e = new Element("link", XHTML_NAMESPACE);
            e.setAttribute("rel", s);
            e.setAttribute("href", schemaURLs.get(s));

            metas.add(e);
        }

        for (int i = 0; i < values.length; i++)
        {
            DCValue v = values[i];

            // Work out the key for the Maps that will tell us which metadata
            // name + scheme to use
            String key = v.schema + "." + v.element
                    + (v.qualifier != null ? "." + v.qualifier : "");
            String originalKey = key; // For later error msg

            // Find appropriate metadata field name to put in element
            String name = names.get(key);

            // If we don't have a field, try removing qualifier
            if (name == null && v.qualifier != null)
            {
                key = v.schema + "." + v.element;
                name = names.get(key);
            }

            if (name == null)
            {
                // Most of the time, in this crosswalk, an unrecognised
                // element is OK, so just report at DEBUG level
               if (log.isDebugEnabled())
               {
                   log.debug("No <meta> field for item "
                            + (handle == null ? String.valueOf(dso.getID())
                                    : handle) + " field " + originalKey);
               }
            }
            else
            {
                Element e = new Element("meta", XHTML_NAMESPACE);
                e.setAttribute("name", name);
                if (v.value == null)
                {
                    e.setAttribute("content", "");
                }
                else
                {
                    // Check that we can output the content
                    String reason = Verifier.checkCharacterData(v.value);
                    if (reason == null)
                    {
                        // TODO: Check valid encoding?  We assume UTF-8
                        // TODO: Check escaping "<>&
                        e.setAttribute("content", v.value == null ? "" : v.value);
                    }
                    else
                    {
                        // Warn that we found invalid characters
                        log.warn("Invalid attribute characters in Metadata: " + reason);

                        // Strip any characters that we can, and if the result is valid, output it
                        String simpleText = v.value.replaceAll("\\p{Cntrl}", "");
                        if (Verifier.checkCharacterData(simpleText) == null)
                            e.setAttribute("content", simpleText);
                    }
                }
                if (v.language != null && !v.language.equals(""))
                {
                    e.setAttribute("lang", v.language, Namespace.XML_NAMESPACE);
                }
                String schemeAttr = schemes.get(key);
                if (schemeAttr != null)
                {
                    e.setAttribute("scheme", schemeAttr);
                }
                metas.add(e);
            }
        }

        return metas;
    }

    public Namespace[] getNamespaces()
    {
        return new Namespace[] {Namespace.getNamespace(XHTML_NAMESPACE)};
    }

    public String getSchemaLocation()
    {
        return "";
    }

    public boolean preferList()
    {
        return true;
    }

    // Plugin Methods
    public static String[] getPluginNames()
    {
        return new String[] {"XHTML_HEAD_ITEM"};
    }
}
