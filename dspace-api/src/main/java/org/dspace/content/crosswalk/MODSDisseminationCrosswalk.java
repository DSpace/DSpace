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
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.SelfNamedPlugin;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.Verifier;
import org.jdom.input.SAXBuilder;
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
 * name and simply reusing those instances.  The PluginService does this
 * by default.
 *
 * @author Larry Stone
 * @author Scott Phillips
 * @version $Revision$
 */
public class MODSDisseminationCrosswalk extends SelfNamedPlugin
    implements DisseminationCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(MODSDisseminationCrosswalk.class);

    private static final String CONFIG_PREFIX = "crosswalk.mods.properties.";

    protected final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * Fill in the plugin alias table from DSpace configuration entries
     * for configuration files for flavors of MODS crosswalk:
     */
    private static String aliases[] = null;
    static
    {
        List<String> aliasList = new ArrayList<String>();
        Enumeration<String> pe = (Enumeration<String>)ConfigurationManager.propertyNames();
        while (pe.hasMoreElements())
        {
            String key = pe.nextElement();
            if (key.startsWith(CONFIG_PREFIX))
            {
                aliasList.add(key.substring(CONFIG_PREFIX.length()));
            }
        }
        aliases = (String[])aliasList.toArray(new String[aliasList.size()]);
    }

    public static String[] getPluginNames()
    {
        return (String[]) ArrayUtils.clone(aliases);
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
    private static SAXBuilder builder = new SAXBuilder();

    private Map<String, modsTriple> modsMap = null;

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
        {
            return;
        }
        String myAlias = getPluginInstanceName();
        if (myAlias == null)
        {
            log.error("Must use PluginService to instantiate MODSDisseminationCrosswalk so the class knows its name.");
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
                    "open config file: "+e.toString(), e);
            }
            finally
            {
                if (pfs != null)
                {
                    try
                    {
                        pfs.close();
                    }
                    catch (IOException ioe)
                    {
                    }
                }
            }

            modsMap = new HashMap<String, modsTriple>();
            Enumeration<String> pe = (Enumeration<String>)modsConfig.propertyNames();
            while (pe.hasMoreElements())
            {
                String qdc = pe.nextElement();
                String val = modsConfig.getProperty(qdc);
                String pair[] = val.split("\\s+\\|\\s+", 2);
                if (pair.length < 2)
                {
                    log.warn("Illegal MODS mapping in " + propsFile.toString() + ", line = " +
                            qdc + " = " + val);
                }
                else
                {
                    modsTriple trip = modsTriple.create(qdc, pair[0], pair[1]);
                    if (trip != null)
                    {
                        modsMap.put(qdc, trip);
                    }
                }
            }
        }
    }

    /**
     *  Return the MODS namespace
     */
    @Override
    public Namespace[] getNamespaces()
    {
        return (Namespace[]) ArrayUtils.clone(namespaces);
    }

    /**
     * Return the MODS schema
     */
    @Override
    public String getSchemaLocation()
    {
        return schemaLocation;
    }

    /**
     * Returns object's metadata in MODS format, as List of XML structure nodes.
     * @param context context
     * @throws CrosswalkException if crosswalk error
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public List<Element> disseminateList(Context context, DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        return disseminateListInternal(dso, true);
    }

    /**
     * Disseminate an Item, Collection, or Community to MODS.
     * @param context context
     * @throws CrosswalkException if crosswalk error
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public Element disseminateElement(Context context, DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        Element root = new Element("mods", MODS_NS);
        root.setAttribute("schemaLocation", schemaLocation, XSI_NS);
        root.addContent(disseminateListInternal(dso,false));
        return root;
    }

    private List<Element> disseminateListInternal(DSpaceObject dso, boolean addSchema)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        List<MockMetadataValue> dcvs = null;
        if (dso.getType() == Constants.ITEM)
        {
            dcvs = item2Metadata((Item) dso);
        }
        else if (dso.getType() == Constants.COLLECTION)
        {
            dcvs = collection2Metadata((Collection) dso);
        }
        else if (dso.getType() == Constants.COMMUNITY)
        {
            dcvs = community2Metadata((Community) dso);
        }
        else if (dso.getType() == Constants.SITE)
        {
            dcvs = site2Metadata((Site) dso);
        }
        else
        {
            throw new CrosswalkObjectNotSupported(
                    "MODSDisseminationCrosswalk can only crosswalk Items, Collections, or Communities");
        }
        initMap();

        List<Element> result = new ArrayList<Element>(dcvs.size());

        for (MockMetadataValue dcv : dcvs)
        {
            String qdc = dcv.getSchema() + "." + dcv.getElement();
            if (dcv.getQualifier() != null)
            {
                qdc += "." + dcv.getQualifier();
            }
            String value = dcv.getValue();

            modsTriple trip = modsMap.get(qdc);
            if (trip == null) {
                log.warn("WARNING: " + getPluginInstanceName() + ": No MODS mapping for \"" + qdc + "\"");
            } else {
                try {
                    Element me = (Element) trip.xml.clone();
                    if (addSchema) {
                        me.setAttribute("schemaLocation", schemaLocation, XSI_NS);
                    }
                    Iterator ni = trip.xpath.selectNodes(me).iterator();
                    if (!ni.hasNext()) {
                        log.warn("XPath \"" + trip.xpath.getXPath() +
                                "\" found no elements in \"" +
                                outputUgly.outputString(me) +
                                "\", qdc=" + qdc);
                    }
                    while (ni.hasNext()) {
                        Object what = ni.next();
                        if (what instanceof Element) {
                            ((Element) what).setText(checkedString(value));
                        } else if (what instanceof Attribute) {
                            ((Attribute) what).setValue(checkedString(value));
                        } else if (what instanceof Text) {
                            ((Text) what).setText(checkedString(value));
                        } else {
                            log.warn("Got unknown object from XPath, class=" + what.getClass().getName());
                        }
                    }
                    result.add(me);
                } catch (JDOMException je) {
                    log.error("Error following XPath in modsTriple: context=" +
                            outputUgly.outputString(trip.xml) +
                            ", xpath=" + trip.xpath.getXPath() + ", exception=" +
                            je.toString());
                }
            }
        }
        return result;
    }

    /**
     * ModsCrosswalk can disseminate: Items, Collections, Communities, and Site.
     */
    @Override
    public boolean canDisseminate(DSpaceObject dso)
    {
        return (dso.getType() == Constants.ITEM ||
            dso.getType() == Constants.COLLECTION ||
            dso.getType() == Constants.COMMUNITY ||
            dso.getType() == Constants.SITE);
    }

    /**
     * ModsCrosswalk prefer's element form over list.
     */
    @Override
    public boolean preferList()
    {
        return false;
    }


    /**
     * Generate a list of metadata elements for the given DSpace
     * site.
     *
     * @param site
     *            The site to derive metadata from
     * @return list of metadata
     */
    protected List<MockMetadataValue> site2Metadata(Site site)
    {
        List<MockMetadataValue> metadata = new ArrayList<>();

        String identifier_uri = "http://hdl.handle.net/"
                + site.getHandle();
        String title = site.getName();
        String url = site.getURL();

        if (identifier_uri != null)
        {
            metadata.add(createDCValue("identifier.uri", null, identifier_uri));
        }

        //FIXME: adding two URIs for now (site handle and URL), in case site isn't using handles
        if (url != null)
        {
            metadata.add(createDCValue("identifier.uri", null, url));
        }

        if (title != null)
        {
            metadata.add(createDCValue("title", null, title));
        }

        return metadata;
    }
    /**
     * Generate a list of metadata elements for the given DSpace
     * community.
     *
     * @param community
     *            The community to derive metadata from
     * @return list of metadata
     */
    protected List<MockMetadataValue> community2Metadata(Community community)
    {
        List<MockMetadataValue> metadata = new ArrayList<>();

        String description = communityService.getMetadata(community, "introductory_text");
        String description_abstract = communityService.getMetadata(community, "short_description");
        String description_table = communityService.getMetadata(community,"side_bar_text");
        String identifier_uri = "http://hdl.handle.net/"
                + community.getHandle();
        String rights = communityService.getMetadata(community,"copyright_text");
        String title = communityService.getMetadata(community,"name");

        metadata.add(createDCValue("description", null, description));

        if (description_abstract != null)
        {
            metadata.add(createDCValue("description", "abstract", description_abstract));
        }

        if (description_table != null)
        {
            metadata.add(createDCValue("description", "tableofcontents", description_table));
        }

        if (identifier_uri != null)
        {
            metadata.add(createDCValue("identifier.uri", null, identifier_uri));
        }

        if (rights != null)
        {
            metadata.add(createDCValue("rights", null, rights));
        }

        if (title != null)
        {
            metadata.add(createDCValue("title", null, title));
        }

        return metadata;
    }

    /**
     * Generate a list of metadata elements for the given DSpace
     * collection.
     *
     * @param collection
     *            The collection to derive metadata from
     * @return list of metadata
     */
    protected List<MockMetadataValue> collection2Metadata(Collection collection)
    {
        List<MockMetadataValue> metadata = new ArrayList<>();

        String description = collectionService.getMetadata(collection, "introductory_text");
        String description_abstract = collectionService.getMetadata(collection, "short_description");
        String description_table = collectionService.getMetadata(collection, "side_bar_text");
        String identifier_uri = "http://hdl.handle.net/"
                + collection.getHandle();
        String provenance = collectionService.getMetadata(collection, "provenance_description");
        String rights = collectionService.getMetadata(collection, "copyright_text");
        String rights_license = collectionService.getMetadata(collection, "license");
        String title = collectionService.getMetadata(collection, "name");

        if (description != null)
        {
            metadata.add(createDCValue("description", null, description));
        }

        if (description_abstract != null)
        {
            metadata.add(createDCValue("description", "abstract", description_abstract));
        }

        if (description_table != null)
        {
            metadata.add(createDCValue("description", "tableofcontents", description_table));
        }

        if (identifier_uri != null)
        {
            metadata.add(createDCValue("identifier", "uri", identifier_uri));
        }

        if (provenance != null)
        {
            metadata.add(createDCValue("provenance", null, provenance));
        }

        if (rights != null)
        {
            metadata.add(createDCValue("rights", null, rights));
        }

        if (rights_license != null)
        {
            metadata.add(createDCValue("rights.license", null, rights_license));
        }

        if (title != null)
        {
            metadata.add(createDCValue("title", null, title));
        }

        return metadata;
    }

    /**
     * Generate a list of metadata elements for the given DSpace item.
     *
     * @param item
     *            The item to derive metadata from
     * @return list of metadata
     */
    protected List<MockMetadataValue> item2Metadata(Item item)
    {
        List<MetadataValue> dcvs = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY,
                Item.ANY);
        List<MockMetadataValue> result = new ArrayList<>();
        for (MetadataValue metadataValue : dcvs) {
            result.add(new MockMetadataValue(metadataValue));
        }

        return result;
    }

    protected MockMetadataValue createDCValue(String element, String qualifier, String value) {
        MockMetadataValue dcv = new MockMetadataValue();
        dcv.setSchema("dc");
        dcv.setElement(element);
        dcv.setQualifier(qualifier);
        dcv.setValue(value);
        return dcv;
    }

    // check for non-XML characters
    private String checkedString(String value)
    {
        if (value == null)
        {
            return null;
        }
        String reason = Verifier.checkCharacterData(value);
        if (reason == null)
        {
            return value;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Filtering out non-XML characters in string, reason=" + reason);
            }
            StringBuffer result = new StringBuffer(value.length());
            for (int i = 0; i < value.length(); ++i)
            {
                char c = value.charAt(i);
                if (Verifier.isXMLCharacter((int)c))
                {
                    result.append(c);
                }
            }
            return result.toString();
        }
    }
}
