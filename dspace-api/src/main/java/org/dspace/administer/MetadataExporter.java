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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.xml.sax.SAXException;


/**
 * @author Graham Triggs
 *
 * This class creates an xml document as passed in the arguments and
 * from the metadata schemas for the repository.
 * 
 * The form of the XML is as follows
 * 
 * <metadata-schemas>
 *   <schema>
 *     <name>dc</name>
 *     <namespace>http://dublincore.org/documents/dcmi-terms/</namespace>
 *   </schema>
 * </metadata-schemas>
 */
public class MetadataExporter
{

    /**
     * @param args
     * @throws ParseException 
     * @throws SAXException 
     * @throws IOException 
     * @throws SQLException 
     * @throws RegistryExportException 
     */
    public static void main(String[] args) throws ParseException, SQLException, IOException, SAXException, RegistryExportException
    {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("f", "file",   true, "output xml file for registry");
        options.addOption("s", "schema", true, "the name of the schema to export");
        CommandLine line = parser.parse(options, args);
        
        String file   = null;
        String schema = null;

        if (line.hasOption('f'))
        {
            file   = line.getOptionValue('f');
        }
        else
        {
            usage();
            System.exit(0);
        }
        
        if (line.hasOption('s'))
        {
            schema = line.getOptionValue('s');
        }

        saveRegistry(file, schema);
    }

    public static void saveRegistry(String file, String schema) throws SQLException, IOException, SAXException, RegistryExportException
    {
        // create a context
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        OutputFormat xmlFormat = new OutputFormat(Method.XML, "UTF-8", true);
        xmlFormat.setLineWidth(120);
        xmlFormat.setIndent(4);
        
        XMLSerializer xmlSerializer = new XMLSerializer(new BufferedWriter(new FileWriter(file)), xmlFormat);
        //        XMLSerializer xmlSerializer = new XMLSerializer(System.out, xmlFormat);
        xmlSerializer.startDocument();
        xmlSerializer.startElement("dspace-dc-types", null);
        
        // Save the schema definition(s)
        saveSchema(context, xmlSerializer, schema);

        MetadataField[] mdFields = null;

        // If a single schema has been specified
        if (schema != null && !"".equals(schema))
        {
            // Get the id of that schema
            MetadataSchema mdSchema = MetadataSchema.find(context, schema);
            if (mdSchema == null)
            {
                throw new RegistryExportException("no schema to export");
            }
            
            // Get the metadata fields only for the specified schema
            mdFields = MetadataField.findAllInSchema(context, mdSchema.getSchemaID());
        }
        else
        {
            // Get the metadata fields for all the schemas
            mdFields = MetadataField.findAll(context);
        }
        
        // Output the metadata fields
        for (MetadataField mdField : mdFields)
        {
            saveType(context, xmlSerializer, mdField);
        }
        
        xmlSerializer.endElement("dspace-dc-types");
        xmlSerializer.endDocument();
        
        // abort the context, as we shouldn't have changed it!!
        context.abort();
    }
    
    /**
     * Serialize the schema registry. If the parameter 'schema' is null or empty, save all schemas
     * @param context
     * @param xmlSerializer
     * @param schema
     * @throws SQLException
     * @throws SAXException
     * @throws RegistryExportException
     */
    public static void saveSchema(Context context, XMLSerializer xmlSerializer, String schema) throws SQLException, SAXException, RegistryExportException
    {
        if (schema != null && !"".equals(schema))
        {
            // Find a single named schema
            MetadataSchema mdSchema = MetadataSchema.find(context, schema);
            
            saveSchema(xmlSerializer, mdSchema);
        }
        else
        {
            // Find all schemas
            MetadataSchema[] mdSchemas = MetadataSchema.findAll(context);
            
            for (MetadataSchema mdSchema : mdSchemas)
            {
                saveSchema(xmlSerializer, mdSchema);
            }
        }
    }
    
    /**
     * Serialize a single schema (namespace) registry entry
     * 
     * @param xmlSerializer
     * @param mdSchema
     * @throws SAXException
     * @throws RegistryExportException
     */
    private static void saveSchema(XMLSerializer xmlSerializer, MetadataSchema mdSchema) throws SAXException, RegistryExportException
    {
        // If we haven't got a schema, it's an error
        if (mdSchema == null)
        {
            throw new RegistryExportException("no schema to export");
        }
        
        String name      = mdSchema.getName();
        String namespace = mdSchema.getNamespace();
        
        if (name == null || "".equals(name))
        {
            System.out.println("name is null, skipping");
            return;
        }

        if (namespace == null || "".equals(namespace))
        {
            System.out.println("namespace is null, skipping");
            return;
        }

        // Output the parent tag
        xmlSerializer.startElement("dc-schema", null);
        
        // Output the schema name
        xmlSerializer.startElement("name", null);
        xmlSerializer.characters(name.toCharArray(), 0, name.length());
        xmlSerializer.endElement("name");

        // Output the schema namespace
        xmlSerializer.startElement("namespace", null);
        xmlSerializer.characters(namespace.toCharArray(), 0, namespace.length());
        xmlSerializer.endElement("namespace");

        xmlSerializer.endElement("dc-schema");
    }
    
    /**
     * Serialize a single metadata field registry entry to xml
     * 
     * @param context
     * @param xmlSerializer
     * @param mdField
     * @throws SAXException
     * @throws RegistryExportException
     * @throws SQLException
     * @throws IOException 
     */
    private static void saveType(Context context, XMLSerializer xmlSerializer, MetadataField mdField) throws SAXException, RegistryExportException, SQLException, IOException
    {
        // If we haven't been given a field, it's an error
        if (mdField == null)
        {
            throw new RegistryExportException("no field to export");
        }
        
        // Get the data from the metadata field
        String schemaName = getSchemaName(context, mdField);
        String element = mdField.getElement();
        String qualifier = mdField.getQualifier();
        String scopeNote = mdField.getScopeNote();

        // We must have a schema and element
        if (schemaName == null || element == null)
        {
            throw new RegistryExportException("incomplete field information");
        }

        // Output the parent tag
        xmlSerializer.startElement("dc-type", null);

        // Output the schema name
        xmlSerializer.startElement("schema", null);
        xmlSerializer.characters(schemaName.toCharArray(), 0, schemaName.length());
        xmlSerializer.endElement("schema");

        // Output the element
        xmlSerializer.startElement("element", null);
        xmlSerializer.characters(element.toCharArray(), 0, element.length());
        xmlSerializer.endElement("element");

        // Output the qualifier, if present
        if (qualifier != null)
        {
            xmlSerializer.startElement("qualifier", null);
            xmlSerializer.characters(qualifier.toCharArray(), 0, qualifier.length());
            xmlSerializer.endElement("qualifier");
        }
        else
        {
            xmlSerializer.comment("unqualified");
        }
        
        // Output the scope note, if present
        if (scopeNote != null)
        {
            xmlSerializer.startElement("scope_note", null);
            xmlSerializer.characters(scopeNote.toCharArray(), 0, scopeNote.length());
            xmlSerializer.endElement("scope_note");
        }
        else
        {
            xmlSerializer.comment("no scope note");
        }
        
        xmlSerializer.endElement("dc-type");
    }
    
    /**
     * Helper method to retrieve a schema name for the field.
     * Caches the name after looking up the id.
     */
    static Map<Integer, String> schemaMap = new HashMap<Integer, String>();
    private static String getSchemaName(Context context, MetadataField mdField) throws SQLException, RegistryExportException
    {
        // Get name from cache
        String name = schemaMap.get(Integer.valueOf(mdField.getSchemaID()));

        if (name == null)
        {
            // Name not retrieved before, so get the schema now
            MetadataSchema mdSchema = MetadataSchema.find(context, mdField.getSchemaID());
            if (mdSchema != null)
            {
                name = mdSchema.getName();
                schemaMap.put(Integer.valueOf(mdSchema.getSchemaID()), name);
            }
            else
            {
                // Can't find the schema
                throw new RegistryExportException("Can't get schema name for field");
            }
        }
        return name;
    }
    
    /**
     * Print the usage message to stdout
     */
    public static void usage()
    {
        String usage = "Use this class with the following options:\n" +
                        " -f <xml output file> : specify the output file for the schemas\n" +
                        " -s <schema> : name of the schema to export\n";
        System.out.println(usage);
    }
}
