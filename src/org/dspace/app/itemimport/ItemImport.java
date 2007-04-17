/*
 * ItemImport.java
 *
 * $Id: ItemImport.java,v 1.23 2005/03/10 04:37:48 syeadon Exp $
 *
 * Version: $Revision: 1.23 $
 *
 * Date: $Date: 2005/03/10 04:37:48 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
 *
 * <pre>
 * Revision History
 *
 *   2005/08/03: Ben
 *     - handle null dc value
 *
 *   2005/07/15: Ben
 *     - don't check for duplicate title when doing --test
 *
 *   2005/05/06: Ben
 *     - handle conflict between --title-check and --test
 *     - don't disable workflow notification
 *
 *   2005/02/11: Ben
 *     - handle multiple collections when reporting duplicate titles
 *
 *   2004/07/15: Ben
 *     - add option to send email notification of a duplicate
 *       title loaded
 *
 *   2004/03/30: Ben
 *     - call System.exit(1) on an Exception
 *
 * </pre>
 */
package org.dspace.app.itemimport;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.xpath.XPathAPI;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.Browse;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Email;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.WorkflowManager;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The Item importer does exactly that - imports items into the repository.
 */
public class ItemImport
{
    static boolean useWorkflow = false;
    static boolean checkTitle = false;
    static Connection connection = null;
    static PreparedStatement st = null;
  
    static boolean isTest = false;

    static boolean isResume = false;

    static PrintWriter mapOut = null;

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
                "destination collection(s) Handle or database ID");
        options.addOption("m", "mapfile", true, "mapfile items in mapfile");
        options.addOption("e", "eperson", true,
                "email of eperson doing importing");
        options.addOption("w", "workflow", false,
                "send submission through collection's workflow");
        options.addOption("t", "test", false,
                "test run - do not actually import items");
        options.addOption("R", "resume", false,
                "resume a failed import (add only)");

        options.addOption("h", "help", false, "help");
        options.addOption("z", "title-check", false, "check for duplicate titles (if yes, send notification)");

        CommandLine line = parser.parse(options, argv);

        String command = null; // add replace remove, etc
        String sourcedir = null;
        String mapfile = null;
        String eperson = null; // db ID or email
        String[] collections = null; // db ID or handles

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
        if( line.hasOption( 'z' ) ) { checkTitle = true; }

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

	// open the database connection
	if (checkTitle) {
	    connection = c.getDBConnection();
	    st = connection.prepareStatement("select count(distinct item_id) from itemsbytitle where sort_title=?");
	}
 
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
                // is the ID a handle?
                if (collections[i].indexOf('/') != -1)
                {
                    // string has a / so it must be a handle - try and resolve
                    // it
                    mycollections[i] = (Collection) HandleManager
                            .resolveToObject(c, collections[i]);

                    // resolved, now make sure it's a collection
                    if ((mycollections[i] == null)
                            || (mycollections[i].getType() != Constants.COLLECTION))
                    {
                        mycollections[i] = null;
                    }
                }
                // not a handle, try and treat it as an integer collection
                // database ID
                else if (collections[i] != null)
                {
                    mycollections[i] = Collection.find(c, Integer
                            .parseInt(collections[i]));
                }

                // was the collection valid?
                if (mycollections[i] == null)
                {
                    throw new IllegalArgumentException("Cannot resolve "
                            + collections[i] + " to collection");
                }

                // print progress info
                String owningPrefix = "";

                if (i == 0)
                {
                    owningPrefix = "Owning ";
                }

                System.out.println(owningPrefix + " Collection: "
                        + mycollections[i].getMetadata("name"));
            }
        } // end of validating collections

        try
        {
            c.setIgnoreAuthorization(true);

            if (command.equals("add"))
            {
                myloader.addItems(c, mycollections, sourcedir, mapfile);
            }
            else if (command.equals("replace"))
            {
                myloader.replaceItems(c, mycollections, sourcedir, mapfile);
            }
            else if (command.equals("delete"))
            {
                myloader.deleteItems(c, mapfile);
            }

 	    // complete database session
	    if (checkTitle) {
		st.close();
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
	    System.exit(1);
        }

        if (mapOut != null)
        {
            mapOut.close();
        }

        if (isTest)
        {
            System.out.println("***End of Test Run***");
        }
    }

    private void addItems(Context c, Collection[] mycollections,
            String sourceDir, String mapFile) throws Exception
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

        String[] dircontents = d.list();

        for (int i = 0; i < dircontents.length; i++)
        {
            if (skipItems.containsKey(dircontents[i]))
            {
                System.out.println("Skipping import of " + dircontents[i]);
            }
            else
            {
                addItem(c, mycollections, sourceDir, dircontents[i], mapOut);
                System.out.println(i + " " + dircontents[i]);
            }
        }
    }

    private void replaceItems(Context c, Collection[] mycollections,
            String sourceDir, String mapFile) throws Exception
    {
        // verify the source directory
        File d = new java.io.File(sourceDir);

        if (d == null)
        {
            System.out.println("Error, cannot open source directory "
                    + sourceDir);
            System.exit(1);
        }

        // read in HashMap first, to get list of handles & source dirs
        Map myhash = readMapFile(mapFile);

        // for each handle, re-import the item, discard the new handle
        // and re-assign the old handle
        Iterator i = myhash.keySet().iterator();
        ArrayList itemsToDelete = new ArrayList();

        while (i.hasNext())
        {
            // get the old handle
            String newItemName = (String) i.next();
            String oldHandle = (String) myhash.get(newItemName);

            Item oldItem = null;
            Item newItem = null;

            if (oldHandle.indexOf('/') != -1)
            {
                System.out.println("\tReplacing:  " + oldHandle);

                // add new item, locate old one
                oldItem = (Item) HandleManager.resolveToObject(c, oldHandle);
            }
            else
            {
                oldItem = Item.find(c, Integer.parseInt(oldHandle));
            }
            
            /* Rather than exposing public item methods to change handles -- 
             * two handles can't exist at the same time due to key constraints
             * so would require temp handle being stored, old being copied to new and
             * new being copied to old, all a bit messy -- a handle file is written to
             * the import directory containing the old handle, the existing item is 
             * deleted and then the import runs as though it were loading an item which 
             * had already been assigned a handle (so a new handle is not even assigned).
             * As a commit does not occur until after a successful add, it is safe to 
             * do a delete as any error results in an aborted transaction without harming
             * the original item */
            File handleFile = new File(sourceDir + File.separatorChar + newItemName + File.separatorChar + "handle");
            PrintWriter handleOut = new PrintWriter(new FileWriter(handleFile, true));

            if (handleOut == null)
            {
                throw new Exception("can't open handle file: " + handleFile.getCanonicalPath());
            }
            
            handleOut.println(oldHandle);
            handleOut.close();
            
            deleteItem(c, oldItem);
            
            newItem = addItem(c, mycollections, sourceDir, newItemName, null);
        }
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
                String myhandle = itemID;
                System.out.println("Deleting item " + myhandle);
                deleteItem(c, myhandle);
            }
            else
            {
                // it's an ID
                Item myitem = Item.find(c, Integer.parseInt(itemID));
                System.out.println("Deleting item " + itemID);
                deleteItem(c, myitem);
            }
        }
    }

    /**
     * item? try and add it to the archive c mycollection path itemname handle -
     * non-null means we have a pre-defined handle already mapOut - mapfile
     * we're writing
     */
    private Item addItem(Context c, Collection[] mycollections, String path,
            String itemname, PrintWriter mapOut) throws Exception
    {
        String mapOutput = null;

        System.out.println("Adding item from directory " + itemname);

        // create workspace item
        Item myitem = null;
        WorkspaceItem wi = null;

        if (!isTest)
        {
            wi = WorkspaceItem.create(c, mycollections[0], false);
            myitem = wi.getItem();
        }

        // now fill out dublin core for item
        loadDublinCore(c, myitem, path + File.separatorChar + itemname
                + File.separatorChar + "dublin_core.xml");

        // and the bitstreams from the contents file
        // process contents file, add bistreams and bundles
        processContentsFile(c, myitem, path + File.separatorChar + itemname,
                "contents");

        if (useWorkflow)
        {
            // don't process handle file
            // start up a workflow
            if (!isTest)
            {
                WorkflowManager.start(c, wi);

                // send ID to the mapfile
                mapOutput = itemname + " " + myitem.getID();
            }
        }
        else
        {
            // only process handle file if not using workflow system
            String myhandle = processHandleFile(c, myitem, path
                    + File.separatorChar + itemname, "handle");

            // put item in system
            if (!isTest)
            {
                InstallItem.installItem(c, wi, myhandle);

                // find the handle, and output to map file
                myhandle = HandleManager.findHandle(c, myitem);

                mapOutput = itemname + " " + myhandle;
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

	// check for duplicate titles
	if (checkTitle && !isTest) {
	    checkTitle(c, myitem, mycollections);
	}
 
        return myitem;
    }

    // check for duplicate titles
    private void checkTitle(Context c, Item item, Collection[] collections)
	throws Exception
    {
	// Get the list of collections
	StringBuffer sbCollections = new StringBuffer();
	for (int j=0; j < collections.length; j++) {
	    if (sbCollections.length() > 0) {
		sbCollections.append(", ");
	    }
	    sbCollections.append(collections[j].getMetadata("name"));
	}

	// Get the title(s)
	DCValue dc[] = item.getDC("title", null, Item.ANY);

	// Process each title
	for (int i=0; i < dc.length; i++) {
	    String title = Browse.getNormalizedTitle(dc[i].value, dc[i].language);

	    st.setString(1, title);
	    ResultSet rs = st.executeQuery();
	    if (rs.next()) {
		int count = rs.getInt(1);
		
		if (count > 1) {
		    System.out.println("Duplicate title: " + title);
					  
		    // Send an email notice
		    
		    // Get the email recipient
		    String email = ConfigurationManager.getProperty("mail.duplicate_title");
		    if (email == null) {
			email = ConfigurationManager.getProperty("mail.admin");
		    }
		    
		    if (email != null) {
			// Send the email
			Email bean = ConfigurationManager.getEmail("duplicate_title");
			bean.addRecipient(email);
			bean.addArgument(title);
			bean.addArgument(""+item.getID());
			bean.addArgument(HandleManager.findHandle(c, item));
			bean.addArgument(sbCollections.toString());
			bean.send();
		    }
		}
		rs.close();
	    }
	}
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

    // remove, given a handle
    private void deleteItem(Context c, String myhandle) throws Exception
    {
        // bit of a hack - to remove an item, you must remove it
        // from all collections it's a part of, then it will be removed
        Item myitem = (Item) HandleManager.resolveToObject(c, myhandle);

        if (myitem == null)
        {
            System.out.println("Error - cannot locate item - already deleted?");
        }
        else
        {
            deleteItem(c, myitem);
        }
    }

    ////////////////////////////////////
    // utility methods
    ////////////////////////////////////
    // read in the map file and generate a hashmap of (file,handle) pairs
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
                String myhandle;

                // a line should be archive filename<whitespace>handle
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
                    myhandle = st.nextToken();
                }
                else
                {
                    throw new Exception("Bad mapfile line:\n" + line);
                }

                myhash.put(myfile, myhandle);
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

    private void loadDublinCore(Context c, Item myitem, String filename)
            throws SQLException, IOException, ParserConfigurationException,
            SAXException, TransformerException //, AuthorizeException
    {
        Document document = loadXML(filename);

        // Get the nodes corresponding to formats
        NodeList dcNodes = XPathAPI.selectNodeList(document,
                "/dublin_core/dcvalue");

        System.out.println("\tLoading dublin core from " + filename);

        // Add each one as a new format to the registry
        for (int i = 0; i < dcNodes.getLength(); i++)
        {
            Node n = dcNodes.item(i);
            addDCValue(myitem, n);
        }
    }

    private void addDCValue(Item i, Node n) throws TransformerException
    {
        String value = getStringValue(n); //n.getNodeValue();
        // //getElementData(n, "element");
        String element = getAttributeValue(n, "element");
        String qualifier = getAttributeValue(n, "qualifier"); //NodeValue();
        // //getElementData(n,
        // "qualifier");
        String language = getAttributeValue(n, "language");

        System.out.println("\tElement: " + element + " Qualifier: " + qualifier
                + " Value: " + value);

		  if (value == null)
		  {
				value = "";
		  }

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
            i.addDC(element, qualifier, language, value);
        }
    }

    /**
     * Return the String value of a Node
     */
    public String getStringValue(Node node)
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
     * Read in the handle file or return null if empty or doesn't exist
     */
    private String processHandleFile(Context c, Item i, String path,
            String filename)
    {
        String filePath = path + File.separatorChar + filename;
        String line = "";
        String result = null;

        System.out.println("Processing handle file: " + filename);
        BufferedReader is = null;
        try
        {
            is = new BufferedReader(new FileReader(filePath));

            // result gets contents of file, or null
            result = is.readLine();

            System.out.println("read handle: '" + result + "'");

        }
        catch (Exception e)
        {
            // probably no handle file, just return null
            System.out
                    .println("It appears there is no handle file -- generating one");
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
     * contents file
     */
    private void processContentsFile(Context c, Item i, String path,
            String filename) throws SQLException, IOException,
            AuthorizeException
    {
        String contentspath = path + File.separatorChar + filename;
        String line = "";

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

                // look for a bundle name
                String bundleMarker = "\tbundle:";

                int markerIndex = line.indexOf(bundleMarker);

                if (markerIndex == -1)
                {
                    // no bundle found
                    processContentFileEntry(c, i, path, line, null);
                    System.out.println("\tBitstream: " + line);
                }
                else
                {
                    // found bundle
                    String bundleName = line.substring(markerIndex
                            + bundleMarker.length());
                    String bitstreamName = line.substring(0, markerIndex);

                    processContentFileEntry(c, i, path, bitstreamName,
                            bundleName);
                    System.out.println("\tBitstream: " + bitstreamName
                            + "\tBundle: " + bundleName);
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
    }

    // each entry represents a bitstream....
    public void processContentFileEntry(Context c, Item i, String path,
            String fileName, String bundleName) throws SQLException,
            IOException, AuthorizeException
    {
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

            bs.update();
        }
    }

    // XML utility methods
    public String getAttributeValue(Node n, String myattributename)
    {
        String myvalue = "";

        NamedNodeMap nm = n.getAttributes();

        for (int i = 0; i < nm.getLength(); i++)
        {
            Node node = nm.item(i);
            String name = node.getNodeName();
            String value = node.getNodeValue();

            if (myattributename.equals(name))
            {
                return value;
            }
        }

        return myvalue;
    }

    // XML utility methods stolen from administer.

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
    private String getElementData(Node parentElement, String childName)
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
}
