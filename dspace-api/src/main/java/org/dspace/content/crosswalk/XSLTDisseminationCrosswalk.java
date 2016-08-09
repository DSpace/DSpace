/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Verifier;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configurable XSLT-driven dissemination Crosswalk
 * <p>
 * See the XSLTCrosswalk superclass for details on configuration.
 * <p>
 * <h3>Additional Configuration of Dissemination crosswalk:</h3>
 * The disseminator also needs to be configured with an XML Namespace
 * (including prefix and URI) and an XML Schema for output format.  This
 * is configured on additional properties in the DSpace Configuration, i.e.:
 * <pre>
 *   crosswalk.dissemination.<i>PluginName</i>.namespace.<i>Prefix</i> = <i>namespace-URI</i>
 *   crosswalk.dissemination.<i>PluginName</i>.schemaLocation = <i>schemaLocation value</i>
 *   crosswalk.dissemination.<i>PluginName</i>.preferList = <i>boolean</i> (default is false)
 * </pre>
 * For example:
 * <pre>
 *   crosswalk.dissemination.qdc.namespace.dc = http://purl.org/dc/elements/1.1/
 *   crosswalk.dissemination.qdc.namespace.dcterms = http://purl.org/dc/terms/
 *   crosswalk.dissemination.qdc.schemaLocation = \
 *      http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2003/04/02/qualifieddc.xsd
 *   crosswalk.dissemination.qdc.preferList = true
 * </pre>
 *
 * @author Larry Stone
 * @author Scott Phillips
 * @author Pascal-Nicolas Becker
 * @see XSLTCrosswalk
 */
public class XSLTDisseminationCrosswalk
    extends XSLTCrosswalk
    implements ParameterizedDisseminationCrosswalk
{
    /** log4j category */
    private static final Logger LOG = LoggerFactory.getLogger(XSLTDisseminationCrosswalk.class);

    /** DSpace context, will be created if XSLTDisseminationCrosswalk had been started by command-line. */
    private static Context context;

    private static final String DIRECTION = "dissemination";

    protected static final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private static final String aliases[] = makeAliases(DIRECTION);

    public static String[] getPluginNames()
    {
        return (String[]) ArrayUtils.clone(aliases);
    }

    // namespace and schema; don't worry about initializing these
    // until there's an instance, so do it in constructor.
    private String schemaLocation = null;

    private Namespace namespaces[] = null;

    private boolean preferList = false;

    // load the namespace and schema from config
    private void init()
        throws CrosswalkInternalException
    {
        if (namespaces != null || schemaLocation != null)
        {
            return;
        }
        String myAlias = getPluginInstanceName();
        if (myAlias == null)
        {
            LOG.error("Must use PluginService to instantiate XSLTDisseminationCrosswalk so the class knows its name.");
            throw new CrosswalkInternalException("Must use PluginService to instantiate XSLTDisseminationCrosswalk so the class knows its name.");
        }

        // all configs for this plugin instance start with this:
        String prefix = CONFIG_PREFIX+DIRECTION+"."+myAlias+".";

        // get the schema location string, should already be in the
        // right format for value of "schemaLocation" attribute.
        schemaLocation = ConfigurationManager.getProperty(prefix+"schemaLocation");
        if (schemaLocation == null)
        {
            LOG.warn("No schemaLocation for crosswalk="+myAlias+", key="+prefix+"schemaLocation");
        }

        // sanity check: schemaLocation should have space.
        else if (schemaLocation.length() > 0 && schemaLocation.indexOf(' ') < 0)
        {
            LOG.warn("Possible INVALID schemaLocation (no space found) for crosswalk="+
                      myAlias+", key="+prefix+"schemaLocation"+
                      "\n\tCorrect format is \"{namespace} {schema-URL}\"");
        }

        // grovel for namespaces of the form:
        //  crosswalk.diss.{PLUGIN_NAME}.namespace.{PREFIX} = {URI}
        String nsPrefix = prefix + "namespace.";
        Enumeration<String> pe = (Enumeration<String>)ConfigurationManager.propertyNames();
        List<Namespace> nsList = new ArrayList<>();
        while (pe.hasMoreElements())
        {
            String key = pe.nextElement();
            if (key.startsWith(nsPrefix))
            {
                nsList.add(Namespace.getNamespace(key.substring(nsPrefix.length()),
                        ConfigurationManager.getProperty(key)));
            }
        }
        namespaces = nsList.toArray(new Namespace[nsList.size()]);

        preferList = ConfigurationManager.getBooleanProperty(prefix+"preferList", false);
    }

    /**
     * Return the namespace used by this crosswalk.
     *
     * @see DisseminationCrosswalk
     */
    @Override
    public Namespace[] getNamespaces()
    {
        try
        {
            init();
        }
        catch (CrosswalkInternalException e)
        {
            LOG.error(e.toString());
        }
        return (Namespace[]) ArrayUtils.clone(namespaces);
    }

    /**
     * Return the schema location used by this crosswalk.
     *
     * @see DisseminationCrosswalk
     */
    @Override
    public String getSchemaLocation()
    {
        try
        {
            init();
        }
        catch (CrosswalkInternalException e)
        {
            LOG.error(e.toString());
        }
        return schemaLocation;
    }

    @Override
    public Element disseminateElement(Context context, DSpaceObject dso)
            throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        return disseminateElement(context, dso, new HashMap());
    }

    @Override
    public Element disseminateElement(Context context, DSpaceObject dso,
            Map<String, String> parameters)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        int type = dso.getType();
        if (!(type == Constants.ITEM ||
              type == Constants.COLLECTION ||
              type == Constants.COMMUNITY))
        {
            throw new CrosswalkObjectNotSupported("XSLTDisseminationCrosswalk can only crosswalk items, collections, and communities.");
        }

        init();

        Transformer xform = getTransformer(DIRECTION);
        if (xform == null)
        {
            throw new CrosswalkInternalException("Failed to initialize transformer, probably error loading stylesheet.");
        }

        for (Map.Entry<String, String> parameter : parameters.entrySet())
        {
            LOG.debug("Setting parameter {} to {}", parameter.getKey(), parameter.getValue());
            xform.setParameter(parameter.getKey(), parameter.getValue());
        }

        try
        {
            Document ddim = new Document(createDIM(dso));
            JDOMResult result = new JDOMResult();
            xform.transform(new JDOMSource(ddim), result);
            Element root = result.getDocument().getRootElement();
            root.detach();
            return root;
        }
        catch (TransformerException e)
        {
            LOG.error("Got error: "+e.toString());
            throw new CrosswalkInternalException("XSL translation failed: "+e.toString(), e);
        }
    }

    /**
     * Disseminate the DSpace item, collection, or community.
     *
     * @param context context
     * @throws CrosswalkException crosswalk error
     * @throws IOException if IO error 
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @see DisseminationCrosswalk
     */
    @Override
    public List<Element> disseminateList(Context context, DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        int type = dso.getType();
        if (!(type == Constants.ITEM ||
              type == Constants.COLLECTION ||
              type == Constants.COMMUNITY))
        {
            throw new CrosswalkObjectNotSupported("XSLTDisseminationCrosswalk can only crosswalk a items, collections, and communities.");
        }

        init();

        Transformer xform = getTransformer(DIRECTION);
        if (xform == null)
        {
            throw new CrosswalkInternalException("Failed to initialize transformer, probably error loading stylesheet.");
        }

        try
        {
            JDOMResult result = new JDOMResult();
            xform.transform(new JDOMSource(createDIM(dso).getChildren()), result);
            return result.getResult();
        }
        catch (TransformerException e)
        {
            LOG.error("Got error: "+e.toString());
            throw new CrosswalkInternalException("XSL translation failed: "+e.toString(), e);
        }
    }

    /**
     * Determine is this crosswalk can dessiminate the given object.
     *
     * @see DisseminationCrosswalk
     */
    @Override
    public boolean canDisseminate(DSpaceObject dso)
    {
        return dso.getType() == Constants.ITEM;
    }

    /**
     * return true if this crosswalk prefers the list form over an single
     * element, otherwise false.
     *
     * @see DisseminationCrosswalk
     */
    @Override
    public boolean preferList()
    {
        try
        {
            init();
        }
        catch (CrosswalkInternalException e)
        {
            LOG.error(e.toString());
        }
        return preferList;
    }

    /**
     * Generate an intermediate representation of a DSpace object.
     *
     * @param dso The dspace object to build a representation of.
     * @param dcvs list of metadata
     * @return element
     */
    public static Element createDIM(DSpaceObject dso, List<MockMetadataValue> dcvs)
    {
        Element dim = new Element("dim", DIM_NS);
        String type = Constants.typeText[dso.getType()];
        dim.setAttribute("dspaceType",type);

        for (int i = 0; i < dcvs.size(); i++)
        {
            MockMetadataValue dcv = dcvs.get(i);
            Element field =
            createField(dcv.getSchema(), dcv.getElement(), dcv.getQualifier(),
                    dcv.getLanguage(), dcv.getValue(), dcv.getAuthority(), dcv.getConfidence());
            dim.addContent(field);
        }
        return dim;
    }

    /**
     * Generate an intermediate representation of a DSpace object.
     *
     * @param dso The dspace object to build a representation of.
     * @return element
     */
    public static Element createDIM(DSpaceObject dso)
    {
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item) dso;
            return createDIM(dso, item2Metadata(item));
        }
        else
        {
            Element dim = new Element("dim", DIM_NS);
            String type = Constants.typeText[dso.getType()];
            dim.setAttribute("dspaceType",type);

            if (dso.getType() == Constants.COLLECTION)
            {
                Collection collection = (Collection) dso;

                String description = collectionService.getMetadata(collection, "introductory_text");
                String description_abstract = collectionService.getMetadata(collection, "short_description");
                String description_table = collectionService.getMetadata(collection, "side_bar_text");
                String identifier_uri = "hdl:" + collection.getHandle();
                String provenance = collectionService.getMetadata(collection, "provenance_description");
                String rights = collectionService.getMetadata(collection, "copyright_text");
                String rights_license = collectionService.getMetadata(collection, "license");
                String title = collectionService.getMetadata(collection, "name");

                dim.addContent(createField("dc","description",null,null,description));
                dim.addContent(createField("dc","description","abstract",null,description_abstract));
                dim.addContent(createField("dc","description","tableofcontents",null,description_table));
                dim.addContent(createField("dc","identifier","uri",null,identifier_uri));
                dim.addContent(createField("dc","provenance",null,null,provenance));
                dim.addContent(createField("dc","rights",null,null,rights));
                dim.addContent(createField("dc","rights","license",null,rights_license));
                dim.addContent(createField("dc","title",null,null,title));
            }
            else if (dso.getType() == Constants.COMMUNITY)
            {
                Community community = (Community) dso;

                String description = communityService.getMetadata(community, "introductory_text");
                String description_abstract = communityService.getMetadata(community, "short_description");
                String description_table = communityService.getMetadata(community, "side_bar_text");
                String identifier_uri = "hdl:" + community.getHandle();
                String rights = communityService.getMetadata(community, "copyright_text");
                String title = communityService.getMetadata(community, "name");

                dim.addContent(createField("dc","description",null,null,description));
                dim.addContent(createField("dc","description","abstract",null,description_abstract));
                dim.addContent(createField("dc","description","tableofcontents",null,description_table));
                dim.addContent(createField("dc","identifier","uri",null,identifier_uri));
                dim.addContent(createField("dc","rights",null,null,rights));
                dim.addContent(createField("dc","title",null,null,title));
            }
            else if (dso.getType() == Constants.SITE)
            {
                Site site = (Site) dso;

                String identifier_uri = "hdl:" + site.getHandle();
                String title = site.getName();
                String url = site.getURL();

                //FIXME: adding two URIs for now (site handle and URL), in case site isn't using handles
                dim.addContent(createField("dc","identifier","uri",null,identifier_uri));
                dim.addContent(createField("dc","identifier","uri",null,url));
                dim.addContent(createField("dc","title",null,null,title));
            }
            // XXX FIXME: Nothing to crosswalk for bitstream?
            return dim;
        }
    }

    protected static List<MockMetadataValue> item2Metadata(Item item)
    {
        List<MetadataValue> dcvs = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY,
                Item.ANY);
        List<MockMetadataValue> result = new ArrayList<>();
        for (MetadataValue metadataValue : dcvs) {
            result.add(new MockMetadataValue(metadataValue));
        }

        return result;
    }



     /**
     * Create a new DIM field element with the given attributes.
     *
     * @param schema The schema the DIM field belongs to.
     * @param element The element the DIM field belongs to.
     * @param qualifier The qualifier the DIM field belongs to.
     * @param language The language the DIM field belongs to.
     * @param value The value of the DIM field.
     * @return A new DIM field element
     */
    private static Element createField(String schema, String element, String qualifier, String language, String value)
    {
        return createField(schema, element, qualifier, language, value, null, -1);
    }

    /**
     * Create a new DIM field element with the given attributes.
     *
     * @param schema The schema the DIM field belongs to.
     * @param element The element the DIM field belongs to.
     * @param qualifier The qualifier the DIM field belongs to.
     * @param language The language the DIM field belongs to.
     * @param value The value of the DIM field.
     * @param authority The authority
     * @param confidence confidence in the authority
     * @return A new DIM field element
     */
    private static Element createField(String schema, String element, String qualifier, String language, String value,
                                        String authority, int confidence)
    {
        Element field = new Element("field",DIM_NS);
        field.setAttribute("mdschema",schema);
        field.setAttribute("element",element);
        if (qualifier != null)
        {
            field.setAttribute("qualifier", qualifier);
        }
        if (language != null)
        {
            field.setAttribute("lang", language);
        }

        field.setText(checkedString(value));

        if (authority != null)
        {
            field.setAttribute("authority", authority);
            field.setAttribute("confidence", Choices.getConfidenceText(confidence));
        }

        return field;
    }

    // Return string with non-XML characters (i.e. low control chars) excised.
    private static String checkedString(String value)
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
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Filtering out non-XML characters in string, reason=" + reason);
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

    /**
     * Simple command-line rig for testing the DIM output of a stylesheet.
     * Usage:  {@code java XSLTDisseminationCrosswalk  <crosswalk-name> <handle> [output-file]}
     * @param argv arguments
     * @throws Exception if error
     */
    public static void main(String[] argv) throws Exception
    {
        LOG.error("started.");
        if (argv.length < 2 || argv.length > 3)
        {
            System.err.println("Usage:  java XSLTDisseminationCrosswalk <crosswalk-name> <handle> [output-file]");
            LOG.error("You started Dissemination Crosswalk Test/Export with a wrong number of parameters.");
            System.exit(1);
        }

        String xwalkname = argv[0];
        String handle = argv[1];
        OutputStream out = System.out;
        if (argv.length > 2)
        {
            try
            {
                out = new FileOutputStream(argv[2]);
            }
            catch (FileNotFoundException e)
            {
                System.err.format("Can't write to the specified file: %s%n",
                        e.getMessage());
                System.err.println("Will write output to stdout.");
            }
        }
        
        DisseminationCrosswalk xwalk
                = (DisseminationCrosswalk) CoreServiceFactory.getInstance()
                        .getPluginService()
                        .getNamedPlugin(DisseminationCrosswalk.class, xwalkname);
        if (xwalk == null)
        {
            System.err.format("Error: Cannot find a DisseminationCrosswalk plugin for: \"%s\"%n", xwalkname);
            LOG.error("Cannot find the Dissemination Crosswalk plugin.");
            System.exit(1);
        }

        context = new Context();
        context.turnOffAuthorisationSystem();

        DSpaceObject dso = null;
        try
        {
            dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, handle);
        }
        catch (SQLException e)
        {
            System.err.println("Error: A problem with the database connection occurred, check logs for further information.");
            System.exit(1);
        }

        if (null == dso)
        {
            System.err.format("Can't find a DSpaceObject with the handle \"%s\"%n", handle);
            System.exit(1);
        }

        if (!xwalk.canDisseminate(dso))
        {
            System.err.println("Dissemination Crosswalk can't disseminate this DSpaceObject.");
            LOG.error("Dissemination Crosswalk can't disseminate this DSpaceObject.");
            System.exit(1);
        }

        Element root = null;
        try
        {
            root = xwalk.disseminateElement(context, dso);
        }
        catch (CrosswalkException | IOException | SQLException | AuthorizeException e)
        {
            // as this script is for testing dissemination crosswalks, we want
            // verbose information in case of an exception.
            System.err.println("An error occurred while processing the dissemination crosswalk.");
            System.err.println("=== Error Message ===");
            System.err.println(e.getMessage());
            System.err.println("===  Stack Trace  ===");
            e.printStackTrace(System.err);
            System.err.println("=====================");
            LOG.error("Caught: {}.", e.toString());
            LOG.error(e.getMessage());
            CharArrayWriter traceWriter = new CharArrayWriter(2048);
            e.printStackTrace(new PrintWriter(traceWriter));
            LOG.error(traceWriter.toString());
            System.exit(1);
        }

        try
        {
            XMLOutputter xmlout = new XMLOutputter(Format.getPrettyFormat());
            xmlout.output(new Document(root), out);
        }
        catch (Exception e)
        {
            // as this script is for testing dissemination crosswalks, we want
            // verbose information in case of an exception.
            System.err.println("An error occurred after processing the dissemination crosswalk.");
            System.err.println("The error occurred while trying to print the generated XML.");
            System.err.println("=== Error Message ===");
            System.err.println(e.getMessage());
            System.err.println("===  Stack Trace  ===");
            e.printStackTrace(System.err);
            System.err.println("=====================");
            LOG.error("Caught: {}.", e.toString());
            LOG.error(e.getMessage());
            CharArrayWriter traceWriter = new CharArrayWriter(2048);
            e.printStackTrace(new PrintWriter(traceWriter));
            LOG.error(traceWriter.toString());
            System.exit(1);
        }

        context.complete();
        if (out instanceof FileOutputStream)
        {
            out.close();
        }
    }
}
