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
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
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

    protected static BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();

    /**
     * For invoking via the command line
     * 
     * @param argv the command line arguments given
     * @throws Exception if error
     */
    public static void main(String[] argv) throws Exception
    {
        String usage = "Usage: " + RegistryLoader.class.getName()
                + " (-bitstream | -metadata) registry-file.xml";

        Context context = null;

        try
        {
            context = new Context();

            // Can't update registries anonymously, so we need to turn off
            // authorisation
            context.turnOffAuthorisationSystem();

            // Work out what we're loading
            if (argv[0].equalsIgnoreCase("-bitstream"))
            {
                RegistryLoader.loadBitstreamFormats(context, argv[1]);
            }
            else if (argv[0].equalsIgnoreCase("-metadata"))
            {
                // Call MetadataImporter, as it handles Metadata schema updates
                MetadataImporter.loadRegistry(argv[1], true);
            }
            else
            {
                System.err.println(usage);
            }

            // Commit changes and close Context
            context.complete();

            System.exit(0);
        }
        catch (ArrayIndexOutOfBoundsException ae)
        {
            System.err.println(usage);

            System.exit(1);
        }
        catch (Exception e)
        {
            log.fatal(LogManager.getHeader(context, "error_loading_registries",
                    ""), e);

            System.err.println("Error: \n - " + e.getMessage());
            System.exit(1);
        }
        finally
        {
            // Clean up our context, if it still exists & it was never completed
            if(context!=null && context.isValid())
                context.abort();
        }
    }

    /**
     * Load Bitstream Format metadata
     * 
     * @param context
     *            DSpace context object
     * @param filename
     *            the filename of the XML file to load
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws TransformerException if transformer error
     * @throws ParserConfigurationException if config error
     * @throws AuthorizeException if authorization error
     * @throws SAXException if parser error
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
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws TransformerException if transformer error
     * @throws AuthorizeException if authorization error
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

        // Check if this format already exists in our registry (by mime type)
        BitstreamFormat exists = bitstreamFormatService.findByMIMEType(context, mimeType);
        
        // If not found by mimeType, check by short description (since this must also be unique)
        if(exists==null)
        {    
            exists = bitstreamFormatService.findByShortDescription(context, shortDesc);
        }
            
        // If it doesn't exist, create it..otherwise skip it.
        if(exists==null)
        {
            // Create the format object
            BitstreamFormat format = bitstreamFormatService.create(context);

            // Fill it out with the values
            format.setMIMEType(mimeType);
            bitstreamFormatService.setShortDescription(context, format, shortDesc);
            format.setDescription(desc);
            format.setSupportLevel(supportLevel);
            format.setInternal(internal);
            ArrayList<String> extensionList = new ArrayList<>();
            extensionList.addAll(Arrays.asList(extensions));
            format.setExtensions(extensionList);

            // Write to database
            bitstreamFormatService.update(context, format);
        }
    }

    // ===================== XML Utility Methods =========================

    /**
     * Load in the XML from file.
     * 
     * @param filename
     *            the filename to load from
     * @throws IOException if IO error
     * @throws ParserConfigurationException if config error
     * @throws SAXException if parser error
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
     * @throws TransformerException if transformer error
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
     * @throws TransformerException if transformer error
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
