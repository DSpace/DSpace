/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.xpath.XPathAPI;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.jdom.Element;
import org.jdom.output.Format;
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
 * <pre>{@code
 * <import_structure>
 *   <community>
 *     <name>....</name>
 *     <community>...</community>
 *     <collection>
 *       <name>....</name>
 *     </collection>
 *   </community>
 * </import_structure>
 * }</pre>
 * <p>
 * It can be arbitrarily deep, and supports all the metadata elements
 * that make up the community and collection metadata.  See the system
 * documentation for more details.
 *
 * @author Richard Jones
 */

public class StructBuilder {
    /** Name of the root element for the document to be imported. */
    static final String INPUT_ROOT = "import_structure";

    /*
     * Name of the root element for the document produced by importing.
     * Community and collection elements are annotated with their identifiers.
     */
    static final String RESULT_ROOT = "imported_structure";

    /**
     * A table to hold metadata for the collection being worked on.
     */
    private static final Map<String, String> collectionMap = new HashMap<>();

    /**
     * A table to hold metadata for the community being worked on.
     */
    private static final Map<String, String> communityMap = new HashMap<>();

    protected static CommunityService communityService
            = ContentServiceFactory.getInstance().getCommunityService();
    protected static CollectionService collectionService
            = ContentServiceFactory.getInstance().getCollectionService();
    protected static EPersonService ePersonService
            = EPersonServiceFactory.getInstance().getEPersonService();

    /**
     * Default constructor
     */
    private StructBuilder() { }

    /**
     * Main method to be run from the command line to import a structure into
     * DSpacee or export existing structure to a file.The command is of the form:
     *
     * <p>{@code StructBuilder -f [XML source] -e [administrator email] -o [output file]}
     *
     * <p>to import, or
     *
     * <p>{@code StructBuilder -x -e [administrator email] -o [output file]}</p>
     *
     * <p>to export.  The output will contain exactly the same as the source XML
     * document, but with the Handle for each imported item added as an attribute.
     *
     *
     * @param argv command line arguments.
     * @throws ParserConfigurationException passed through.
     * @throws SQLException passed through.
     * @throws FileNotFoundException if input or output could not be opened.
     * @throws TransformerException if the input document is invalid.
     */
    public static void main(String[] argv)
            throws ParserConfigurationException, SQLException,
            FileNotFoundException, IOException, TransformerException {
        // Define command line options.
        Options options = new Options();

        options.addOption("h", "help", false, "Print this help message.");
        options.addOption("?", "help");
        options.addOption("x", "export", false, "Export the current structure as XML.");

        options.addOption(Option.builder("e").longOpt("eperson")
                .desc("User who is manipulating the repository's structure.")
                .hasArg().argName("eperson").required().build());

        options.addOption(Option.builder("f").longOpt("file")
                .desc("File of new structure information.")
                .hasArg().argName("input").build());

        options.addOption(Option.builder("o").longOpt("output")
                .desc("File to receive the structure map ('-' for standard out).")
                .hasArg().argName("output").required().build());

        // Parse the command line.
        CommandLineParser parser = new DefaultParser();
        CommandLine line = null;
        try {
            line = parser.parse(options, argv);
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            usage(options);
            System.exit(1);
        }

        // If the user asked for help, give it and exit.
        if (line.hasOption('h') || line.hasOption('?')) {
            giveHelp(options);
            System.exit(0);
        }

        // Otherwise, analyze the command.
        // Must be import or export.
        if (!(line.hasOption('f') || line.hasOption('x'))) {
            giveHelp(options);
            System.exit(1);
        }

        // Open the output stream.
        String output = line.getOptionValue('o');
        OutputStream outputStream;
        if ("-".equals(output)) {
            outputStream = System.out;
        } else {
            outputStream = new FileOutputStream(output);
        }

        // create a context
        Context context = new Context();

        // set the context.
        String eperson = line.getOptionValue('e');
        try {
            context.setCurrentUser(ePersonService.findByEmail(context, eperson));
        } catch (SQLException ex) {
            System.err.format("That user could not be found:  %s%n", ex.getMessage());
            System.exit(1);
        }

        // Export? Import?
        if (line.hasOption('x')) { // export
            exportStructure(context, outputStream);
        } else { // Must be import
            String input = line.getOptionValue('f');
            if (null == input) {
                usage(options);
                System.exit(1);
            }

            InputStream inputStream;
            if ("-".equals(input)) {
                inputStream = System.in;
            } else {
                inputStream = new FileInputStream(input);
            }

            importStructure(context, inputStream, outputStream);
            // save changes from import
            context.complete();
        }
        System.exit(0);
    }

    /**
     * Import new Community/Collection structure.
     *
     * @param context
     * @param input XML which describes the new communities and collections.
     * @param output input, annotated with the new objects' identifiers.
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws TransformerException
     * @throws SQLException
     */
    static void importStructure(Context context, InputStream input, OutputStream output)
            throws IOException, ParserConfigurationException, SQLException, TransformerException {

        // load the XML
        Document document = null;
        try {
            document = loadXML(input);
        } catch (IOException ex) {
            System.err.format("The input document could not be read:  %s%n", ex.getMessage());
            System.exit(1);
        } catch (SAXException ex) {
            System.err.format("The input document could not be parsed:  %s%n", ex.getMessage());
            System.exit(1);
        }

        // run the preliminary validation, to be sure that the the XML document
        // is properly structured.
        try {
            validate(document);
        } catch (TransformerException ex) {
            System.err.format("The input document is invalid:  %s%n", ex.getMessage());
            System.exit(1);
        }

        // Check for 'identifier' attributes -- possibly output by this class.
        NodeList identifierNodes = XPathAPI.selectNodeList(document, "//*[@identifier]");
        if (identifierNodes.getLength() > 0) {
            System.err.println("The input document has 'identifier' attributes, which will be ignored.");
        }

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

        Element[] elements = new Element[]{};
        try {
            // get the top level community list
            NodeList first = XPathAPI.selectNodeList(document, "/import_structure/community");

            // run the import starting with the top level communities
            elements = handleCommunities(context, first, null);
        } catch (TransformerException ex) {
            System.err.format("Input content not understood:  %s%n", ex.getMessage());
            System.exit(1);
        } catch (AuthorizeException ex) {
            System.err.format("Not authorized:  %s%n", ex.getMessage());
            System.exit(1);
        }

        // generate the output
        final Element root = new Element(RESULT_ROOT);

        for (Element element : elements) {
            root.addContent(element);
        }

        // finally write the string into the output file.
        final org.jdom.Document xmlOutput = new org.jdom.Document(root);
        try {
            new XMLOutputter().output(xmlOutput, output);
        } catch (IOException e) {
            System.out.printf("Unable to write to output file %s:  %s%n",
                    output, e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Add a single community, and its children, to the Document.
     *
     * @param community
     * @return a fragment representing this Community.
     */
    private static Element exportACommunity(Community community) {
        // Export this Community.
        Element element = new Element("community");
        element.setAttribute("identifier", community.getHandle());
        element.addContent(new Element("name").setText(community.getName()));
        element.addContent(new Element("description")
                .setText(communityService.getMetadataFirstValue(community,
                        MetadataSchemaEnum.DC.getName(), "description", "abstract", Item.ANY)));
        element.addContent(new Element("intro")
                .setText(communityService.getMetadataFirstValue(community,
                        MetadataSchemaEnum.DC.getName(), "description", null, Item.ANY)));
        element.addContent(new Element("copyright")
                .setText(communityService.getMetadataFirstValue(community,
                        MetadataSchemaEnum.DC.getName(), "rights", null, Item.ANY)));
        element.addContent(new Element("sidebar")
                .setText(communityService.getMetadataFirstValue(community,
                        MetadataSchemaEnum.DC.getName(), "description", "tableofcontents", Item.ANY)));

        // Export this Community's Community children.
        for (Community subCommunity : community.getSubcommunities()) {
            element.addContent(exportACommunity(subCommunity));
        }

        // Export this Community's Collection children.
        for (Collection collection : community.getCollections()) {
            element.addContent(exportACollection(collection));
        }

        return element;
    }

    /**
     * Add a single Collection to the Document.
     *
     * @param collection
     * @return a fragment representing this Collection.
     */
    private static Element exportACollection(Collection collection) {
        // Export this Collection.
        Element element = new Element("collection");
        element.setAttribute("identifier", collection.getHandle());
        element.addContent(new Element("name").setText(collection.getName()));
        element.addContent(new Element("description")
                .setText(collectionService.getMetadataFirstValue(collection,
                        MetadataSchemaEnum.DC.getName(), "description", "abstract", Item.ANY)));
        element.addContent(new Element("intro")
                .setText(collectionService.getMetadataFirstValue(collection,
                        MetadataSchemaEnum.DC.getName(), "description", null, Item.ANY)));
        element.addContent(new Element("copyright")
                .setText(collectionService.getMetadataFirstValue(collection,
                        MetadataSchemaEnum.DC.getName(), "rights", null, Item.ANY)));
        element.addContent(new Element("sidebar")
                .setText(collectionService.getMetadataFirstValue(collection,
                        MetadataSchemaEnum.DC.getName(), "description", "tableofcontents", Item.ANY)));
        element.addContent(new Element("license")
                .setText(collectionService.getMetadataFirstValue(collection,
                        MetadataSchemaEnum.DC.getName(), "rights", "license", Item.ANY)));
        // Provenance is special:  multivalued
        for (MetadataValue value : collectionService.getMetadata(collection,
                MetadataSchemaEnum.DC.getName(), "provenance", null, Item.ANY)) {
            element.addContent(new Element("provenance")
                    .setText(value.getValue()));
        }

        return element;
    }

    /**
     * Write out the existing Community/Collection structure.
     */
    static void exportStructure(Context context, OutputStream output) {
        // Build a document from the Community/Collection hierarchy.
        Element rootElement = new Element(INPUT_ROOT);  // To be read by importStructure, perhaps

        List<Community> communities = null;
        try {
            communities = communityService.findAllTop(context);
        } catch (SQLException ex) {
            System.out.printf("Unable to get the list of top-level communities:  %s%n",
                    ex.getMessage());
            System.exit(1);
        }

        for (Community community : communities) {
            rootElement.addContent(exportACommunity(community));
        }

        // Now write the structure out.
        org.jdom.Document xmlOutput = new org.jdom.Document(rootElement);
        try {
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            outputter.output(xmlOutput, output);
        } catch (IOException e) {
            System.out.printf("Unable to write to output file %s:  %s%n",
                    output, e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Output the usage information.
     */
    private static void usage(Options options) {
        HelpFormatter helper = new HelpFormatter();
        try (PrintWriter writer = new PrintWriter(System.out);) {
            helper.printUsage(writer, 80/* FIXME Magic */,
                    "structure-builder", options);
        }
    }

    /**
     * Help the user more.
     */
    private static void giveHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("struct-builder",
                "Import or export Community/Collection structure.",
                options,
                "When importing (-f), communities will be created from the "
                    + "top level, and a map of communities to handles will "
                    + "be returned in the output file.  When exporting (-x),"
                    + "the current structure will be written to the map file.",
                true);
    }

    /**
     * Validate the XML document.  This method returns if the document is valid.
     * If validation fails it generates an error and ceases execution.
     *
     * @param document the XML document object
     * @throws TransformerException if transformer error
     */
    private static void validate(org.w3c.dom.Document document)
        throws TransformerException {
        StringBuilder err = new StringBuilder();
        boolean trip = false;

        err.append("The following errors were encountered parsing the source XML.\n");
        err.append("No changes have been made to the DSpace instance.\n\n");

        NodeList first = XPathAPI.selectNodeList(document, "/import_structure/community");
        if (first.getLength() == 0) {
            err.append("-There are no top level communities in the source document.");
            System.out.println(err.toString());
            System.exit(1);
        }

        String errs = validateCommunities(first, 1);
        if (errs != null) {
            err.append(errs);
            trip = true;
        }

        if (trip) {
            System.out.println(err.toString());
            System.exit(1);
        }
    }

    /**
     * Validate the communities section of the XML document.  This returns a string
     * containing any errors encountered, or null if there were no errors.
     *
     * @param communities the NodeList of communities to validate
     * @param level       the level in the XML document that we are at, for the purposes
     *                    of error reporting
     * @return the errors that need to be generated by the calling method, or null if
     * no errors.
     */
    private static String validateCommunities(NodeList communities, int level)
        throws TransformerException {
        StringBuilder err = new StringBuilder();
        boolean trip = false;
        String errs = null;

        for (int i = 0; i < communities.getLength(); i++) {
            Node n = communities.item(i);
            NodeList name = XPathAPI.selectNodeList(n, "name");
            if (name.getLength() != 1) {
                String pos = Integer.toString(i + 1);
                err.append("-The level ").append(level)
                        .append(" community in position ").append(pos)
                        .append(" does not contain exactly one name field.\n");
                trip = true;
            }

            // validate sub communities
            NodeList subCommunities = XPathAPI.selectNodeList(n, "community");
            String comErrs = validateCommunities(subCommunities, level + 1);
            if (comErrs != null) {
                err.append(comErrs);
                trip = true;
            }

            // validate collections
            NodeList collections = XPathAPI.selectNodeList(n, "collection");
            String colErrs = validateCollections(collections, level + 1);
            if (colErrs != null) {
                err.append(colErrs);
                trip = true;
            }
        }

        if (trip) {
            errs = err.toString();
        }

        return errs;
    }

    /**
     * validate the collection section of the XML document.  This generates a
     * string containing any errors encountered, or returns null if no errors.
     *
     * @param collections a NodeList of collections to validate
     * @param level       the level in the XML document for the purposes of error reporting
     * @return the errors to be generated by the calling method, or null if none
     */
    private static String validateCollections(NodeList collections, int level)
        throws TransformerException {
        StringBuilder err = new StringBuilder();
        boolean trip = false;
        String errs = null;

        for (int i = 0; i < collections.getLength(); i++) {
            Node n = collections.item(i);
            NodeList name = XPathAPI.selectNodeList(n, "name");
            if (name.getLength() != 1) {
                String pos = Integer.toString(i + 1);
                err.append("-The level ").append(level)
                        .append(" collection in position ").append(pos)
                        .append(" does not contain exactly one name field.\n");
                trip = true;
            }
        }

        if (trip) {
            errs = err.toString();
        }

        return errs;
    }

    /**
     * Load the XML document from input.
     *
     * @param input the filename to load from.
     * @return the DOM representation of the XML input.
     */
    private static org.w3c.dom.Document loadXML(InputStream input)
        throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                                                        .newDocumentBuilder();

        org.w3c.dom.Document document = builder.parse(input);

        return document;
    }

    /**
     * Return the String value of a Node
     *
     * @param node the node from which we want to extract the string value
     * @return the string value of the node
     */
    private static String getStringValue(Node node) {
        String value = node.getNodeValue();

        if (node.hasChildNodes()) {
            Node first = node.getFirstChild();

            if (first.getNodeType() == Node.TEXT_NODE) {
                return first.getNodeValue().trim();
            }
        }

        return value;
    }

    /**
     * Take a node list of communities and build the structure from them, delegating
     * to the relevant methods in this class for sub-communities and collections
     *
     * @param context     the context of the request
     * @param communities a nodelist of communities to create along with their sub-structures
     * @param parent      the parent community of the nodelist of communities to create
     * @return an element array containing additional information regarding the
     * created communities (e.g. the handles they have been assigned)
     */
    private static Element[] handleCommunities(Context context, NodeList communities, Community parent)
        throws TransformerException, SQLException, AuthorizeException {
        Element[] elements = new Element[communities.getLength()];

        for (int i = 0; i < communities.getLength(); i++) {
            Community community;
            Element element = new Element("community");

            // create the community or sub community
            if (parent != null) {
                community = communityService.create(parent, context);
            } else {
                community = communityService.create(null, context);
            }

            // default the short description to be an empty string
            communityService.setMetadata(context, community, "short_description", " ");

            // now update the metadata
            Node tn = communities.item(i);
            for (Map.Entry<String, String> entry : communityMap.entrySet()) {
                NodeList nl = XPathAPI.selectNodeList(tn, entry.getKey());
                if (nl.getLength() == 1) {
                    communityService.setMetadata(context, community, entry.getValue(), getStringValue(nl.item(0)));
                }
            }

            // FIXME: at the moment, if the community already exists by name
            // then this will throw an SQLException on a duplicate key
            // violation.
            // Ideally we'd skip this row and continue to create sub communities
            // and so forth where they don't exist, but it's proving difficult
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

            if (communityService.getMetadata(community, "short_description") != null) {
                Element descriptionElement = new Element("description");
                descriptionElement.setText(communityService.getMetadata(community, "short_description"));
                element.addContent(descriptionElement);
            }

            if (communityService.getMetadata(community, "introductory_text") != null) {
                Element introElement = new Element("intro");
                introElement.setText(communityService.getMetadata(community, "introductory_text"));
                element.addContent(introElement);
            }

            if (communityService.getMetadata(community, "copyright_text") != null) {
                Element copyrightElement = new Element("copyright");
                copyrightElement.setText(communityService.getMetadata(community, "copyright_text"));
                element.addContent(copyrightElement);
            }

            if (communityService.getMetadata(community, "side_bar_text") != null) {
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
            for (j = 0; j < subCommunityElements.length; j++) {
                element.addContent(subCommunityElements[j]);
            }
            for (j = 0; j < collectionElements.length; j++) {
                element.addContent(collectionElements[j]);
            }

            elements[i] = element;
        }

        return elements;
    }

    /**
     * Take a node list of collections and create the structure from them
     *
     * @param context     the context of the request
     * @param collections the node list of collections to be created
     * @param parent      the parent community to whom the collections belong
     * @return an Element array containing additional information about the
     * created collections (e.g. the handle)
     */
    private static Element[] handleCollections(Context context, NodeList collections, Community parent)
        throws TransformerException, SQLException, AuthorizeException {
        Element[] elements = new Element[collections.getLength()];

        for (int i = 0; i < collections.getLength(); i++) {
            Element element = new Element("collection");
            Collection collection = collectionService.create(context, parent);

            // default the short description to the empty string
            collectionService.setMetadata(context, collection, "short_description", " ");

            // import the rest of the metadata
            Node tn = collections.item(i);
            for (Map.Entry<String, String> entry : collectionMap.entrySet()) {
                NodeList nl = XPathAPI.selectNodeList(tn, entry.getKey());
                if (nl.getLength() == 1) {
                    collectionService.setMetadata(context, collection, entry.getValue(), getStringValue(nl.item(0)));
                }
            }

            collectionService.update(context, collection);

            element.setAttribute("identifier", collection.getHandle());

            Element nameElement = new Element("name");
            nameElement.setText(collectionService.getMetadata(collection, "name"));
            element.addContent(nameElement);

            if (collectionService.getMetadata(collection, "short_description") != null) {
                Element descriptionElement = new Element("description");
                descriptionElement.setText(collectionService.getMetadata(collection, "short_description"));
                element.addContent(descriptionElement);
            }

            if (collectionService.getMetadata(collection, "introductory_text") != null) {
                Element introElement = new Element("intro");
                introElement.setText(collectionService.getMetadata(collection, "introductory_text"));
                element.addContent(introElement);
            }

            if (collectionService.getMetadata(collection, "copyright_text") != null) {
                Element copyrightElement = new Element("copyright");
                copyrightElement.setText(collectionService.getMetadata(collection, "copyright_text"));
                element.addContent(copyrightElement);
            }

            if (collectionService.getMetadata(collection, "side_bar_text") != null) {
                Element sidebarElement = new Element("sidebar");
                sidebarElement.setText(collectionService.getMetadata(collection, "side_bar_text"));
                element.addContent(sidebarElement);
            }

            if (collectionService.getMetadata(collection, "license") != null) {
                Element sidebarElement = new Element("license");
                sidebarElement.setText(collectionService.getMetadata(collection, "license"));
                element.addContent(sidebarElement);
            }

            if (collectionService.getMetadata(collection, "provenance_description") != null) {
                Element sidebarElement = new Element("provenance");
                sidebarElement.setText(collectionService.getMetadata(collection, "provenance_description"));
                element.addContent(sidebarElement);
            }

            elements[i] = element;
        }

        return elements;
    }

}
