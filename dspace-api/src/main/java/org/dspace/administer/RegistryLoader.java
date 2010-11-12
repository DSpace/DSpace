/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Loads the bitstream format and Dublin Core type registries into the database.
 * Intended for use as a command-line tool.
 * <P>
 * Example usage:
 * <P>
 * <code>RegistryLoader -bitstream bitstream-formats.xml</code>
 * <P>
 * <code>RegistryLoader -dc dc-types.xml</code>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class RegistryLoader
{
    /** log4j category */
    private static Logger log = Logger.getLogger(RegistryLoader.class);

    /**
     * For invoking via the command line
     * 
     * @param argv
     *            command-line arguments
     */
    public static void main(String[] argv) throws Exception
    {
        String usage = "Usage: " + RegistryLoader.class.getName()
                + " (-bitstream | -dc) registry-file.xml";

        Context context = null;

        try
        {
            context = new Context();

            // Can't update registries anonymously, so we need to turn off
            // authorisation
            context.setIgnoreAuthorization(true);

            // Work out what we're loading
            if (argv[0].equalsIgnoreCase("-bitstream"))
            {
                RegistryLoader.loadBitstreamFormats(context, argv[1]);
            }
            else if (argv[0].equalsIgnoreCase("-dc"))
            {
                loadDublinCoreTypes(context, argv[1]);
            }
            else
            {
                System.err.println(usage);
            }

            context.complete();

            System.exit(0);
        }
        catch (ArrayIndexOutOfBoundsException ae)
        {
            System.err.println(usage);

            if (context != null)
            {
                context.abort();
            }

            System.exit(1);
        }
        catch (Exception e)
        {
            log.fatal(LogManager.getHeader(context, "error_loading_registries",
                    ""), e);

            if (context != null)
            {
                context.abort();
            }

            System.err.println("Error: \n - " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Load Bitstream Format metadata
     * 
     * @param context
     *            DSpace context object
     * @param filename
     *            the filename of the XML file to load
     */
    public static void loadBitstreamFormats(Context context, String filename)
            throws SQLException, IOException, ParserConfigurationException,
            SAXException, TransformerException, AuthorizeException
    {
        Document document = loadXML(filename);

        // Get the nodes corresponding to formats
        NodeList typeNodes = XPathAPI.selectNodeList(document,
                "dspace-bitstream-types/bitstream-type");

        // Add each one as a new format to the registry
        for (int i = 0; i < typeNodes.getLength(); i++)
        {
            Node n = typeNodes.item(i);
            loadFormat(context, n);
        }

        log.info(LogManager.getHeader(context, "load_bitstream_formats",
                "number_loaded=" + typeNodes.getLength()));
    }

    /**
     * Process a node in the bitstream format registry XML file. The node must
     * be a "bitstream-type" node
     * 
     * @param context
     *            DSpace context object
     * @param node
     *            the node in the DOM tree
     */
    private static void loadFormat(Context context, Node node)
            throws SQLException, IOException, TransformerException,
            AuthorizeException
    {
        // Get the values
        String mimeType = getElementData(node, "mimetype");
        String shortDesc = getElementData(node, "short_description");
        String desc = getElementData(node, "description");

        String supportLevelString = getElementData(node, "support_level");
        int supportLevel = Integer.parseInt(supportLevelString);

        String internalString = getElementData(node, "internal");
        boolean internal = Boolean.valueOf(internalString).booleanValue();

        String[] extensions = getRepeatedElementData(node, "extension");

        // Create the format object
        BitstreamFormat format = BitstreamFormat.create(context);

        // Fill it out with the values
        format.setMIMEType(mimeType);
        format.setShortDescription(shortDesc);
        format.setDescription(desc);
        format.setSupportLevel(supportLevel);
        format.setInternal(internal);
        format.setExtensions(extensions);

        // Write to database
        format.update();
    }

    /**
     * Load Dublin Core types
     * 
     * @param context
     *            DSpace context object
     * @param filename
     *            the filename of the XML file to load
     * @throws NonUniqueMetadataException
     */
    public static void loadDublinCoreTypes(Context context, String filename)
            throws SQLException, IOException, ParserConfigurationException,
            SAXException, TransformerException, AuthorizeException,
            NonUniqueMetadataException
    {
        Document document = loadXML(filename);

        // Get the nodes corresponding to schemas
        NodeList schemaNodes = XPathAPI.selectNodeList(document,
                "/dspace-dc-types/dc-schema");

        // Add each schema
        for (int i = 0; i < schemaNodes.getLength(); i++)
        {
            Node n = schemaNodes.item(i);
            loadMDSchema(context, n);
        }
        
        // Get the nodes corresponding to fields
        NodeList typeNodes = XPathAPI.selectNodeList(document,
                "/dspace-dc-types/dc-type");

        // Add each one as a new field to the schema
        for (int i = 0; i < typeNodes.getLength(); i++)
        {
            Node n = typeNodes.item(i);
            loadDCType(context, n);
        }

        log.info(LogManager.getHeader(context, "load_dublin_core_types",
                "number_loaded=" + typeNodes.getLength()));
    }

    /**
     * Load Dublin Core Schemas
     * 
     * @param context
     * @param node
     */
    private static void loadMDSchema(Context context, Node node) 
    		throws TransformerException, SQLException, AuthorizeException, 
    		NonUniqueMetadataException
    {
    	// Get the values
        String shortname = getElementData(node, "name");
        String namespace = getElementData(node, "namespace");

        // Check if the schema exists already
        MetadataSchema schema = MetadataSchema.find(context, shortname);
        if (schema == null)
        {
        	// If not create it.
        	schema = new MetadataSchema();
        	schema.setNamespace(namespace);
        	schema.setName(shortname);
        	schema.create(context);
        }
    }
    
    /**
     * Process a node in the bitstream format registry XML file. The node must
     * be a "bitstream-type" node
     * 
     * @param context
     *            DSpace context object
     * @param node
     *            the node in the DOM tree
     * @throws NonUniqueMetadataException
     */
    private static void loadDCType(Context context, Node node)
            throws SQLException, IOException, TransformerException,
            AuthorizeException, NonUniqueMetadataException
    {
        // Get the values
        String schema = getElementData(node, "schema");
        String element = getElementData(node, "element");
        String qualifier = getElementData(node, "qualifier");
        String scopeNote = getElementData(node, "scope_note");

        // If the schema is not provided default to DC
        if (schema == null)
        {
            schema = MetadataSchema.DC_SCHEMA;
        }

        // Find the matching schema object
        MetadataSchema schemaObj = MetadataSchema.find(context, schema);
        
        MetadataField field = new MetadataField();
        field.setSchemaID(schemaObj.getSchemaID());
        field.setElement(element);
        field.setQualifier(qualifier);
        field.setScopeNote(scopeNote);
        field.create(context);
    }

    // ===================== XML Utility Methods =========================

    /**
     * Load in the XML from file.
     * 
     * @param filename
     *            the filename to load from
     * 
     * @return the DOM representation of the XML file
     */
    private static Document loadXML(String filename) throws IOException,
            ParserConfigurationException, SAXException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();

        return builder.parse(new File(filename));
    }

    /**
     * Get the CDATA of a particular element. For example, if the XML document
     * contains:
     * <P>
     * <code>
     * &lt;foo&gt;&lt;mimetype&gt;application/pdf&lt;/mimetype&gt;&lt;/foo&gt;
     * </code>
     * passing this the <code>foo</code> node and <code>mimetype</code> will
     * return <code>application/pdf</code>.
     * </P>
     * Why this isn't a core part of the XML API I do not know...
     * 
     * @param parentElement
     *            the element, whose child element you want the CDATA from
     * @param childName
     *            the name of the element you want the CDATA from
     * 
     * @return the CDATA as a <code>String</code>
     */
    private static String getElementData(Node parentElement, String childName)
            throws TransformerException
    {
        // Grab the child node
        Node childNode = XPathAPI.selectSingleNode(parentElement, childName);

        if (childNode == null)
        {
            // No child node, so no values
            return null;
        }

        // Get the #text
        Node dataNode = childNode.getFirstChild();

        if (dataNode == null)
        {
            return null;
        }

        // Get the data
        String value = dataNode.getNodeValue().trim();

        return value;
    }

    /**
     * Get repeated CDATA for a particular element. For example, if the XML
     * document contains:
     * <P>
     * <code>
     * &lt;foo&gt;
     *   &lt;bar&gt;val1&lt;/bar&gt;
     *   &lt;bar&gt;val2&lt;/bar&gt;
     * &lt;/foo&gt;
     * </code>
     * passing this the <code>foo</code> node and <code>bar</code> will
     * return <code>val1</code> and <code>val2</code>.
     * </P>
     * Why this also isn't a core part of the XML API I do not know...
     * 
     * @param parentElement
     *            the element, whose child element you want the CDATA from
     * @param childName
     *            the name of the element you want the CDATA from
     * 
     * @return the CDATA as a <code>String</code>
     */
    private static String[] getRepeatedElementData(Node parentElement,
            String childName) throws TransformerException
    {
        // Grab the child node
        NodeList childNodes = XPathAPI.selectNodeList(parentElement, childName);

        String[] data = new String[childNodes.getLength()];

        for (int i = 0; i < childNodes.getLength(); i++)
        {
            // Get the #text node
            Node dataNode = childNodes.item(i).getFirstChild();

            // Get the data
            data[i] = dataNode.getNodeValue().trim();
        }

        return data;
    }
}
