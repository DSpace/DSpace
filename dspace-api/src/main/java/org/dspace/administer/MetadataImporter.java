/*
 * MetadataImporter.java
 * 
 * Copyright (c) 2006, Imperial College London.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of Imperial College nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.administer;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.apache.xpath.XPathAPI;

import org.dspace.administer.DCType;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

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
 * <dspace-dc-types>
 *   <dc-type>
 *     <schema>icadmin</schema>
 *     <element>status</element>
 *     <qualifier>dateset</qualifier>
 *     <scope_note>the workflow status of an item</scope_note>
 *   </dc-type>
 *   
 *   [....]
 *   
 * </dspace-dc-types>
 */
public class MetadataImporter
{
	/**
	 * main method for reading user input from the command line
	 */
    public static void main(String[] args)
    	throws ParseException, SQLException, IOException, TransformerException,
    			ParserConfigurationException, AuthorizeException, SAXException,
    			NonUniqueMetadataException
    {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("f", "file", true, "source xml file for DC fields");
        CommandLine line = parser.parse(options, args);
        
        String file = null;
        if (line.hasOption('f'))
        {
            file = line.getOptionValue('f');
        }
        else
        {
            usage();
            System.exit(0);
        }
        
        loadRegistry(file);
    }
    
    /**
     * Load the data from the specified file path into the database
     * 
     * @param 	file	the file path containing the source data
     */
    public static void loadRegistry(String file)
    	throws SQLException, IOException, TransformerException, ParserConfigurationException, 
    		AuthorizeException, SAXException, NonUniqueMetadataException
    {
        // create a context
        Context context = new Context();
        context.setIgnoreAuthorization(true);
        
        // read the XML
        Document document = RegistryImporter.loadXML(file);
        
        // Get the nodes corresponding to types
        NodeList typeNodes = XPathAPI.selectNodeList(document, "/dspace-dc-types/dc-type");

        // Add each one as a new format to the registry
        for (int i = 0; i < typeNodes.getLength(); i++)
        {
            Node n = typeNodes.item(i);
            loadType(context, n);
        }
        
        context.complete();
    }
    
    /**
     * Process a node in the metadata registry XML file. The node must
     * be a "dc-type" node.  If the type already exists, then it
     * will not be reimported
     * 
     * @param context
     *            DSpace context object
     * @param node
     *            the node in the DOM tree
     * @throws NonUniqueMetadataException
     */
    private static void loadType(Context context, Node node)
            throws SQLException, IOException, TransformerException,
            AuthorizeException, NonUniqueMetadataException
    {
        // Get the values
        String schema = RegistryImporter.getElementData(node, "schema");
        String element = RegistryImporter.getElementData(node, "element");
        String qualifier = RegistryImporter.getElementData(node, "qualifier");
        String scopeNote = RegistryImporter.getElementData(node, "scope_note");

        // If the schema is not provided default to DC
        if (schema == null)
        {
            schema = MetadataSchema.DC_SCHEMA;
        }

        System.out.print("Registering Metadata: " + schema + "." + element + "." + qualifier + " ... ");
        
        // Find the matching schema object
        MetadataSchema schemaObj = MetadataSchema.find(context, schema);
        
        MetadataField mf = MetadataField.findByElement(context, schemaObj.getSchemaID(), element, qualifier);
        if (mf != null)
        {
            System.out.println("already exists, skipping");
            return;
        }
        
        MetadataField field = new MetadataField();
        field.setSchemaID(schemaObj.getSchemaID());
        field.setElement(element);
        field.setQualifier(qualifier);
        field.setScopeNote(scopeNote);
        field.create(context);
        System.out.println("created");
    }
    
    /**
     * Print the usage message to stdout
     */
    public static void usage()
    {
        String usage = "Use this class with the following option:\n" +
        				" -f <xml source file> : specify which xml source file " +
        				"contains the DC fields to import.\n";
        System.out.println(usage);
    }
}
