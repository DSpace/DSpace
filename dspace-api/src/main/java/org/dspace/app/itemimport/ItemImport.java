/*
 * ItemImport.java
 *
 * Version: $Revision: 4563 $
 *
 * Date: $Date: 2009-11-20 22:01:30 -0500 (Fri, 20 Nov 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Email;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.WorkflowManager;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
    static boolean checkTitle = false;
    static Connection connection = null;
    static PreparedStatement st = null;
  
    static boolean useWorkflowSendEmail = false;

    static boolean isTest = false;

    static boolean isResume = false;
    
    static boolean template = false;

    static PrintWriter mapOut = null;

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
        options.addOption("z", "zip", true, "name of zip file");
        options.addOption("c", "collection", true,
                "destination collection(s) Handle or database ID");
        options.addOption("m", "mapfile", true, "mapfile items in mapfile");
        options.addOption("e", "eperson", true,
                "email of eperson doing importing");
        options.addOption("w", "workflow", false,
                "send submission through collection's workflow");
        options.addOption("n", "notification", false,
                        "if sending submissions through the workflow, send notification emails");
        options.addOption("t", "test", false,
                "test run - do not actually import items");
        options.addOption("p", "template", false, "apply template");
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
        int status = 0;

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ItemImport\n", options);
            System.out
                   .println("\nadding items:    ItemImport -a -e eperson -c collection -s sourcedir -m mapfile");
            System.out
                    .println("\nadding items from zip file:    ItemImport -a -e eperson -c collection -s sourcedir -z filename.zip -m mapfile");
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
            if (line.hasOption('n'))
            {
                useWorkflowSendEmail = true;
            }
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
        if( line.hasOption( 'z' ) ) { checkTitle = true; }

        if (line.hasOption('R'))
        {
            isResume = true;
            System.out
                    .println("**Resume import** - attempting to import items not already imported");
        }

        boolean zip = false;
        String zipfilename = "";
        String ziptempdir = ConfigurationManager.getProperty("org.dspace.app.itemexport.work.dir");
        if (line.hasOption('z'))
        {
            zip = true;
            zipfilename = sourcedir + System.getProperty("file.separator") + line.getOptionValue('z');
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

        // does the zip file exist and can we write to the temp directory
        if (zip)
        {
            File zipfile = new File(sourcedir);
            if (!zipfile.canRead())
            {
                System.out.println("Zip file '" + sourcedir + "' does not exist, or is not readable.");
                System.exit(1);
            }

            if (ziptempdir == null)
            {
                System.out.println("Unable to unzip import file as the key 'org.dspace.app.itemexport.work.dir' is not set in dspace.cfg");
                System.exit(1);
            }
            zipfile = new File(ziptempdir);
            if (!zipfile.isDirectory())
            {
                System.out.println("'" + ConfigurationManager.getProperty("org.dspace.app.itemexport.work.dir") +
                                   "' as defined by the key 'org.dspace.app.itemexport.work.dir' in dspace.cfg " +
                                   "is not a valid directory");
                System.exit(1);
            }
            File tempdir = new File(ziptempdir);
            tempdir.mkdirs();
            sourcedir = ziptempdir + System.getProperty("file.separator") + line.getOptionValue("z");
            ziptempdir = ziptempdir + System.getProperty("file.separator") +
                         line.getOptionValue("z") + System.getProperty("file.separator");
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
            // If this is a zip archive, unzip it first
            if (zip)
            {
                ZipFile zf = new ZipFile(zipfilename);
                ZipEntry entry;
                Enumeration entries = zf.entries();
                while (entries.hasMoreElements())
                {
                    entry = (ZipEntry)entries.nextElement();
                    if (entry.isDirectory())
                    {
                        new File(ziptempdir + entry.getName()).mkdir();
                    }
                    else
                    {
                        System.out.println("Extracting file: " + entry.getName());
                        int index = entry.getName().lastIndexOf('/');
                        if (index == -1)
                        {
                            // Was it created on Windows instead?
                            index = entry.getName().lastIndexOf('\\');
                        }
                        if (index > 0)
                        {
                            File dir = new File(ziptempdir + entry.getName().substring(0, index));
                            dir.mkdirs();
                        }
                        byte[] buffer = new byte[1024];
                        int len;
                        InputStream in = zf.getInputStream(entry);
                        BufferedOutputStream out = new BufferedOutputStream(
                            new FileOutputStream(ziptempdir + entry.getName()));
                        while((len = in.read(buffer)) >= 0)
                        {
                            out.write(buffer, 0, len);
                        }
                        in.close();
                        out.close();
                    }
                }
            }

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
            status = 1;
        }

        // Delete the unzipped file
        try
        {
            if (zip)
            {
                System.gc();
                System.out.println("Deleting temporary zip directory: " + ziptempdir);
                ItemImport.deleteDirectory(new File(ziptempdir));
            }
        }
        catch (Exception ex)
        {
            System.out.println("Unable to delete temporary zip archive location: " + ziptempdir);
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
            
            newItem = addItem(c, mycollections, sourceDir, newItemName, null, template);
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
            String itemname, PrintWriter mapOut, boolean template) throws Exception
    {
        String mapOutput = null;

        System.out.println("Adding item from directory " + itemname);

        // create workspace item
        Item myitem = null;
        WorkspaceItem wi = null;

        if (!isTest)
        {
            wi = WorkspaceItem.create(c, mycollections[0], template);
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

        if (useWorkflow)
        {
            // don't process handle file
            // start up a workflow
            if (!isTest)
            {

                // Should we send a workflow alert email or not?
                if (useWorkflowSendEmail)
                {
                    WorkflowManager.start(c, wi);
                }
                else
                {
                    WorkflowManager.startWithoutNotify(c, wi);
                }

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
	    String title = "TODO: normalized title"; //Browse.getNormalizedTitle(dc[i].value, dc[i].language);

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

    // Load all metadata schemas into the item.
    private void loadMetadata(Context c, Item myitem, String path)
            throws SQLException, IOException, ParserConfigurationException,
            SAXException, TransformerException, AuthorizeException
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
            throws SQLException, IOException, ParserConfigurationException,
            SAXException, TransformerException, AuthorizeException
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

    private void addDCValue(Context c, Item i, String schema, Node n) throws TransformerException, SQLException, AuthorizeException
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
        	
        	int schemaID = foundSchema.getSchemaID();
        	MetadataField foundField = MetadataField.findByElement(c, schemaID, element, qualifier);
        	
        	if (foundField == null)
        	{
        		System.out.println("ERROR: Metadata field: '"+schema+"."+element+"."+qualifier+"' was not found in the registry.");
        		return;
            }		
        }
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
                    System.err.println("Non-critical problem releasing resources.");
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
            String filename) throws SQLException, IOException,
            AuthorizeException
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
                    StringTokenizer tokenizer = new StringTokenizer(sRegistrationLine);
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
                            + "\tBundle: " + sBundle
                            + "\tDescription: " + sBundle);
                    continue;				// process next line in contents file
            	}

                int bitstreamEndIndex = line.indexOf("\t");

                if (bitstreamEndIndex == -1)
                {
                    // no extra info
                    processContentFileEntry(c, i, path, line, null, false);
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

                    // is this the primary bitstream?
                    String primaryBitstreamMarker = "\tprimary:true";
                    boolean primary = false;
                    String primaryStr = "";
                    if (line.contains(primaryBitstreamMarker))
                    {
                        primary = true;
                        primaryStr = "\t **Setting as primary bitstream**";
                    }

                    if (bundleExists)
                    {
                        String bundleName = line.substring(bMarkerIndex
                                + bundleMarker.length(), bEndIndex).trim();

                        processContentFileEntry(c, i, path, bitstreamName, bundleName, primary);
                        System.out.println("\tBitstream: " + bitstreamName +
                                           "\tBundle: " + bundleName +
                                           primaryStr);
                    }
                    else
                    {
                        processContentFileEntry(c, i, path, bitstreamName, null, primary);
                        System.out.println("\tBitstream: " + bitstreamName + primaryStr);
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
            String fileName, String bundleName, boolean primary) throws SQLException,
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

            // Is this a the primary bitstream?
            if (primary)
            {
                targetBundle.setPrimaryBitstreamID(bs.getID());
                targetBundle.update();
            }

            bs.update();
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
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    private void registerBitstream(Context c, Item i, int assetstore, 
            String bitstreamPath, String bundleName )
        	throws SQLException, IOException, AuthorizeException
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
            throws SQLException, AuthorizeException
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

                try
                {
                    myGroup = Group.findByName(c, groupName);
                }
                catch (SQLException sqle)
                {
                    System.out.println("SQL Exception finding group name: "
                            + groupName);
                    // do nothing, will check for null group later
                }
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
            throws SQLException, AuthorizeException
    {
        if (!isTest)
        {
            // remove the default policy
            AuthorizeManager.removeAllPolicies(c, bs);

            // add the policy
            ResourcePolicy rp = ResourcePolicy.create(c);

            rp.setResource(bs);
            rp.setAction(actionID);
            rp.setGroup(g);

            rp.update();
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
    private static Document loadXML(String filename) throws IOException,
            ParserConfigurationException, SAXException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();

        return builder.parse(new File(filename));
    }

    /**
     * Delete a directory and its child files and directories
     * @param path The directory to delete
     * @return Whether the deletion was successful or not
     */
    private static boolean deleteDirectory(File path)
    {
        if (path.exists())
        {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                if (files[i].isDirectory())
                {
                    deleteDirectory(files[i]);
                }
                else
                {
                    files[i].delete();
                }
            }
        }

        boolean pathDeleted = path.delete();
        return (pathDeleted);
    }
}
