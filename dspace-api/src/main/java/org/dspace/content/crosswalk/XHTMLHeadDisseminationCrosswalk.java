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
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Verifier;

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
 * @author Robert Tansley
 */
public class XHTMLHeadDisseminationCrosswalk
        extends SelfNamedPlugin
        implements DisseminationCrosswalk {
    /**
     * log4j logger
     */
    private static final Logger log = LogManager.getLogger(XHTMLHeadDisseminationCrosswalk.class);

    private static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";

    protected final ItemService itemService
            = ContentServiceFactory.getInstance().getItemService();
    protected final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Location of configuration file
     */
    private final String config = configurationService.getProperty("dspace.dir")
        + File.separator
        + "config"
        + File.separator
        + "crosswalks"
        + File.separator + "xhtml-head-item.properties";

    /**
     * Maps DSpace metadata field to name to use in XHTML head element, e.g.
     * dc.creator or dc.description.abstract
     */
    private Map<String, String> names;

    /**
     * Maps DSpace metadata field to scheme for that field, if any
     */
    private Map<String, String> schemes;

    /**
     * Schemas to add -- maps schema.NAME to schema URL
     */
    private Map<String, String> schemaURLs;

    public XHTMLHeadDisseminationCrosswalk() throws IOException {
        names = new HashMap<>();
        schemes = new HashMap<>();
        schemaURLs = new HashMap<>();

        // Read in configuration
        Properties crosswalkProps = new Properties();
        FileInputStream fis = new FileInputStream(config);
        try {
            crosswalkProps.load(fis);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }

        Enumeration e = crosswalkProps.keys();
        while (e.hasMoreElements()) {
            String prop = (String) e.nextElement();

            if (prop.startsWith("schema.")) {
                schemaURLs.put(prop, crosswalkProps.getProperty(prop));
            } else {
                String[] s = ((String) crosswalkProps.get(prop)).split(",");

                if (s.length == 2) {
                    schemes.put(prop, s[1]);
                }

                if (s.length == 1 || s.length == 2) {
                    names.put(prop, s[0]);
                } else {
                    log.warn("Malformed parameter " + prop + " in " + config);
                }
            }
        }
    }

    @Override
    public boolean canDisseminate(DSpaceObject dso) {
        return (dso.getType() == Constants.ITEM);
    }

    /**
     * This generates a &lt;head&gt; element around the metadata; in general
     * this will probably not be used
     *
     * @param context context
     * @throws CrosswalkException crosswalk error
     * @throws IOException        if IO error
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public Element disseminateElement(Context context, DSpaceObject dso)
        throws CrosswalkException, IOException, SQLException,
        AuthorizeException {
        Element head = new Element("head", XHTML_NAMESPACE);
        head.addContent(disseminateList(context, dso));

        return head;
    }

    /**
     * Return &lt;meta&gt; elements that can be put in the &lt;head&gt; element
     * of an XHTML document.
     *
     * @param context context
     * @throws CrosswalkException crosswalk error
     * @throws IOException        if IO error
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @return List of Elements
     */
    @Override
    public List<Element> disseminateList(Context context, DSpaceObject dso) throws CrosswalkException,
        IOException, SQLException, AuthorizeException {
        if (dso.getType() != Constants.ITEM) {
            String h = dso.getHandle();
            throw new CrosswalkObjectNotSupported(
                "Can only support items; object passed in with DB ID "
                    + dso.getID() + ", type "
                    + Constants.typeText[dso.getType()] + ", handle "
                    + (h == null ? "null" : h));
        }

        Item item = (Item) dso;
        String handle = item.getHandle();
        List<Element> metas = new ArrayList<>();
        List<MetadataValue> values = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        // Add in schema URLs e.g. <link rel="schema.DC" href="...." />
        Iterator<String> schemaIterator = schemaURLs.keySet().iterator();
        while (schemaIterator.hasNext()) {
            String s = schemaIterator.next();
            Element e = new Element("link", XHTML_NAMESPACE);
            e.setAttribute("rel", s);
            e.setAttribute("href", schemaURLs.get(s));

            metas.add(e);
        }

        for (int i = 0; i < values.size(); i++) {
            MetadataValue v = values.get(i);
            MetadataField metadataField = v.getMetadataField();
            MetadataSchema metadataSchema = metadataField.getMetadataSchema();

            // Work out the key for the Maps that will tell us which metadata
            // name + scheme to use
            String key = metadataSchema.getName() + "." + metadataField.getElement()
                + (metadataField.getQualifier() != null ? "." + metadataField.getQualifier() : "");
            String originalKey = key; // For later error msg

            // Find appropriate metadata field name to put in element
            String name = names.get(key);

            // If we don't have a field, try removing qualifier
            if (name == null && metadataField.getQualifier() != null) {
                key = metadataSchema.getName() + "." + metadataField.getElement();
                name = names.get(key);
            }

            // Do not include description.provenance
            boolean provenance = "description".equals(metadataField.getElement()) && "provenance"
                .equals(metadataField.getQualifier());

            if (name == null) {
                // Most of the time, in this crosswalk, an unrecognised
                // element is OK, so just report at DEBUG level
                if (log.isDebugEnabled()) {
                    log.debug("No <meta> field for item "
                                  + (handle == null ? String.valueOf(dso.getID())
                        : handle) + " field " + originalKey);
                }
            } else if (!provenance) {
                Element e = new Element("meta", XHTML_NAMESPACE);
                e.setAttribute("name", name);
                if (v.getValue() == null) {
                    e.setAttribute("content", "");
                } else {
                    // Check that we can output the content
                    String reason = Verifier.checkCharacterData(v.getValue());
                    if (reason == null) {
                        // TODO: Check valid encoding?  We assume UTF-8
                        // TODO: Check escaping "<>&
                        e.setAttribute("content", v.getValue());
                    } else {
                        // Warn that we found invalid characters
                        log.warn("Invalid attribute characters in Metadata: " + reason);

                        // Strip any characters that we can, and if the result is valid, output it
                        String simpleText = v.getValue().replaceAll("\\p{Cntrl}", "");
                        if (Verifier.checkCharacterData(simpleText) == null) {
                            e.setAttribute("content", simpleText);
                        }
                    }
                }
                if (v.getLanguage() != null && !v.getLanguage().equals("")) {
                    e.setAttribute("lang", v.getLanguage(), Namespace.XML_NAMESPACE);
                }
                String schemeAttr = schemes.get(key);
                if (schemeAttr != null) {
                    e.setAttribute("scheme", schemeAttr);
                }
                metas.add(e);
            }
        }

        return metas;
    }

    @Override
    public Namespace[] getNamespaces() {
        return new Namespace[] {Namespace.getNamespace(XHTML_NAMESPACE)};
    }

    @Override
    public String getSchemaLocation() {
        return "";
    }

    @Override
    public boolean preferList() {
        return true;
    }

    // Plugin Methods
    public static String[] getPluginNames() {
        return new String[] {"XHTML_HEAD_ITEM"};
    }
}
