/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import java.io.IOException;
import java.sql.SQLException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Richard Jones
 *
 * This class takes an xml document as passed in the arguments and
 * uses it to create metadata elements in the Metadata Registry if
 * they do not already exist
 *
 * The format of the XML file is as follows:
 *
 * {@code
 * <dspace-dc-types>
 * <dc-type>
 * <schema>icadmin</schema>
 * <element>status</element>
 * <qualifier>dateset</qualifier>
 * <scope_note>the workflow status of an item</scope_note>
 * </dc-type>
 *
 * [....]
 *
 * </dspace-dc-types>
 * }
 */
public class MetadataImporter {
    protected static MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance()
                                                                                        .getMetadataSchemaService();
    protected static MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance()
                                                                                      .getMetadataFieldService();

    /**
     * logging category
     */
    private static final Logger log = LoggerFactory.getLogger(MetadataImporter.class);

    /**
     * Default constructor
     */
    private MetadataImporter() { }

    /**
     * main method for reading user input from the command line
     *
     * @param args the command line arguments given
     * @throws ParseException               if parse error
     * @throws SQLException                 if database error
     * @throws IOException                  if IO error
     * @throws TransformerException         if transformer error
     * @throws ParserConfigurationException if configuration error
     * @throws AuthorizeException           if authorization error
     * @throws SAXException                 if parser error
     * @throws NonUniqueMetadataException   if duplicate metadata
     * @throws RegistryImportException      if import fails
     **/
    public static void main(String[] args)
        throws ParseException, SQLException, IOException, TransformerException,
        ParserConfigurationException, AuthorizeException, SAXException,
        NonUniqueMetadataException, RegistryImportException, XPathExpressionException {

        // create an options object and populate it
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("f", "file", true, "source xml file for DC fields");
        options.addOption("u", "update", false, "update an existing schema");
        CommandLine line = parser.parse(options, args);

        if (line.hasOption('f')) {
            String file = line.getOptionValue('f');
            boolean forceUpdate = line.hasOption('u');
            loadRegistry(file, forceUpdate);
        } else {
            usage();
            System.exit(1);
        }
    }

    /**
     * Load the data from the specified file path into the database
     *
     * @param file        the file path containing the source data
     * @param forceUpdate whether to force update
     * @throws SQLException                 if database error
     * @throws IOException                  if IO error
     * @throws TransformerException         if transformer error
     * @throws ParserConfigurationException if configuration error
     * @throws AuthorizeException           if authorization error
     * @throws SAXException                 if parser error
     * @throws NonUniqueMetadataException   if duplicate metadata
     * @throws RegistryImportException      if import fails
     */
    public static void loadRegistry(String file, boolean forceUpdate)
        throws SQLException, IOException, TransformerException, ParserConfigurationException, AuthorizeException,
        SAXException, NonUniqueMetadataException, RegistryImportException, XPathExpressionException {
        Context context = null;

        try {
            // create a context
            context = new Context();
            context.turnOffAuthorisationSystem();

            // read the XML
            Document document = RegistryImporter.loadXML(file);

            // Get the nodes corresponding to types
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList schemaNodes = (NodeList) xPath.compile("/dspace-dc-types/dc-schema")
                                                   .evaluate(document, XPathConstants.NODESET);

            // Add each one as a new format to the registry
            for (int i = 0; i < schemaNodes.getLength(); i++) {
                Node n = schemaNodes.item(i);
                loadSchema(context, n, forceUpdate);
            }

            // Get the nodes corresponding to types
            NodeList typeNodes = (NodeList) xPath.compile("/dspace-dc-types/dc-type")
                                                 .evaluate(document, XPathConstants.NODESET);

            // Add each one as a new format to the registry
            for (int i = 0; i < typeNodes.getLength(); i++) {
                Node n = typeNodes.item(i);
                loadType(context, n);
            }

            context.restoreAuthSystemState();
            context.complete();
        } finally {
            // Clean up our context, if it still exists & it was never completed
            if (context != null && context.isValid()) {
                context.abort();
            }
        }
    }

    /**
     * Process a node in the metadata registry XML file.  If the
     * schema already exists, it will not be recreated
     *
     * @param context DSpace context object
     * @param node    the node in the DOM tree
     * @throws SQLException               if database error
     * @throws IOException                if IO error
     * @throws TransformerException       if transformer error
     * @throws AuthorizeException         if authorization error
     * @throws NonUniqueMetadataException if duplicate metadata
     * @throws RegistryImportException    if import fails
     */
    private static void loadSchema(Context context, Node node, boolean updateExisting)
        throws SQLException, AuthorizeException, NonUniqueMetadataException, RegistryImportException,
        XPathExpressionException {
        // Get the values
        String name = RegistryImporter.getElementData(node, "name");
        String namespace = RegistryImporter.getElementData(node, "namespace");

        if (name == null || "".equals(name)) {
            throw new RegistryImportException("Name of schema must be supplied");
        }

        if (namespace == null || "".equals(namespace)) {
            throw new RegistryImportException("Namespace of schema must be supplied");
        }

        // check to see if the schema already exists
        MetadataSchema s = metadataSchemaService.find(context, name);

        if (s == null) {
            // Schema does not exist - create
            log.info("Registering Schema " + name + " (" + namespace + ")");
            metadataSchemaService.create(context, name, namespace);
        } else {
            // Schema exists - if it's the same namespace, allow the type imports to continue
            if (s.getNamespace().equals(namespace)) {
                // This schema already exists with this namespace, skipping it
                return;
            }

            // It's a different namespace - have we been told to update?
            if (updateExisting) {
                // Update the existing schema namespace and continue to type import
                log.info("Updating Schema " + name + ": New namespace " + namespace);
                s.setNamespace(namespace);
                metadataSchemaService.update(context, s);
            } else {
                throw new RegistryImportException(
                    "Schema " + name + " already registered with different namespace " + namespace + ". Rerun with " +
                        "'update' option enabled if you wish to update this schema.");
            }
        }

    }

    /**
     * Process a node in the metadata registry XML file. The node must
     * be a "dc-type" node.  If the type already exists, then it
     * will not be re-imported.
     *
     * @param context DSpace context object
     * @param node    the node in the DOM tree
     * @throws SQLException               if database error
     * @throws IOException                if IO error
     * @throws TransformerException       if transformer error
     * @throws AuthorizeException         if authorization error
     * @throws NonUniqueMetadataException if duplicate metadata
     * @throws RegistryImportException    if import fails
     */
    private static void loadType(Context context, Node node)
        throws SQLException, IOException, AuthorizeException, NonUniqueMetadataException, RegistryImportException,
        XPathExpressionException {
        // Get the values
        String schema = RegistryImporter.getElementData(node, "schema");
        String element = RegistryImporter.getElementData(node, "element");
        String qualifier = RegistryImporter.getElementData(node, "qualifier");
        String scopeNote = RegistryImporter.getElementData(node, "scope_note");

        // If the schema is not provided default to DC
        if (schema == null) {
            schema = MetadataSchemaEnum.DC.getName();
        }


        // Find the matching schema object
        MetadataSchema schemaObj = metadataSchemaService.find(context, schema);

        if (schemaObj == null) {
            throw new RegistryImportException("Schema '" + schema + "' is not registered and does not exist.");
        }

        MetadataField mf = metadataFieldService.findByElement(context, schemaObj, element, qualifier);
        if (mf != null) {
            // Metadata field already exists, skipping it
            return;
        }

        // Actually create this metadata field as it doesn't yet exist
        String fieldName = schema + "." + element + "." + qualifier;
        if (qualifier == null) {
            fieldName = schema + "." + element;
        }
        log.info("Registering metadata field " + fieldName);
        MetadataField field = metadataFieldService.create(context, schemaObj, element, qualifier, scopeNote);
        metadataFieldService.update(context, field);
    }

    /**
     * Print the usage message to stdout
     */
    public static void usage() {
        String usage = "Use this class with the following option:\n" +
            " -f <xml source file> : specify which xml source file " +
            "contains the DC fields to import.\n";
        System.out.println(usage);
    }
}
