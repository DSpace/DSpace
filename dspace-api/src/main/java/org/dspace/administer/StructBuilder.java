/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.xpath.XPathAPI;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class deals with importing community and collection structures from
 * an XML file.
 * 
 * The XML file structure needs to be:
 * {@code
 * <import_structure>
 *     <community>
 *         <name>....</name>
 *         <community>...</community>
 *         <collection>
 *             <name>....</name>
 *         </collection>
 *     </community>
 * </import_structure>
 * }
 * it can be arbitrarily deep, and supports all the metadata elements
 * that make up the community and collection metadata.  See the system
 * documentation for more details
 * 
 * @author Richard Jones
 *
 */

public class StructBuilder
{
    /** the output xml document which will contain updated information about the 
     * imported structure
     */
    private static org.jdom.Document xmlOutput = new org.jdom.Document(new Element("imported_structure"));
    
    /** a hashtable to hold metadata for the collection being worked on */
    private static Map<String, String> collectionMap = new HashMap<String, String>();
    
    /** a hashtable to hold metadata for the community being worked on */
    private static Map<String, String> communityMap = new HashMap<String, String>();

    protected static CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected static CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected static EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    /**
     * Main method to be run from the command line to import a structure into
     * DSpace
     * 
     * This is of the form:
     * 
     * {@code StructBuilder -f [xml source] -e [administrator email] -o [output file]}
     * 
     * The output file will contain exactly the same as the source xml document, but
     * with the handle for each imported item added as an attribute.
     *
     * @param argv the command line arguments given
     * @throws Exception if an error occurs
     */
    public static void main(String[] argv) 
        throws Exception
    {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption( "f", "file", true, "file");
        options.addOption( "e", "eperson", true, "eperson");
        options.addOption("o", "output", true, "output");
        
        CommandLine line = parser.parse( options, argv );
        
        String file = null;
        String eperson = null;
        String output = null;
        
        if (line.hasOption('f'))
        {
            file = line.getOptionValue('f');
        }
        
        if (line.hasOption('e'))
        {
            eperson = line.getOptionValue('e');
        }
        
        if (line.hasOption('o'))
        {
            output = line.getOptionValue('o');
        }
        
        if (output == null || eperson == null || file == null)
        {
            usage();
            System.exit(0);
        }
        
        // create a context
        Context context = new Context();
        
        // set the context
        context.setCurrentUser(ePersonService.findByEmail(context, eperson));
 
        // load the XML
        Document document = loadXML(file);
        
        // run the preliminary validation, to be sure that the the XML document
        // is properly structured
        validate(document);
        
        // load the mappings into the member variable hashmaps
        communityMap.put("name", "name");
        communityMap.put("description", "short_description");
        communityMap.put("intro", "introductory_text");
        communityMap.put("copyright", "copyright_text");
        communityMap.put("sidebar", "side_bar_text");
        
        collectionMap.put("name", "name");
        collectionMap.put("description", "short_description");
        collectionMap.put("intro", "introductory_text");
        collectionMap.put("copyright", "copyright_text");
        collectionMap.put("sidebar", "side_bar_text");
        collectionMap.put("license", "license");
        collectionMap.put("provenance", "provenance_description");
        
        // get the top level community list
        NodeList first = XPathAPI.selectNodeList(document, "/import_structure/community");
        
        // run the import starting with the top level communities
        Element[] elements = handleCommunities(context, first, null);
        
        // generate the output
        Element root = xmlOutput.getRootElement();
        for (int i = 0; i < elements.length; i++)
        {
            root.addContent(elements[i]);
        }
        
        // finally write the string into the output file
        try 
        {
            BufferedWriter out = new BufferedWriter(new FileWriter(output));
            out.write(new XMLOutputter().outputString(xmlOutput));
            out.close();
        } 
        catch (IOException e) 
        {
            System.out.println("Unable to write to output file " + output);
            System.exit(0);
        }
        
        context.complete();
    }
    
    /**
     * Output the usage information
     */
    private static void usage()
    {
        System.out.println("Usage: java StructBuilder -f <source XML file> -o <output file> -e <eperson email>");
        System.out.println("Communities will be created from the top level, and a map of communities to handles will be returned in the output file");
        return;
    }
    
    /**
     * Validate the XML document.  This method does not return, but if validation
     * fails it generates an error and ceases execution
     * 
     * @param    document    the XML document object
     * @throws TransformerException if transformer error
     * 
     */
    private static void validate(org.w3c.dom.Document document)
        throws TransformerException
    {
        StringBuffer err = new StringBuffer();
        boolean trip = false;
        
        err.append("The following errors were encountered parsing the source XML\n");
        err.append("No changes have been made to the DSpace instance\n\n");
        
        NodeList first = XPathAPI.selectNodeList(document, "/import_structure/community");
        if (first.getLength() == 0)
        {
            err.append("-There are no top level communities in the source document");
            System.out.println(err.toString());
            System.exit(0);
        }
        
        String errs = validateCommunities(first, 1);
        if (errs != null)
        {
            err.append(errs);
            trip = true;
        }
        
        if (trip)
        {
            System.out.println(err.toString());
            System.exit(0);
        }
    }
    
    /**
     * Validate the communities section of the XML document.  This returns a string
     * containing any errors encountered, or null if there were no errors
     * 
     * @param communities the NodeList of communities to validate
     * @param level the level in the XML document that we are at, for the purposes
     *             of error reporting
     * 
     * @return the errors that need to be generated by the calling method, or null if
     *             no errors.
     */
    private static String validateCommunities(NodeList communities, int level)
        throws TransformerException
    {
        StringBuffer err = new StringBuffer();
        boolean trip = false;
        String errs = null;
        
        for (int i = 0; i < communities.getLength(); i++)
        {
            Node n = communities.item(i);
            NodeList name = XPathAPI.selectNodeList(n, "name");
            if (name.getLength() != 1)
            {
                String pos = Integer.toString(i + 1);
                err.append("-The level " + level + " community in position " + pos);
                err.append(" does not contain exactly one name field\n");
                trip = true;
            }
            
            // validate sub communities
            NodeList subCommunities = XPathAPI.selectNodeList(n, "community");
            String comErrs = validateCommunities(subCommunities, level + 1);
            if (comErrs != null)
            {
                err.append(comErrs);
                trip = true;
            }
            
            // validate collections
            NodeList collections = XPathAPI.selectNodeList(n, "collection");
            String colErrs = validateCollections(collections, level + 1);
            if (colErrs != null)
            {
                err.append(colErrs);
                trip = true;
            }
        }
        
        if (trip)
        {
            errs = err.toString();
        }
        
        return errs;
    }
    
    /**
     * validate the collection section of the XML document.  This generates a
     * string containing any errors encountered, or returns null if no errors
     * 
     * @param collections a NodeList of collections to validate
     * @param level the level in the XML document for the purposes of error reporting
     * 
     * @return the errors to be generated by the calling method, or null if none
     */
    private static String validateCollections(NodeList collections, int level)
        throws TransformerException
    {
        StringBuffer err = new StringBuffer();
        boolean trip = false;
        String errs = null;
        
        for (int i = 0; i < collections.getLength(); i++)
        {
            Node n = collections.item(i);
            NodeList name = XPathAPI.selectNodeList(n, "name");
            if (name.getLength() != 1)
            {
                String pos = Integer.toString(i + 1);
                err.append("-The level " + level + " collection in position " + pos);
                err.append(" does not contain exactly one name field\n");
                trip = true;
            }
        }
        
        if (trip)
        {
            errs = err.toString();
        }
        
        return errs;
    }
    
    /**
     * Load in the XML from file.
     * 
     * @param filename
     *            the filename to load from
     * 
     * @return the DOM representation of the XML file
     */
    private static org.w3c.dom.Document loadXML(String filename) 
        throws IOException, ParserConfigurationException, SAXException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();

        org.w3c.dom.Document document = builder.parse(new File(filename));
        
        return document;
    }
    
    /**
     * Return the String value of a Node
     * 
     * @param node the node from which we want to extract the string value
     * 
     * @return the string value of the node
     */
    public static String getStringValue(Node node)
    {
        String value = node.getNodeValue();

        if (node.hasChildNodes())
        {
            Node first = node.getFirstChild();

            if (first.getNodeType() == Node.TEXT_NODE)
            {
                return first.getNodeValue().trim();
            }
        }

        return value;
    }
    
    /**
     * Take a node list of communities and build the structure from them, delegating
     * to the relevant methods in this class for sub-communities and collections
     * 
     * @param context the context of the request
     * @param communities a nodelist of communities to create along with their sub-structures
     * @param parent the parent community of the nodelist of communities to create
     * 
     * @return an element array containing additional information regarding the 
     *             created communities (e.g. the handles they have been assigned)
     */
    private static Element[] handleCommunities(Context context, NodeList communities, Community parent)
        throws TransformerException, SQLException, Exception
    {
        Element[] elements = new Element[communities.getLength()];
        
        for (int i = 0; i < communities.getLength(); i++)
        {
            Community community;
            Element element = new Element("community");
            
            // create the community or sub community
            if (parent != null)
            {
                community = communityService.create(parent, context);
            }
            else
            {
                community = communityService.create(null, context);
            }
            
            // default the short description to be an empty string
            communityService.setMetadata(context, community, "short_description", " ");
            
            // now update the metadata
            Node tn = communities.item(i);
            for (Map.Entry<String, String> entry : communityMap.entrySet())
            {
                NodeList nl = XPathAPI.selectNodeList(tn, entry.getKey());
                if (nl.getLength() == 1)
                {
                    communityService.setMetadata(context, community, entry.getValue(), getStringValue(nl.item(0)));
                }
            }
            
            // FIXME: at the moment, if the community already exists by name
            // then this will throw a PSQLException on a duplicate key
            // violation
            // Ideally we'd skip this row and continue to create sub
            // communities
            // and so forth where they don't exist, but it's proving
            // difficult
            // to isolate the community that already exists without hitting
            // the database directly.
            communityService.update(context, community);
            
            // build the element with the handle that identifies the new
            // community
            // along with all the information that we imported here
            // This looks like a lot of repetition of getting information
            // from above
            // but it's here to keep it separate from the create process in
            // case
            // we want to move it or make it switchable later
            element.setAttribute("identifier", community.getHandle());
            
            Element nameElement = new Element("name");
            nameElement.setText(communityService.getMetadata(community, "name"));
            element.addContent(nameElement);
            
            if (communityService.getMetadata(community, "short_description") != null)
            {
                Element descriptionElement = new Element("description");
                descriptionElement.setText(communityService.getMetadata(community, "short_description"));
                element.addContent(descriptionElement);
            }
            
            if (communityService.getMetadata(community, "introductory_text") != null)
            {
                Element introElement = new Element("intro");
                introElement.setText(communityService.getMetadata(community, "introductory_text"));
                element.addContent(introElement);
            }
            
            if (communityService.getMetadata(community, "copyright_text") != null)
            {
                Element copyrightElement = new Element("copyright");
                copyrightElement.setText(communityService.getMetadata(community, "copyright_text"));
                element.addContent(copyrightElement);
            }
            
            if (communityService.getMetadata(community, "side_bar_text") != null)
            {
                Element sidebarElement = new Element("sidebar");
                sidebarElement.setText(communityService.getMetadata(community, "side_bar_text"));
                element.addContent(sidebarElement);
            }
            
            // handle sub communities
            NodeList subCommunities = XPathAPI.selectNodeList(tn, "community");
            Element[] subCommunityElements = handleCommunities(context, subCommunities, community);
            
            // handle collections
            NodeList collections = XPathAPI.selectNodeList(tn, "collection");
            Element[] collectionElements = handleCollections(context, collections, community);
            
            int j;
            for (j = 0; j < subCommunityElements.length; j++)
            {
                element.addContent(subCommunityElements[j]);
            }
            for (j = 0; j < collectionElements.length; j++)
            {
                element.addContent(collectionElements[j]);
            }
            
            elements[i] = element;
        }
        
        return elements;
    }
    
    /**
     *  Take a node list of collections and create the structure from them
     * 
     * @param context the context of the request
     * @param collections the node list of collections to be created
     * @param parent the parent community to whom the collections belong
     * 
     * @return an Element array containing additional information about the
     *             created collections (e.g. the handle)
     */
    private static Element[] handleCollections(Context context, NodeList collections, Community parent)
        throws TransformerException, SQLException, AuthorizeException, IOException, Exception
    {
        Element[] elements = new Element[collections.getLength()];
        
        for (int i = 0; i < collections.getLength(); i++)
        {
            Element element = new Element("collection");
            Collection collection = collectionService.create(context, parent);
            
            // default the short description to the empty string
            collectionService.setMetadata(context, collection, "short_description", " ");
            
            // import the rest of the metadata
            Node tn = collections.item(i);
            for (Map.Entry<String, String> entry : collectionMap.entrySet())
            {
                NodeList nl = XPathAPI.selectNodeList(tn, entry.getKey());
                if (nl.getLength() == 1)
                {
                    collectionService.setMetadata(context, collection, entry.getValue(), getStringValue(nl.item(0)));
                }
            }

            collectionService.update(context, collection);
            
            element.setAttribute("identifier", collection.getHandle());
            
            Element nameElement = new Element("name");
            nameElement.setText(collectionService.getMetadata(collection, "name"));
            element.addContent(nameElement);
            
            if (collectionService.getMetadata(collection, "short_description") != null)
            {
                Element descriptionElement = new Element("description");
                descriptionElement.setText(collectionService.getMetadata(collection, "short_description"));
                element.addContent(descriptionElement);
            }
            
            if (collectionService.getMetadata(collection, "introductory_text") != null)
            {
                Element introElement = new Element("intro");
                introElement.setText(collectionService.getMetadata(collection, "introductory_text"));
                element.addContent(introElement);
            }
            
            if (collectionService.getMetadata(collection, "copyright_text") != null)
            {
                Element copyrightElement = new Element("copyright");
                copyrightElement.setText(collectionService.getMetadata(collection, "copyright_text"));
                element.addContent(copyrightElement);
            }
            
            if (collectionService.getMetadata(collection, "side_bar_text") != null)
            {
                Element sidebarElement = new Element("sidebar");
                sidebarElement.setText(collectionService.getMetadata(collection, "side_bar_text"));
                element.addContent(sidebarElement);
            }
            
            if (collectionService.getMetadata(collection, "license") != null)
            {
                Element sidebarElement = new Element("license");
                sidebarElement.setText(collectionService.getMetadata(collection, "license"));
                element.addContent(sidebarElement);
            }
            
            if (collectionService.getMetadata(collection, "provenance_description") != null)
            {
                Element sidebarElement = new Element("provenance");
                sidebarElement.setText(collectionService.getMetadata(collection, "provenance_description"));
                element.addContent(sidebarElement);
            }
            
            elements[i] = element;
        }
        
        return elements;
    }
    
}
