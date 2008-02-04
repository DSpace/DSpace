/*
 * ItemImport.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
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
package org.dspace.app.itemimport;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.xpath.XPathAPI;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.dao.ResourcePolicyDAO;
import org.dspace.authorize.dao.ResourcePolicyDAOFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.dao.BitstreamDAOFactory;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.dao.WorkspaceItemDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ExternalIdentifierService;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;
import org.dspace.workflow.WorkflowManager;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Import items into DSpace. The conventional use is upload files by copying
 * them. DSpace writes the item's bitstreams into its assetstore. Metadata is
 * also loaded to the DSpace database.
 * <P>
 * A second use assumes the bitstream files already exist in a storage
 * resource accessible to DSpace. In this case the bitstreams are 'registered'.
 * That is, the metadata is loaded to the DSpace database and DSpace is given
 * the location of the file which is subsumed into DSpace.
 * <P>
 * The distinction is controlled by the format of lines in the 'contents' file.
 * See comments in processContentsFile() below.
 * <P>
 * Modified by David Little, UCSD Libraries 12/21/04 to
 * allow the registration of files (bitstreams) into DSpace.
 */
public class ItemImport
{
    static boolean useWorkflow = false;

    static boolean isTest = false;

    static boolean isResume = false;

    static boolean template = false;

    static PrintWriter mapOut = null;

    private static CollectionDAO collectionDAO;

    private static ItemDAO itemDAO;

    private static ExternalIdentifierDAO identifierDAO;

    private static Context context;

    // File listing filter to look for metadata files
    static FilenameFilter metadataFileFilter = new FilenameFilter()
    {
        public boolean accept(File dir, String n)
        {
            return n.startsWith("metadata_");
        }
    };

    // File listing filter to check for folders
    static FilenameFilter directoryFilter = new FilenameFilter()
    {
        public boolean accept(File dir, String n)
        {
            File item = new File(dir.getAbsolutePath() + File.separatorChar + n);
            return item.isDirectory();
        }
    };

    public static void main(String[] argv) throws Exception
    {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("a", "add", false, "add items to DSpace");
        options.addOption("r", "replace", false, "replace items in mapfile");
        options.addOption("d", "delete", false,
                "delete items listed in mapfile");
        options.addOption("s", "source", true, "source of items (directory)");
        options.addOption("c", "collection", true,
                "destination collection(s) URIs (canonical form) or database ID");
        options.addOption("m", "mapfile", true, "mapfile items in mapfile");
        options.addOption("e", "eperson", true,
                "email of eperson doing importing");
        options.addOption("w", "workflow", false,
                "send submission through collection's workflow");
        options.addOption("t", "test", false,
                "test run - do not actually import items");
        options.addOption("p", "template", false, "apply template");
        options.addOption("R", "resume", false,
                "resume a failed import (add only)");

        options.addOption("h", "help", false, "help");

        CommandLine line = parser.parse(options, argv);

        String command = null; // add replace remove, etc
        String sourcedir = null;
        String mapfile = null;
        String eperson = null; // db ID or email
        String[] collections = null; // db ID or URIs
        int status = 0;

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ItemImport\n", options);
            System.out
                    .println("\nadding items:    ItemImport -a -e eperson -c collection -s sourcedir -m mapfile");
            System.out
                    .println("replacing items: ItemImport -r -e eperson -c collection -s sourcedir -m mapfile");
            System.out
                    .println("deleting items:  ItemImport -d -e eperson -m mapfile");
            System.out
                    .println("If multiple collections are specified, the first collection will be the one that owns the item.");

            System.exit(0);
        }

        if (line.hasOption('a'))
        {
            command = "add";
        }

        if (line.hasOption('r'))
        {
            command = "replace";
        }

        if (line.hasOption('d'))
        {
            command = "delete";
        }

        if (line.hasOption('w'))
        {
            useWorkflow = true;
        }

        if (line.hasOption('t'))
        {
            isTest = true;
            System.out.println("**Test Run** - not actually importing items.");
        }

        if (line.hasOption('p'))
        {
            template = true;
        }

        if (line.hasOption('s')) // source
        {
            sourcedir = line.getOptionValue('s');
        }

        if (line.hasOption('m')) // mapfile
        {
            mapfile = line.getOptionValue('m');
        }

        if (line.hasOption('e')) // eperson
        {
            eperson = line.getOptionValue('e');
        }

        if (line.hasOption('c')) // collections
        {
            collections = line.getOptionValues('c');
        }

        if (line.hasOption('R'))
        {
            isResume = true;
            System.out
                    .println("**Resume import** - attempting to import items not already imported");
        }

        // now validate
        // must have a command set
        if (command == null)
        {
            System.out
                    .println("Error - must run with either add, replace, or remove (run with -h flag for details)");
            System.exit(1);
        }
        else if (command.equals("add") || command.equals("replace"))
        {
            if (sourcedir == null)
            {
                System.out
                        .println("Error - a source directory containing items must be set");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }

            if (mapfile == null)
            {
                System.out
                        .println("Error - a map file to hold importing results must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }

            if (eperson == null)
            {
                System.out
                        .println("Error - an eperson to do the importing must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }

            if (collections == null)
            {
                System.out
                        .println("Error - at least one destination collection must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
        }
        else if (command.equals("delete"))
        {
            if (eperson == null)
            {
                System.out
                        .println("Error - an eperson to do the importing must be specified");
                System.exit(1);
            }

            if (mapfile == null)
            {
                System.out.println("Error - a map file must be specified");
                System.exit(1);
            }
        }

        // can only resume for adds
        if (isResume && !command.equals("add"))
        {
            System.out
                    .println("Error - resume option only works with --add command");
            System.exit(1);
        }

        // do checks around mapfile - if mapfile exists and 'add' is selected,
        // resume must be chosen
        File myFile = new File(mapfile);

        if (myFile.exists() && command.equals("add") && !isResume)
        {
            System.out.println("Error - the mapfile " + mapfile
                    + " already exists.");
            System.out
                    .println("Either delete it or use --resume if attempting to resume an aborted import.");
            System.exit(1);
        }

        ItemImport myloader = new ItemImport();

        // create a context
        Context c = new Context();
        context =c ;

        collectionDAO = CollectionDAOFactory.getInstance(c);
        itemDAO = ItemDAOFactory.getInstance(c);
        identifierDAO = ExternalIdentifierDAOFactory.getInstance(c);

        // find the EPerson, assign to context
        EPerson myEPerson = null;

        if (eperson.indexOf('@') != -1)
        {
            // @ sign, must be an email
            myEPerson = EPerson.findByEmail(c, eperson);
        }
        else
        {
            myEPerson = EPerson.find(c, Integer.parseInt(eperson));
        }

        if (myEPerson == null)
        {
            System.out.println("Error, eperson cannot be found: " + eperson);
            System.exit(1);
        }

        c.setCurrentUser(myEPerson);

        // find collections
        Collection[] mycollections = null;

        // don't need to validate collections set if command is "delete"
        if (!command.equals("delete"))
        {
            System.out.println("Destination collections:");

            mycollections = new Collection[collections.length];

            // validate each collection arg to see if it's a real collection
            for (int i = 0; i < collections.length; i++)
            {
                ObjectIdentifier oid = ObjectIdentifier.parseCanonicalForm(collections[i]);
                DSpaceObject dso = oid.getObject(c);
                if (!(dso instanceof Collection))
                {
                    throw new IllegalArgumentException(collections[i] + " does not resolve to a Collection");
                }
                mycollections[i] = (Collection) dso;
                
                // print progress info
                String owningPrefix = "";

                if (i == 0)
                {
                    owningPrefix = "Owning ";
                }

                System.out.println(owningPrefix + " Collection: " + mycollections[i].getMetadata("name"));
            }
        }

        try
        {
            c.setIgnoreAuthorization(true);

            if (command.equals("add"))
            {
                myloader.addItems(c, mycollections, sourcedir, mapfile, template);
            }
            else if (command.equals("replace"))
            {
                myloader.replaceItems(c, mycollections, sourcedir, mapfile, template);
            }
            else if (command.equals("delete"))
            {
                myloader.deleteItems(c, mapfile);
            }

            // complete all transactions
            c.complete();
        }
        catch (Exception e)
        {
            // abort all operations
            if (mapOut != null)
            {
                mapOut.close();
            }

            mapOut = null;

            c.abort();
            e.printStackTrace();
            System.out.println(e);
            status = 1;
        }

        if (mapOut != null)
        {
            mapOut.close();
        }

        if (isTest)
        {
            System.out.println("***End of Test Run***");
        }
        System.exit(status);
    }

    private void addItems(Context c, Collection[] mycollections,
            String sourceDir, String mapFile, boolean template) throws Exception
    {
        Map skipItems = new HashMap(); // set of items to skip if in 'resume'
        // mode

        System.out.println("Adding items from directory: " + sourceDir);
        System.out.println("Generating mapfile: " + mapFile);

        // create the mapfile
        File outFile = null;

        if (!isTest)
        {
            // get the directory names of items to skip (will be in keys of
            // hash)
            if (isResume)
            {
                skipItems = readMapFile(mapFile);
            }

            // sneaky isResume == true means open file in append mode
            outFile = new File(mapFile);
            mapOut = new PrintWriter(new FileWriter(outFile, isResume));

            if (mapOut == null)
            {
                throw new Exception("can't open mapfile: " + mapFile);
            }
        }

        // open and process the source directory
        File d = new java.io.File(sourceDir);

        if (d == null)
        {
            System.out.println("Error, cannot open source directory "
                    + sourceDir);
            System.exit(1);
        }

        String[] dircontents = d.list(directoryFilter);

        for (int i = 0; i < dircontents.length; i++)
        {
            if (skipItems.containsKey(dircontents[i]))
            {
                System.out.println("Skipping import of " + dircontents[i]);
            }
            else
            {
                addItem(c, mycollections, sourceDir, dircontents[i], mapOut, template);
                System.out.println(i + " " + dircontents[i]);
            }
        }
    }

    private void replaceItems(Context c, Collection[] mycollections,
            String sourceDir, String mapFile, boolean template) throws Exception
    {
        /*
        // verify the source directory
        File d = new java.io.File(sourceDir);

        if (d == null)
        {
            System.out.println("Error, cannot open source directory "
                    + sourceDir);
            System.exit(1);
        }

        // read in HashMap first, to get list of uris & source dirs
        Map myhash = readMapFile(mapFile);

        // for each uri, re-import the item, discard the new uri
        // and re-assign the old uri
        Iterator i = myhash.keySet().iterator();
        ArrayList itemsToDelete = new ArrayList();

        while (i.hasNext())
        {
            // get the old uri
            String newItemName = (String) i.next();
            String oldURI = (String) myhash.get(newItemName);

            Item oldItem = null;
            Item newItem = null;

            if (oldURI.indexOf('/') != -1)
            {
                if (oldURI.indexOf(':') == -1)
                {
                    // has no : must be a handle
                    oldURI = "hdl:" + oldURI;
                    System.out.println("no namespace provided. assuming handles.");
                }

                System.out.println("\tReplacing:  " + oldURI);

                // add new item, locate old one
                ExternalIdentifier identifier = identifierDAO.retrieve(oldURI);
                ObjectIdentifier oi = identifier.getObjectIdentifier();
                oldItem = (Item) oi.getObject(c);
            }
            else
            {
                oldItem = itemDAO.retrieve(Integer.parseInt(oldURI));
            }
*/
            /*
             * FIXME: This isn't true any more, now that we have persistent
             * identifiers, and items can have as many as they need.
             *
             * Rather than exposing public item methods to change persistent
             * identifiers -- two handles can't exist at the same time due to
             * key constraints so would require temp handle being stored, old
             * being copied to new and new being copied to old, all a bit messy
             * -- a handle file is written to the import directory containing
             * the old handle, the existing item is deleted and then the
             * import runs as though it were loading an item which had already
             * been assigned a handle (so a new handle is not even assigned).
             * As a commit does not occur until after a successful add, it is
             * safe to do a delete as any error results in an aborted
             * transaction without harming the original item
             */
        /*
            File uriFile = new File(sourceDir + File.separatorChar + newItemName + File.separatorChar + "uri");
            PrintWriter uriOut = new PrintWriter(new FileWriter(uriFile, true));

            if (uriOut == null)
            {
                throw new Exception("can't open uri file: " + uriFile.getCanonicalPath());
            }

            uriOut.println(oldURI);
            uriOut.close();

            deleteItem(c, oldItem);

            newItem = addItem(c, mycollections, sourceDir, newItemName, null, template);

        }
    */
    }

    private void deleteItems(Context c, String mapFile) throws Exception
    {
        System.out.println("Deleting items listed in mapfile: " + mapFile);

        // read in the mapfile
        Map myhash = readMapFile(mapFile);

        // now delete everything that appeared in the mapFile
        Iterator i = myhash.keySet().iterator();

        while (i.hasNext())
        {
            String itemID = (String) myhash.get(i.next());

            if (itemID.indexOf('/') != -1)
            {
                if (itemID.indexOf(':') == -1)
                {
                    // has no : must be a handle
                    itemID = "hdl:" + itemID;
                    System.out.println("no namespace provided. assuming handles.");
                }

                String myURI = itemID;
                System.out.println("Deleting item " + myURI);
                deleteItem(c, myURI);
            }
            else
            {
                // it's an ID
                Item myitem = itemDAO.retrieve(Integer.parseInt(itemID));
                System.out.println("Deleting item " + itemID);
                deleteItem(c, myitem);
            }
        }
    }

    /**
     * item? try and add it to the archive c mycollection path itemname URI -
     * non-null means we have a pre-defined URI already mapOut - mapfile we're
     * writing
     */
    private Item addItem(Context c, Collection[] mycollections, String path,
            String itemname, PrintWriter mapOut, boolean template) throws Exception
    {
        WorkspaceItemDAO wsiDAO = WorkspaceItemDAOFactory.getInstance(c);
        String mapOutput = null;

        System.out.println("Adding item from directory " + itemname);

        // create workspace item
        Item myitem = null;
        WorkspaceItem wi = null;

        if (!isTest)
        {
            wi = wsiDAO.create(mycollections[0], template);
            myitem = wi.getItem();
        }

        // now fill out dublin core for item
        loadMetadata(c, myitem, path + File.separatorChar + itemname
                + File.separatorChar);

        // and the bitstreams from the contents file
        // process contents file, add bistreams and bundles, return any
        // non-standard permissions
        Vector options = processContentsFile(c, myitem, path
                + File.separatorChar + itemname, "contents");

        // get the suggested or desired identifiers for the item
        ObjectIdentifier oid = getObjectIdentifier(path + File.separatorChar + itemname + File.separatorChar);
        List<ExternalIdentifier> eids = getExternalIdentifiers(path + File.separatorChar + itemname + File.separatorChar);

        if (oid != null)
        {
            myitem.setIdentifier(oid);
        }
        
        if (eids.size() > 0)
        {
            myitem.setExternalIdentifiers(eids);
        }

        if (useWorkflow)
        {
            // don't process URI file
            // start up a workflow
            if (!isTest)
            {
                WorkflowManager.startWithoutNotify(c, wi);

                // send ID to the mapfile
                mapOutput = itemname + " " + myitem.getID();
            }
        }
        else
        {
            // only process URI file if not using workflow system
            String uri = processURIFile(c, myitem, path
                    + File.separatorChar + itemname, "uri");

            // put item in system
            if (!isTest)
            {
                Item item = InstallItem.installItem(c, wi);
//                Item item = InstallItem.installItem(c, wi, uri);

                uri = item.getIdentifier().getCanonicalForm();

                mapOutput = itemname + " " + uri;
            }

            // set permissions if specified in contents file
            if (options.size() > 0)
            {
                System.out.println("Processing options");
                processOptions(c, myitem, options);
            }
        }

        // now add to multiple collections if requested
        if (mycollections.length > 1)
        {
            for (int i = 1; i < mycollections.length; i++)
            {
                if (!isTest)
                {
                    mycollections[i].addItem(myitem);
                }
            }
        }

        // made it this far, everything is fine, commit transaction
        if (mapOut != null)
        {
            mapOut.println(mapOutput);
        }

        c.commit();

        return myitem;
    }

    // remove, given the actual item
    private void deleteItem(Context c, Item myitem) throws Exception
    {
        if (!isTest)
        {
            Collection[] collections = myitem.getCollections();

            // Remove item from all the collections it's in
            for (int i = 0; i < collections.length; i++)
            {
                collections[i].removeItem(myitem);
            }
        }
    }

    // remove, given a uri
    private void deleteItem(Context c, String uri) throws Exception
    {
        /* FIXME
        // bit of a hack - to remove an item, you must remove it
        // from all collections it's a part of, then it will be removed
        ExternalIdentifier identifier = identifierDAO.retrieve(uri);
        ObjectIdentifier oi = identifier.getObjectIdentifier();
        Item myitem = (Item) oi.getObject(c);

        if (myitem == null)
        {
            System.out.println("Error - cannot locate item - already deleted?");
        }
        else
        {
            deleteItem(c, myitem);
        }*/
    }

    ////////////////////////////////////
    // utility methods
    ////////////////////////////////////
    // read in the map file and generate a hashmap of (file,uri) pairs
    private Map readMapFile(String filename) throws Exception
    {
        Map myhash = new HashMap();

        BufferedReader is = null;
        try
        {
            is = new BufferedReader(new FileReader(filename));

            String line;

            while ((line = is.readLine()) != null)
            {
                String myfile;
                String myURI;

                // a line should be archive filename<whitespace>uri
                StringTokenizer st = new StringTokenizer(line);

                if (st.hasMoreTokens())
                {
                    myfile = st.nextToken();
                }
                else
                {
                    throw new Exception("Bad mapfile line:\n" + line);
                }

                if (st.hasMoreTokens())
                {
                    myURI = st.nextToken();
                }
                else
                {
                    throw new Exception("Bad mapfile line:\n" + line);
                }

                myhash.put(myfile, myURI);
            }
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }

        return myhash;
    }

    // Load all metadata schemas into the item.
    private void loadMetadata(Context c, Item myitem, String path)
        throws IOException, ParserConfigurationException, SAXException,
                          TransformerException
    {
        // Load the dublin core metadata
        loadDublinCore(c, myitem, path + "dublin_core.xml");

        // Load any additional metadata schemas
        File folder = new File(path);
        File file[] = folder.listFiles(metadataFileFilter);
        for (int i = 0; i < file.length; i++)
        {
            loadDublinCore(c, myitem, file[i].getAbsolutePath());
        }
    }

    private void loadDublinCore(Context c, Item myitem, String filename)
        throws IOException, ParserConfigurationException, SAXException,
                          TransformerException
    {
        Document document = loadXML(filename);

        // Get the schema, for backward compatibility we will default to the
        // dublin core schema if the schema name is not available in the import
        // file
        String schema;
        NodeList metadata = XPathAPI.selectNodeList(document, "/dublin_core");
        Node schemaAttr = metadata.item(0).getAttributes().getNamedItem(
                "schema");
        if (schemaAttr == null)
        {
            schema = MetadataSchema.DC_SCHEMA;
        }
        else
        {
            schema = schemaAttr.getNodeValue();
        }

        // Get the nodes corresponding to formats
        NodeList dcNodes = XPathAPI.selectNodeList(document,
                "/dublin_core/dcvalue");

        System.out.println("\tLoading dublin core from " + filename);

        // Add each one as a new format to the registry
        for (int i = 0; i < dcNodes.getLength(); i++)
        {
            Node n = dcNodes.item(i);
            addDCValue(c, myitem, schema, n);
        }
    }

    private void addDCValue(Context c, Item i, String schema, Node n)
        throws TransformerException
    {
        String value = getStringValue(n); //n.getNodeValue();
        // compensate for empty value getting read as "null", which won't display
        if (value == null)
            value = "";
        // //getElementData(n, "element");
        String element = getAttributeValue(n, "element");
        String qualifier = getAttributeValue(n, "qualifier"); //NodeValue();
        // //getElementData(n,
        // "qualifier");
        String language = getAttributeValue(n, "language");

        System.out.println("\tSchema: " + schema + " Element: " + element + " Qualifier: " + qualifier
                + " Value: " + value);

        if (qualifier.equals("none") || "".equals(qualifier))
        {
            qualifier = null;
        }

        // if language isn't set, use the system's default value
        if (language.equals(""))
        {
            language = ConfigurationManager.getProperty("default.language");
        }

        // a goofy default, but there it is
        if (language == null)
        {
            language = "en";
        }

        if (!isTest)
        {
            i.addMetadata(schema, element, qualifier, language, value);
        }
        else
        {
            // If we're just test the import, let's check that the actual metadata field exists.
        	MetadataSchema foundSchema = MetadataSchema.find(c,schema);

        	if (foundSchema == null)
        	{
        		System.out.println("ERROR: schema '"+schema+"' was not found in the registry.");
        		return;
        	}

        	int schemaID = foundSchema.getID();
        	MetadataField foundField = MetadataField.findByElement(c, schemaID, element, qualifier);

        	if (foundField == null)
        	{
        		System.out.println("ERROR: Metadata field: '"+schema+"."+element+"."+qualifier+"' was not found in the registry.");
        		return;
            }
        }
    }

    /**
     * Read in the URI file or return null if empty or doesn't exist
     */
    private String processURIFile(Context c, Item i, String path,
            String filename)
    {
        String filePath = path + File.separatorChar + filename;
        String line = "";
        String result = null;

        System.out.println("Processing URI file: " + filename);
        BufferedReader is = null;
        try
        {
            is = new BufferedReader(new FileReader(filePath));

            // result gets contents of file, or null
            result = is.readLine();

            System.out.println("read URI: '" + result + "'");

        }
        catch (Exception e)
        {
            // probably no URI file, just return null
            System.out.println(
                    "It appears there is no URI file -- generating one");
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e1)
                {
                    System.err
                            .println("Non-critical problem releasing resources.");
                }
            }
        }

        return result;
    }

    /**
     * Given a contents file and an item, stuffing it with bitstreams from the
     * contents file Returns a Vector of Strings with lines from the contents
     * file that request non-default bitstream permission
     */
    private Vector processContentsFile(Context c, Item i, String path,
            String filename) throws IOException, AuthorizeException
    {
        String contentspath = path + File.separatorChar + filename;
        String line = "";
        Vector options = new Vector();

        System.out.println("\tProcessing contents file: " + contentspath);

        BufferedReader is = null;
        try
        {
            is = new BufferedReader(new FileReader(contentspath));

            while ((line = is.readLine()) != null)
            {
                if ("".equals(line.trim()))
                {
                    continue;
                }

            	//	1) registered into dspace (leading -r)
            	//  2) imported conventionally into dspace (no -r)
            	if (line.trim().startsWith("-r "))
            	{
            	    // line should be one of these two:
            	    // -r -s n -f filepath
            	    // -r -s n -f filepath\tbundle:bundlename
            	    // where
            	    //		n is the assetstore number
            	    //  	filepath is the path of the file to be registered
            	    //  	bundlename is an optional bundle name
            	    String sRegistrationLine = line.trim();
            	    int iAssetstore = -1;
            	    String sFilePath = null;
            	    String sBundle = null;
                    StringTokenizer tokenizer =
                        	new StringTokenizer(sRegistrationLine);
                    while (tokenizer.hasMoreTokens())
                    {
                        String sToken = tokenizer.nextToken();
                        if (sToken.equals("-r"))
                        {
                            continue;
                        }
                        else if (sToken.equals("-s") && tokenizer.hasMoreTokens())
                        {
                            try
                            {
                                iAssetstore =
                                    Integer.parseInt(tokenizer.nextToken());
                            }
                            catch (NumberFormatException e)
                            {
                                // ignore - iAssetstore remains -1
                            }
                        }
                        else if (sToken.equals("-f") && tokenizer.hasMoreTokens())
                        {
                            sFilePath = tokenizer.nextToken();

                        }
                        else if (sToken.startsWith("bundle:"))
                        {
                            sBundle = sToken.substring(7);
                        }
                        else
                        {
                            // unrecognized token - should be no problem
                        }
                    } // while
                    if (iAssetstore == -1 || sFilePath == null)
                    {
                        System.out.println("\tERROR: invalid contents file line");
                        System.out.println("\t\tSkipping line: "
                                + sRegistrationLine);
                        continue;
                    }
                    registerBitstream(c, i, iAssetstore, sFilePath, sBundle);
                    System.out.println("\tRegistering Bitstream: " + sFilePath
                            + "\tAssetstore: " + iAssetstore
                            + "\tBundle: " + sBundle);
                    continue;				// process next line in contents file
            	}

                int bitstreamEndIndex = line.indexOf("\t");

                if (bitstreamEndIndex == -1)
                {
                    // no extra info
                    processContentFileEntry(c, i, path, line, null);
                    System.out.println("\tBitstream: " + line);
                }
                else
                {

                    String bitstreamName = line.substring(0, bitstreamEndIndex);

                    boolean bundleExists = false;
                    boolean permissionsExist = false;
                    boolean descriptionExists = false;

                    // look for a bundle name
                    String bundleMarker = "\tbundle:";
                    int bMarkerIndex = line.indexOf(bundleMarker);
                    int bEndIndex = 0;
                    if (bMarkerIndex > 0)
                    {
                        bEndIndex = line.indexOf("\t", bMarkerIndex + 1);
                        if (bEndIndex == -1)
                        {
                            bEndIndex = line.length();
                        }
                        bundleExists = true;
                    }

                    // look for permissions
                    String permissionsMarker = "\tpermissions:";
                    int pMarkerIndex = line.indexOf(permissionsMarker);
                    int pEndIndex = 0;
                    if (pMarkerIndex > 0)
                    {
                        pEndIndex = line.indexOf("\t", pMarkerIndex + 1);
                        if (pEndIndex == -1)
                        {
                            pEndIndex = line.length();
                        }
                        permissionsExist = true;
                    }

                    // look for descriptions
                    String descriptionMarker = "\tdescription:";
                    int dMarkerIndex = line.indexOf(descriptionMarker);
                    int dEndIndex = 0;
                    if (dMarkerIndex > 0)
                    {
                        dEndIndex = line.indexOf("\t", dMarkerIndex + 1);
                        if (dEndIndex == -1)
                        {
                            dEndIndex = line.length();
                        }
                        descriptionExists = true;
                    }

                    if (bundleExists)
                    {
                        String bundleName = line.substring(bMarkerIndex
                                + bundleMarker.length(), bEndIndex);

                        processContentFileEntry(c, i, path, bitstreamName,
                                bundleName);
                        System.out.println("\tBitstream: " + bitstreamName
                                + "\tBundle: " + bundleName);
                    }
                    else
                    {
                        processContentFileEntry(c, i, path, bitstreamName, null);
                        System.out.println("\tBitstream: " + bitstreamName);
                    }

                    if (permissionsExist || descriptionExists)
                    {
                        String extraInfo = bitstreamName;

                        if (permissionsExist)
                        {
                            extraInfo = extraInfo
                                    + line.substring(pMarkerIndex, pEndIndex);
                        }

                        if (descriptionExists)
                        {
                            extraInfo = extraInfo
                                    + line.substring(dMarkerIndex, dEndIndex);
                        }

                        options.add(extraInfo);
                    }
                }
            }
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }
        return options;
    }

    /**
     * each entry represents a bitstream....
     * @param c
     * @param i
     * @param path
     * @param fileName
     * @param bundleName
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    private void processContentFileEntry(Context c, Item i, String path,
            String fileName, String bundleName)
        throws IOException, AuthorizeException
    {
        BitstreamDAO bsDAO = BitstreamDAOFactory.getInstance(c);
        String fullpath = path + File.separatorChar + fileName;

        // get an input stream
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
                fullpath));

        Bitstream bs = null;
        String newBundleName = bundleName;

        if (bundleName == null)
        {
            // is it license.txt?
            if (fileName.equals("license.txt"))
            {
                newBundleName = "LICENSE";
            }
            else
            {
                // call it ORIGINAL
                newBundleName = "ORIGINAL";
            }
        }

        if (!isTest)
        {
            // find the bundle
            Bundle[] bundles = i.getBundles(newBundleName);
            Bundle targetBundle = null;

            if (bundles.length < 1)
            {
                // not found, create a new one
                targetBundle = i.createBundle(newBundleName);
            }
            else
            {
                // put bitstreams into first bundle
                targetBundle = bundles[0];
            }

            // now add the bitstream
            bs = targetBundle.createBitstream(bis);

            bs.setName(fileName);

            // Identify the format
            // FIXME - guessing format guesses license.txt incorrectly as a text
            // file format!
            BitstreamFormat bf = FormatIdentifier.guessFormat(c, bs);
            bs.setFormat(bf);

            bsDAO.update(bs);
        }
    }

    /**
     * Register the bitstream file into DSpace
     * 
     * @param c
     * @param i
     * @param assetstore
     * @param bitstreamPath the full filepath expressed in the contents file
     * @param bundleName
     * @throws IOException
     * @throws AuthorizeException
     */
    private void registerBitstream(Context c, Item i, int assetstore, 
            String bitstreamPath, String bundleName )
        throws IOException, AuthorizeException
    {
        // TODO validate assetstore number
        // TODO make sure the bitstream is there

        Bitstream bs = null;
        String newBundleName = bundleName;
        
        if (bundleName == null)
        {
            // is it license.txt?
            if (bitstreamPath.endsWith("license.txt"))
            {
                newBundleName = "LICENSE";
            }
            else
            {
                // call it ORIGINAL
                newBundleName = "ORIGINAL";
            }
        }

        if(!isTest)
        {
        	// find the bundle
	        Bundle[] bundles = i.getBundles(newBundleName);
	        Bundle targetBundle = null;
	            
	        if( bundles.length < 1 )
	        {
	            // not found, create a new one
	            targetBundle = i.createBundle(newBundleName);
	        }
	        else
	        {
	            // put bitstreams into first bundle
	            targetBundle = bundles[0];
	        }
	
	        // now add the bitstream
	        bs = targetBundle.registerBitstream(assetstore, bitstreamPath);
	
	        // set the name to just the filename
	        int iLastSlash = bitstreamPath.lastIndexOf('/');
	        bs.setName(bitstreamPath.substring(iLastSlash + 1));
	
	        // Identify the format
	        // FIXME - guessing format guesses license.txt incorrectly as a text file format!
	        BitstreamFormat bf = FormatIdentifier.guessFormat(c, bs);
	        bs.setFormat(bf);
	
	        bs.update();
        }
    }

    /**
     * 
     * Process the Options to apply to the Item. The options are tab delimited
     * 
     * Options:
     *      48217870-MIT.pdf        permissions: -r 'MIT Users'     description: Full printable version (MIT only)
     *      permissions:[r|w]-['group name']
     *      description: 'the description of the file'
     *      
     *      where:
     *          [r|w] (meaning: read|write)
     *          ['MIT Users'] (the group name)
     *          
     * @param c
     * @param myItem
     * @param options
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void processOptions(Context c, Item myItem, Vector options)
            throws AuthorizeException
    {
        for (int i = 0; i < options.size(); i++)
        {
            String line = options.elementAt(i).toString();

            System.out.println("\tprocessing " + line);

            boolean permissionsExist = false;
            boolean descriptionExists = false;

            String permissionsMarker = "\tpermissions:";
            int pMarkerIndex = line.indexOf(permissionsMarker);
            int pEndIndex = 0;
            if (pMarkerIndex > 0)
            {
                pEndIndex = line.indexOf("\t", pMarkerIndex + 1);
                if (pEndIndex == -1)
                {
                    pEndIndex = line.length();
                }
                permissionsExist = true;
            }

            String descriptionMarker = "\tdescription:";
            int dMarkerIndex = line.indexOf(descriptionMarker);
            int dEndIndex = 0;
            if (dMarkerIndex > 0)
            {
                dEndIndex = line.indexOf("\t", dMarkerIndex + 1);
                if (dEndIndex == -1)
                {
                    dEndIndex = line.length();
                }
                descriptionExists = true;
            }

            int bsEndIndex = line.indexOf("\t");
            String bitstreamName = line.substring(0, bsEndIndex);

            int actionID = -1;
            String groupName = "";
            Group myGroup = null;
            if (permissionsExist)
            {
                String thisPermission = line.substring(pMarkerIndex
                        + permissionsMarker.length(), pEndIndex);

                // get permission type ("read" or "write")
                int pTypeIndex = thisPermission.indexOf("-");

                // get permission group (should be in single quotes)
                int groupIndex = thisPermission.indexOf("'", pTypeIndex);
                int groupEndIndex = thisPermission.indexOf("'", groupIndex + 1);

                // if not in single quotes, assume everything after type flag is
                // group name
                if (groupIndex == -1)
                {
                    groupIndex = thisPermission.indexOf(" ", pTypeIndex);
                    groupEndIndex = thisPermission.length();
                }

                groupName = thisPermission.substring(groupIndex + 1,
                        groupEndIndex);

                if (thisPermission.toLowerCase().charAt(pTypeIndex + 1) == 'r')
                {
                    actionID = Constants.READ;
                }
                else if (thisPermission.toLowerCase().charAt(pTypeIndex + 1) == 'w')
                {
                    actionID = Constants.WRITE;
                }

                GroupDAO groupDAO = GroupDAOFactory.getInstance(c);
                myGroup = groupDAO.retrieve(groupName);
            }

            String thisDescription = "";
            if (descriptionExists)
            {
                thisDescription = line.substring(
                        dMarkerIndex + descriptionMarker.length(), dEndIndex)
                        .trim();
            }

            Bitstream bs = null;
            boolean notfound = true;
            if (!isTest)
            {
                // find bitstream
                Bitstream[] bitstreams = myItem.getNonInternalBitstreams();
                for (int j = 0; j < bitstreams.length && notfound; j++)
                {
                    if (bitstreams[j].getName().equals(bitstreamName))
                    {
                        bs = bitstreams[j];
                        notfound = false;
                    }
                }
            }

            if (notfound && !isTest)
            {
                // this should never happen
                System.out.println("\tdefault permissions set for "
                        + bitstreamName);
            }
            else if (!isTest)
            {
                if (permissionsExist)
                {
                    if (myGroup == null)
                    {
                        System.out.println("\t" + groupName
                                + " not found, permissions set to default");
                    }
                    else if (actionID == -1)
                    {
                        System.out
                                .println("\tinvalid permissions flag, permissions set to default");
                    }
                    else
                    {
                        System.out.println("\tSetting special permissions for "
                                + bitstreamName);
                        setPermission(c, myGroup, actionID, bs);
                    }
                }

                if (descriptionExists)
                {
                    System.out.println("\tSetting description for "
                            + bitstreamName);
                    bs.setDescription(thisDescription);
                    bs.update();
                }
            }
        }
    }

    /**
     * Set the Permission on a Bitstream.
     * 
     * @param c
     * @param g
     * @param actionID
     * @param bs
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void setPermission(Context c, Group g, int actionID, Bitstream bs)
            throws AuthorizeException
    {
        if (!isTest)
        {
            ResourcePolicyDAO rpDAO = ResourcePolicyDAOFactory.getInstance(c);

            // remove the default policy
            AuthorizeManager.removeAllPolicies(c, bs);

            // add the policy
            ResourcePolicy rp = rpDAO.create();

            rp.setResource(bs);
            rp.setAction(actionID);
            rp.setGroup(g);

            rpDAO.update(rp);
        }
        else
        {
            if (actionID == Constants.READ)
            {
                System.out.println("\t\tpermissions: READ for " + g.getName());
            }
            else if (actionID == Constants.WRITE)
            {
                System.out.println("\t\tpermissions: WRITE for " + g.getName());
            }
        }

    }

    // XML utility methods
    /**
     * Lookup an attribute from a DOM node.
     * @param n
     * @param name
     * @return
     */
    private String getAttributeValue(Node n, String name)
    {
        NamedNodeMap nm = n.getAttributes();

        for (int i = 0; i < nm.getLength(); i++)
        {
            Node node = nm.item(i);

            if (name.equals(node.getNodeName()))
            {
                return node.getNodeValue();
            }
        }

        return "";
    }

    
    /**
     * Return the String value of a Node.
     * @param node
     * @return
     */
    private String getStringValue(Node node)
    {
        String value = node.getNodeValue();

        if (node.hasChildNodes())
        {
            Node first = node.getFirstChild();

            if (first.getNodeType() == Node.TEXT_NODE)
            {
                return first.getNodeValue();
            }
        }

        return value;
    }

    /**
     * Load in the XML from file.
     * 
     * @param filename
     *            the filename to load from
     * 
     * @return the DOM representation of the XML file
     */
    private static Document loadXML(String filename)
        throws IOException, ParserConfigurationException, SAXException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();

        return builder.parse(new File(filename));
    }

    /**
     * Returns the first internal identifier encoungered in the uri file
     * 
     * @param directory
     * @return
     * @throws IOException
     */
    private ObjectIdentifier getObjectIdentifier(String directory)
        throws IOException
    {
        File uriFile = new File(directory + "uri");
        FileReader reader = new FileReader(uriFile);
        BufferedReader br = new BufferedReader(reader);
        String line = null;
        while ((line = br.readLine()) != null)
        {
            ObjectIdentifier oid = ObjectIdentifier.parseCanonicalForm(line);
            if (oid != null)
            {
                return oid;
            }
        }

        return null;   
    }

    private List<ExternalIdentifier> getExternalIdentifiers(String directory)
            throws IOException
    {
        File uriFile = new File(directory + "uri");
        FileReader reader = new FileReader(uriFile);
        BufferedReader br = new BufferedReader(reader);
        String line = null;
        ArrayList<ExternalIdentifier> eids = new ArrayList<ExternalIdentifier>();
        // ExternalIdentifier[] eidPlugins = (ExternalIdentifier[]) PluginManager.getPluginSequence(ExternalIdentifier.class);
        while ((line = br.readLine()) != null)
        {
            ExternalIdentifier eid = ExternalIdentifierService.parseCanonicalForm(context, line);
            if (eid != null)
            {
                eids.add(eid);
            }
            /*
            for (int i = 0; i < eidPlugins.length; i++)
            {
                ExternalIdentifier eid = eidPlugins[i].parseCanonicalForm(line);
                if (eid != null)
                {
                    eids.add(eid);
                }
            }*/
        }
        
        return eids;
    }
}
