/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;


/**
 * @author Graham Triggs
 *
 * This class creates an XML document as passed in the arguments and
 * from the metadata schemas for the repository.
 *
 * The form of the XML is as follows
 * {@code
 * <metadata-schemas>
 * <schema>
 * <name>dc</name>
 * <namespace>http://dublincore.org/documents/dcmi-terms/</namespace>
 * </schema>
 * </metadata-schemas>
 * }
 */
public class MetadataExporter {

    protected static MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance()
                                                                                        .getMetadataSchemaService();
    protected static MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance()
                                                                                      .getMetadataFieldService();

    /**
     * Default constructor
     */
    private MetadataExporter() { }

    /**
     * @param args command line arguments
     * @throws ParseException          if parser error
     * @throws IOException             if IO error
     * @throws SQLException            if database error
     * @throws RegistryExportException if export error
     * @throws ClassNotFoundException  if no suitable DOM implementation
     * @throws InstantiationException  if no suitable DOM implementation
     * @throws IllegalAccessException  if no suitable DOM implementation
     */
    public static void main(String[] args)
        throws ParseException, SQLException, IOException, RegistryExportException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        // create an options object and populate it
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("f", "file", true, "output xml file for registry");
        options.addOption("s", "schema", true, "the name of the schema to export");
        CommandLine line = parser.parse(options, args);

        String file = null;
        String schema = null;

        if (line.hasOption('f')) {
            file = line.getOptionValue('f');
        } else {
            usage();
            System.exit(0);
        }

        if (line.hasOption('s')) {
            schema = line.getOptionValue('s');
        }

        saveRegistry(file, schema);
    }

    /**
     * Save a registry to a file path
     *
     * @param file   file path
     * @param schema schema definition to save
     * @throws SQLException            if database error
     * @throws IOException             if IO error
     * @throws RegistryExportException if export error
     * @throws ClassNotFoundException  if no suitable DOM implementation
     * @throws InstantiationException  if no suitable DOM implementation
     * @throws IllegalAccessException  if no suitable DOM implementation
     */
    public static void saveRegistry(String file, String schema)
        throws SQLException, IOException, RegistryExportException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        // create a context
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        // Initialize an XML document.
        Document document = DOMImplementationRegistry.newInstance()
                .getDOMImplementation("XML 3.0")
                .createDocument(null, "dspace-dc-types", null);

        // Save the schema definition(s)
        saveSchema(context, document, schema);

        List<MetadataField> mdFields = null;

        // If a single schema has been specified
        if (schema != null && !"".equals(schema)) {
            // Get the id of that schema
            MetadataSchema mdSchema = metadataSchemaService.find(context, schema);
            if (mdSchema == null) {
                throw new RegistryExportException("no schema to export");
            }

            // Get the metadata fields only for the specified schema
            mdFields = metadataFieldService.findAllInSchema(context, mdSchema);
        } else {
            // Get the metadata fields for all the schemas
            mdFields = metadataFieldService.findAll(context);
        }

        // Compose the metadata fields
        for (MetadataField mdField : mdFields) {
            saveType(context, document, mdField);
        }

        // Serialize the completed document to the output file.
        try (Writer writer = new BufferedWriter(new FileWriter(file))) {
            DOMImplementationLS lsImplementation
                    = (DOMImplementationLS) DOMImplementationRegistry.newInstance()
                            .getDOMImplementation("LS");
            LSSerializer serializer = lsImplementation.createLSSerializer();
            DOMConfiguration configuration = serializer.getDomConfig();
            configuration.setParameter("format-pretty-print", true);
            LSOutput lsOutput = lsImplementation.createLSOutput();
            lsOutput.setEncoding("UTF-8");
            lsOutput.setCharacterStream(writer);
            serializer.write(document, lsOutput);
        }

        // abort the context, as we shouldn't have changed it!!
        context.abort();
    }

    /**
     * Compose the schema registry. If the parameter 'schema' is null or empty, save all schemas.
     *
     * @param context       DSpace Context
     * @param document      the document being built
     * @param schema        schema (may be null to save all)
     * @throws SQLException            if database error
     * @throws RegistryExportException if export error
     */
    public static void saveSchema(Context context, Document document, String schema)
        throws SQLException, RegistryExportException {
        if (schema != null && !"".equals(schema)) {
            // Find a single named schema
            MetadataSchema mdSchema = metadataSchemaService.find(context, schema);

            saveSchema(document, mdSchema);
        } else {
            // Find all schemas
            List<MetadataSchema> mdSchemas = metadataSchemaService.findAll(context);

            for (MetadataSchema mdSchema : mdSchemas) {
                saveSchema(document, mdSchema);
            }
        }
    }

    /**
     * Compose a single schema (namespace) registry entry
     *
     * @param document the output document being built.
     * @param mdSchema DSpace metadata schema
     * @throws RegistryExportException if export error
     */
    private static void saveSchema(Document document, MetadataSchema mdSchema)
        throws RegistryExportException {
        // If we haven't got a schema, it's an error
        if (mdSchema == null) {
            throw new RegistryExportException("no schema to export");
        }

        String name = mdSchema.getName();
        String namespace = mdSchema.getNamespace();

        if (name == null || "".equals(name)) {
            System.out.println("name is null, skipping");
            return;
        }

        if (namespace == null || "".equals(namespace)) {
            System.out.println("namespace is null, skipping");
            return;
        }

        Element document_element = document.getDocumentElement();

        // Compose the parent tag
        Element schema_element = document.createElement("dc-schema");
        document_element.appendChild(schema_element);

        // Compose the schema name
        Element name_element = document.createElement("name");
        schema_element.appendChild(name_element);
        name_element.setTextContent(name);

        // Compose the schema namespace
        Element namespace_element = document.createElement("namespace");
        schema_element.appendChild(namespace_element);
        namespace_element.setTextContent(namespace);
    }

    /**
     * Compose a single metadata field registry entry to XML.
     *
     * @param context       DSpace context
     * @param document      the output document being built.
     * @param mdField       DSpace metadata field
     * @throws RegistryExportException if export error
     * @throws SQLException            if database error
     */
    private static void saveType(Context context, Document document, MetadataField mdField)
        throws RegistryExportException, SQLException {
        // If we haven't been given a field, it's an error
        if (mdField == null) {
            throw new RegistryExportException("no field to export");
        }

        // Get the data from the metadata field
        String schemaName = getSchemaName(context, mdField);
        String element = mdField.getElement();
        String qualifier = mdField.getQualifier();
        String scopeNote = mdField.getScopeNote();

        // We must have a schema and element
        if (schemaName == null || element == null) {
            throw new RegistryExportException("incomplete field information");
        }

        Element document_element = document.getDocumentElement();

        // Compose the parent tag
        Element dc_type = document.createElement("dc-type");
        document_element.appendChild(dc_type);

        // Compose the schema name
        Element schema_element = document.createElement("schema");
        dc_type.appendChild(schema_element);
        schema_element.setTextContent(schemaName);

        // Compose the element
        Element element_element = document.createElement("element");
        dc_type.appendChild(element_element);
        element_element.setTextContent(element);

        // Compose the qualifier, if present
        if (qualifier != null) {
            Element qualifier_element = document.createElement("qualifier");
            dc_type.appendChild(qualifier_element);
            qualifier_element.setTextContent(qualifier);
        } else {
            dc_type.appendChild(document.createComment("unqualified"));
        }

        // Compose the scope note, if present
        if (scopeNote != null) {
            Element scope_element = document.createElement("scope_note");
            dc_type.appendChild(scope_element);
            scope_element.setTextContent(scopeNote);
        } else {
            dc_type.appendChild(document.createComment("no scope note"));
        }
    }

    static Map<Integer, String> schemaMap = new HashMap<Integer, String>();

    /**
     * Helper method to retrieve a schema name for the field.
     * Caches the name after looking up the id.
     *
     * @param context DSpace Context
     * @param mdField DSpace metadata field
     * @return name of schema
     * @throws SQLException            if database error
     * @throws RegistryExportException if export error
     */
    private static String getSchemaName(Context context, MetadataField mdField)
        throws SQLException, RegistryExportException {
        // Get name from cache
        String name = schemaMap.get(mdField.getMetadataSchema().getID());

        if (name == null) {
            // Name not retrieved before, so get the schema now
            MetadataSchema mdSchema = metadataSchemaService.find(context, mdField.getMetadataSchema().getID());
            if (mdSchema != null) {
                name = mdSchema.getName();
                schemaMap.put(mdSchema.getID(), name);
            } else {
                // Can't find the schema
                throw new RegistryExportException("Can't get schema name for field");
            }
        }
        return name;
    }

    /**
     * Print the usage message to standard output
     */
    public static void usage() {
        String usage = "Use this class with the following options:\n" +
            " -f <xml output file> : specify the output file for the schemas\n" +
            " -s <schema> : name of the schema to export\n";
        System.out.println(usage);
    }
}
