/*
 * SchemaImporter.java
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
 * uses it to create the required metadata schemas for the repository.
 * This needs to be run before the MetadataImporter if there are
 * metadata elements in that document that rely on schemas imported here
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
public class SchemaImporter
{

	/**
	 * Main method for collecting arguments from the command line
	 */
    public static void main(String[] args)
		throws RegistryImportException, ParseException, ParserConfigurationException
	{
	    // create an options object and populate it
	    CommandLineParser parser = new PosixParser();
	    Options options = new Options();
	    options.addOption("f", "file", true, "source xml file for registry");
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
    	throws RegistryImportException
    {
        try 
        {
	        // create a context
	        Context context = new Context();
	        context.setIgnoreAuthorization(true);
	        
	        // read the XML
	        Document document = RegistryImporter.loadXML(file);
	        
	        // Get the nodes corresponding to types
	        NodeList typeNodes = XPathAPI.selectNodeList(document, "/metadata-schemas/schema");
	
	        // Add each one as a new format to the registry
	        for (int i = 0; i < typeNodes.getLength(); i++)
	        {
	            Node n = typeNodes.item(i);
	            loadSchema(context, n);
	        }
	        
	        context.complete();
        }
        catch (Exception e)
        {
            throw new RegistryImportException("there was a problem loading the schema registry", e);
        }
    }
    
    /**
     * Process a node in the metadata registry XML file.  If the
     * schema already exists, it will not be recreated
     * 
     * @param context
     *            DSpace context object
     * @param node
     *            the node in the DOM tree
     * @throws NonUniqueMetadataException
     */
    private static void loadSchema(Context context, Node node)
    	throws SQLException, IOException, TransformerException,
            AuthorizeException, NonUniqueMetadataException
    {
        // Get the values
        String name = RegistryImporter.getElementData(node, "name");
        String namespace = RegistryImporter.getElementData(node, "namespace");

        System.out.print("Registering Schema: " + name + " - " + namespace + " ... ");
        
        // check to see if the schema already exists
        MetadataSchema s = MetadataSchema.find(context, name);
        
        if (s != null)
        {
            System.out.println("already exists, skipping");
            return;
        }
        
        MetadataSchema schema = new MetadataSchema(namespace, name);
        schema.create(context);
        System.out.println("created");
    }
    
    /**
     * Print the usage message to stdout
     */
    public static void usage()
    {
        String usage = "Use this class with the following option:\n" +
        				" -f <xml source file> : specify which xml source file " +
        				"contains the schemas to import.\n";
        System.out.println(usage);
    }
}
