/*
 * ItemExport.java
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
package org.dspace.app.itemexport;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ExternalIdentifierService;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.IdentifierService;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Item exporter to create simple AIPs for DSpace content. Currently exports
 * individual items, or entire collections. For instructions on use, see
 * printUsage() method.
 * <P>
 * ItemExport creates the simple AIP package that the importer also uses. It
 * consists of:
 * <P>
 * /exportdir/42/ (one directory per item) / dublin_core.xml - qualified dublin
 * core in RDF schema / contents - text file, listing one file per line / file1 -
 * files contained in the item / file2 / ...
 * <P>
 * issues -doesn't handle special characters in metadata (needs to turn &'s into
 * &amp;, etc.)
 * <P>
 * Modified by David Little, UCSD Libraries 12/21/04 to
 * allow the registration of files (bitstreams) into DSpace.
 */
public class ItemExport
{
    private static final int SUBDIR_LIMIT = 0;

    private static CollectionDAO collectionDAO;

    private static ItemDAO itemDAO;

    private static ExternalIdentifierDAO identifierDAO;

    /**
     * used for export download
     */
    public static final String COMPRESSED_EXPORT_MIME_TYPE = "application/zip";
    
    /*
     *  
     */
    public static void main(String[] argv) throws Exception
    {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("t", "type", true, "type: COLLECTION or ITEM");
        options.addOption("i", "id", true, "ID or URI (canonical form) of thing to export");
        options.addOption("d", "dest", true,
                "destination where you want items to go");
        options.addOption("n", "number", true,
                "sequence number to begin exporting items with");
        options.addOption("h", "help", false, "help");

        CommandLine line = parser.parse(options, argv);

        String typeString = null;
        String destDirName = null;
        String myIDString = null;
        int seqStart = -1;
        int myType = -1;

        Item myItem = null;
        Collection mycollection = null;

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ItemExport\n", options);
            System.out
                    .println("\nfull collection: ItemExport -t COLLECTION -i ID -d dest -n number");
            System.out
                    .println("singleitem:       ItemExport -t ITEM -i ID -d dest -n number");

            System.exit(0);
        }

        if (line.hasOption('t')) // type
        {
            typeString = line.getOptionValue('t');

            if (typeString.equals("ITEM"))
            {
                myType = Constants.ITEM;
            }
            else if (typeString.equals("COLLECTION"))
            {
                myType = Constants.COLLECTION;
            }
        }

        if (line.hasOption('i')) // id
        {
            myIDString = line.getOptionValue('i');
        }

        if (line.hasOption('d')) // dest
        {
            destDirName = line.getOptionValue('d');
        }

        if (line.hasOption('n')) // number
        {
            seqStart = Integer.parseInt(line.getOptionValue('n'));
        }

        // now validate the args
        if (myType == -1)
        {
            System.out
                    .println("type must be either COLLECTION or ITEM (-h for help)");
            System.exit(1);
        }

        if (destDirName == null)
        {
            System.out
                    .println("destination directory must be set (-h for help)");
            System.exit(1);
        }

        if (seqStart == -1)
        {
            System.out
                    .println("sequence start number must be set (-h for help)");
            System.exit(1);
        }

        if (myIDString == null)
        {
            System.out
                    .println("ID must be set to either a database ID or a canonical form of a URI (-h for help)");
            System.exit(1);
        }

        Context c = new Context();
        c.setIgnoreAuthorization(true);

        collectionDAO = CollectionDAOFactory.getInstance(c);
        itemDAO = ItemDAOFactory.getInstance(c);
        identifierDAO = ExternalIdentifierDAOFactory.getInstance(c);

        ObjectIdentifier oi = ObjectIdentifier.parseCanonicalForm(myIDString);
        if (oi == null)
        {
            ExternalIdentifier eid = ExternalIdentifierService.parseCanonicalForm(c, myIDString);
            oi = eid.getObjectIdentifier();
        }
        
        /*
        // First, add the namespace if necessary
        if (myIDString.indexOf('/') != -1)
        {
            if (myIDString.indexOf(':') == -1)
            {
                // has no : must be a handle
                myIDString = "hdl:" + myIDString;
                System.out.println("no namespace provided. assuming handles.");
            }
        }

        ObjectIdentifier oi = IdentifierUtils.parseCanonicalForm(c, myIDString);
*/

        if (oi == null)
        {
            System.err.println("Identifier " + myIDString + " not recognised.");
            System.exit(1);
        }

        if (myType == Constants.ITEM)
        {
            // first, do we have a persistent identifier for the item?
            // myItem = (Item) oi.getObject(c);
            myItem = (Item) IdentifierService.getResource(c, oi);

            if ((myItem == null) || (myItem.getType() != Constants.ITEM))
            {
                myItem = null;
            }

            if (myItem == null)
            {
                System.out
                        .println("Error, item cannot be found: " + myIDString);
            }
        }
        else
        {
            // mycollection = (Collection) oi.getObject(c);
            mycollection = (Collection) IdentifierService.getResource(c, oi);

            // ensure it's a collection
            if ((mycollection == null)
                    || (mycollection.getType() != Constants.COLLECTION))
            {
                mycollection = null;
            }

            if (mycollection == null)
            {
                System.out.println("Error, collection cannot be found: "
                        + myIDString);
                System.exit(1);
            }
        }

        if (myItem != null)
        {
            // it's only a single item
            exportItem(c, myItem, destDirName, seqStart);
        }
        else
        {
            System.out.println("Exporting from collection: " + myIDString);

            // it's a collection, so do a bunch of items
            ItemIterator i = mycollection.getItems();

            exportItem(c, i, destDirName, seqStart);
        }

        c.complete();
    }

    private static void exportItem(Context c, ItemIterator i,
            String destDirName, int seqStart) throws Exception
    {
        int mySequenceNumber = seqStart;
        int counter = SUBDIR_LIMIT - 1;
        int subDirSuffix = 0;
        String fullPath = destDirName;
        String subdir = "";
        File dir;

        if (SUBDIR_LIMIT > 0)
        {
            dir = new File(destDirName);
            if (!dir.isDirectory())
            {
                throw new IOException(destDirName + " is not a directory.");
            }
        }

        System.out.println("Beginning export");

        while (i.hasNext())
        {
            if (SUBDIR_LIMIT > 0 && ++counter == SUBDIR_LIMIT)
            {
                subdir = Integer.toString(subDirSuffix++);
                fullPath = destDirName + dir.separatorChar + subdir;
                counter = 0;

                if (!new File(fullPath).mkdirs())
                {
                    throw new IOException("Error, can't make dir " + fullPath);
                }
            }

            System.out.println("Exporting item to " + mySequenceNumber);
            exportItem(c, i.next(), fullPath, mySequenceNumber);
            mySequenceNumber++;
        }
    }

    private static void exportItem(Context c, Item myItem, String destDirName,
            int seqStart) throws Exception
    {
        File destDir = new File(destDirName);

        if (destDir.exists())
        {
            // now create a subdirectory
            File itemDir = new File(destDir + "/" + seqStart);

            System.out.println("Exporting Item " + myItem.getID() + " to "
                    + itemDir);

            if (itemDir.exists())
            {
                throw new Exception("Directory " + destDir + "/" + seqStart
                        + " already exists!");
            }

            if (itemDir.mkdir())
            {
                // make it this far, now start exporting
                writeMetadata(c, myItem, itemDir);
                writeBitstreams(c, myItem, itemDir);
                writeURI(c, myItem, itemDir);
            }
            else
            {
                throw new Exception("Error, can't make dir " + itemDir);
            }
        }
        else
        {
            throw new Exception("Error, directory " + destDirName
                    + " doesn't exist!");
        }
    }

    /**
     * Discover the different schemas in use and output a seperate metadata
     * XML file for each schema.
     *
     * @param c
     * @param i
     * @param destDir
     * @throws Exception
     */
    private static void writeMetadata(Context c, Item i, File destDir)
            throws Exception
    {
        // Build a list of schemas for the item
        HashMap map = new HashMap();
        DCValue[] dcorevalues = i.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (int ii = 0; ii < dcorevalues.length; ii++)
        {
            map.put(dcorevalues[ii].schema, null);
        }

        // Save each of the schemas into it's own metadata file
        Iterator iterator = map.keySet().iterator();
        while (iterator.hasNext())
        {
            String schema = (String) iterator.next();
            writeMetadata(c, schema, i, destDir);
        }
    }

    // output the item's dublin core into the item directory
    private static void writeMetadata(Context c, String schema, Item i, File destDir)
            throws Exception
    {
        String filename;
        if (schema.equals(MetadataSchema.DC_SCHEMA)) {
            filename = "dublin_core.xml";
        } else {
            filename = "metadata_" + schema + ".xml";
        }
        
        File outFile = new File(destDir, filename);

        System.out.println("Attempting to create file " + outFile);

        if (outFile.createNewFile())
        {
            BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(outFile));

            DCValue[] dcorevalues = i.getMetadata(schema, Item.ANY, Item.ANY, Item.ANY);

            // XML preamble
            byte[] utf8 = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n"
                    .getBytes("UTF-8");
            out.write(utf8, 0, utf8.length);

            String dcTag = "<dublin_core schema=\""+schema+"\">\n";
            utf8 = dcTag.getBytes("UTF-8");
            out.write(utf8, 0, utf8.length);

            for (int j = 0; j < dcorevalues.length; j++)
            {
                DCValue dcv = dcorevalues[j];
                String qualifier = dcv.qualifier;

                if (qualifier == null)
                {
                    qualifier = "none";
                }

                utf8 = ("  <dcvalue element=\"" + dcv.element + "\" "
                        + "qualifier=\"" + qualifier + "\">"
                        + Utils.addEntities(dcv.value) + "</dcvalue>\n").getBytes("UTF-8");

                out.write(utf8, 0, utf8.length);
            }

            utf8 = "</dublin_core>\n".getBytes("UTF-8");
            out.write(utf8, 0, utf8.length);

            out.close();
        }
        else
        {
            throw new Exception("Cannot create dublin_core.xml in " + destDir);
        }
    }

    // create the file 'uri' which contains (one of the) the URI(s) assigned
    // to the item
    private static void writeURI(Context c, Item i, File destDir)
            throws Exception
    {
        String filename = "uri";

        File outFile = new File(destDir, filename);

        if (outFile.createNewFile())
        {
            PrintWriter out = new PrintWriter(new FileWriter(outFile));

            // first do the internal UUID
            out.println(i.getIdentifier().getCanonicalForm());

            // next do the external identifiers
            List<ExternalIdentifier> eids = i.getExternalIdentifiers();
            for (ExternalIdentifier eid : eids)
            {
                out.println(eid.getCanonicalForm());
            }

            // close the contents file
            out.close();
        }
        else
        {
            throw new Exception("Cannot create file " + filename + " in "
                    + destDir);
        }
    }

    /**
     * Create both the bitstreams and the contents file. Any bitstreams that
     * were originally registered will be marked in the contents file as such.
     * However, the export directory will contain actual copies of the content
     * files being exported.
     * 
     * @param c the DSpace context
     * @param i the item being exported
     * @param destDir the item's export directory
     * @throws Exception if there is any problem writing to the export 
     * 		directory
     */
    private static void writeBitstreams(Context c, Item i, File destDir)
            throws Exception
    {
        File outFile = new File(destDir, "contents");

        if (outFile.createNewFile())
        {
            PrintWriter out = new PrintWriter(new FileWriter(outFile));

            Bundle[] bundles = i.getBundles();

            for (int j = 0; j < bundles.length; j++)
            {
                // bundles can have multiple bitstreams now...
                Bitstream[] bitstreams = bundles[j].getBitstreams();

                String bundleName = bundles[j].getName();

                for (int k = 0; k < bitstreams.length; k++)
                {
                    Bitstream b = bitstreams[k];

                    String myName = b.getName();
                    String oldName = myName;
                    int myPrefix = 1; // only used with name conflict

                    InputStream is = b.retrieve();

                    boolean isDone = false; // done when bitstream is finally
                                            // written

                    while (!isDone)
                    {
                        File fout = new File(destDir, myName);

                        if (fout.createNewFile())
                        {
                            FileOutputStream fos = new FileOutputStream(fout);
                            Utils.bufferedCopy(is, fos);
                            // close streams
                            is.close();
                            fos.close();

                            // write the manifest file entry
                            if (b.isRegisteredBitstream()) {
                                out.println("-r -s " + b.getStoreNumber() 
                                		+ " -f " + myName 
										+ "\tbundle:" + bundleName);
                            } else {
                                out.println(myName + "\tbundle:" + bundleName);
                            }

                            isDone = true;
                        }
                        else
                        {
                            myName = myPrefix + "_" + oldName; // keep appending
                                                               // numbers to the
                                                               // filename until
                                                               // unique
                            myPrefix++;
                        }
                    }
                }
            }

            // close the contents file
            out.close();
        }
        else
        {
            throw new Exception("Cannot create contents in " + destDir);
        }
    }

    /**
     * Convenience methot to create export a single Community, Collection, or
     * Item
     * 
     * @param dso -
     *            the dspace object to export
     * @param context -
     *            the dspace context
     * @throws Exception
     */
    public static void createDownloadableExport(DSpaceObject dso,
    		Context context) throws Exception {
    	EPerson eperson = context.getCurrentUser();
    	ArrayList<DSpaceObject> list = new ArrayList<DSpaceObject>(1);
    	list.add(dso);
    	processDownloadableExport(list, context, eperson == null ? null
    			: eperson.getEmail());
    }

    /**
     * Convenience method to export a List of dspace objects (Community,
     * Collection or Item)
     * 
     * @param dsObjects -
     *            List containing dspace objects
     * @param context -
     *            the dspace context
     * @throws Exception
     */
    public static void createDownloadableExport(List<DSpaceObject> dsObjects,
    		Context context) throws Exception {
    	EPerson eperson = context.getCurrentUser();
    	processDownloadableExport(dsObjects, context, eperson == null ? null
    			: eperson.getEmail());
    }

    /**
     * Convenience methot to create export a single Community, Collection, or
     * Item
     * 
     * @param dso -
     *            the dspace object to export
     * @param context -
     *            the dspace context
     * @param additionalEmail -
     *            cc email to use
     * @throws Exception
     */
    public static void createDownloadableExport(DSpaceObject dso,
    		Context context, String additionalEmail) throws Exception {
    	ArrayList<DSpaceObject> list = new ArrayList<DSpaceObject>(1);
    	list.add(dso);
    	processDownloadableExport(list, context, additionalEmail);
    }

    /**
     * Convenience method to export a List of dspace objects (Community,
     * Collection or Item)
     * 
     * @param dsObjects -
     *            List containing dspace objects
     * @param context -
     *            the dspace context
     * @param additionalEmail -
     *            cc email to use
     * @throws Exception
     */
    public static void createDownloadableExport(List<DSpaceObject> dsObjects,
    		Context context, String additionalEmail) throws Exception {
    	processDownloadableExport(dsObjects, context, additionalEmail);
    }

    /**
     * Does the work creating a List with all the Items in the Community or
     * Collection It then kicks off a new Thread to export the items, zip the
     * export directory and send confirmation email
     * 
     * @param dsObjects -
     *            List of dspace objects to process
     * @param context -
     *            the dspace context
     * @param additionalEmail -
     *            email address to cc in addition the the current user email
     * @throws Exception
     */
    private static void processDownloadableExport(List<DSpaceObject> dsObjects,
    		Context context, final String additionalEmail) throws Exception {
    	final EPerson eperson = context.getCurrentUser();

    	// before we create a new export archive lets delete the 'expired'
    	// archives
    	deleteOldExportArchives(eperson.getID());

    	// keep track of the commulative size of all bitstreams in each of the
    	// items
    	// it will be checked against the config file entry
    	float size = 0;
    	final ArrayList<Integer> items = new ArrayList<Integer>();
    	for (DSpaceObject dso : dsObjects) {
    		if (dso.getType() == Constants.COMMUNITY) {
    			Community community = (Community) dso;
    			// get all the collections in the community
    			Collection[] collections = community.getCollections();
    			for (Collection collection : collections) {
    				// get all the items in each collection
    				ItemIterator iitems = collection.getItems();
    				while (iitems.hasNext()) {
    					Item item = iitems.next();
    					// get all the bundles in the item
    					Bundle[] bundles = item.getBundles();
    					for (Bundle bundle : bundles) {
    						// get all the bitstreams in each bundle
    						Bitstream[] bitstreams = bundle.getBitstreams();
    						for (Bitstream bit : bitstreams) {
    							// add up the size
    							size += bit.getSize();
    						}
    					}
    					items.add(item.getID());
    				}
    			}
    		} else if (dso.getType() == Constants.COLLECTION) {
    			Collection collection = (Collection) dso;
    			// get all the items in the collection
    			ItemIterator iitems = collection.getItems();
    			while (iitems.hasNext()) {
    				Item item = iitems.next();
    				// get all thebundles in the item
    				Bundle[] bundles = item.getBundles();
    				for (Bundle bundle : bundles) {
    					// get all the bitstreams in the bundle
    					Bitstream[] bitstreams = bundle.getBitstreams();
    					for (Bitstream bit : bitstreams) {
    						// add up the size
    						size += bit.getSize();
    					}
    				}
    				items.add(item.getID());
    			}
    		} else if (dso.getType() == Constants.ITEM) {
    			Item item = (Item) dso;
    			// get all the bundles in the item
    			Bundle[] bundles = item.getBundles();
    			for (Bundle bundle : bundles) {
    				// get all the bitstreams in the bundle
    				Bitstream[] bitstreams = bundle.getBitstreams();
    				for (Bitstream bit : bitstreams) {
    					// add up the size
    					size += bit.getSize();
    				}
    			}
    			items.add(item.getID());
    		} else {
    			// nothing to do just ignore this type of DSPaceObject
    		}
    	}

    	// check the size of all the bitstreams against the configuration file
    	// entry if it exists
    	String megaBytes = ConfigurationManager
    	.getProperty("org.dspace.app.itemexport.max.size");
    	if (megaBytes != null) {
    		float maxSize = 0;
    		try {
    			maxSize = Float.parseFloat(megaBytes);
    		} catch (Exception e) {
    			// ignore...configuration entry may not be present
    		}

    		if (maxSize > 0) {
    			if (maxSize < (size / 1048576.00)) { // a megabyte
    				throw new Exception(
    				"The overall size of this export is too large.  Please contact your administrator for more information.");
    			}
    		}
    	}

    	// if we have any items to process then kick off annonymous thread
    	if (items.size() > 0) {
    		Thread go = new Thread() {
    			public void run() {
    				Context context;
    				try {
    					// create a new dspace context
    					context = new Context();
    					// ignore auths
    					context.setIgnoreAuthorization(true);
    					ItemIterator iitems = new ItemIterator(context, items);
    					String fileName = assembleFileName("item",eperson, new Date());
    					String workDir = getExportWorkDirectory()
    					+ System.getProperty("file.separator")
    					+ fileName;
    					String downloadDir = getExportDownloadDirectory(eperson
    							.getID());

    					File wkDir = new File(workDir);
    					if (!wkDir.exists()) {
    						wkDir.mkdirs();
    					}

    					File dnDir = new File(downloadDir);
    					if (!dnDir.exists()) {
    						dnDir.mkdirs();
    					}

    					// export the items using normal export method
    					exportItem(context, iitems, workDir, 1);
    					// now zip up the export directory created above
    					zip(workDir, downloadDir
    							+ System.getProperty("file.separator")
    							+ fileName + ".zip");
    					// email message letting user know the file is ready for
    					// download
    					emailSuccessMessage(eperson.getEmail(),
    							ConfigurationManager
    							.getProperty("mail.from.address"),
    							additionalEmail, fileName + ".zip");
    					// return to enforcing auths
    					context.setIgnoreAuthorization(false);
    				} catch (Exception e1) {
    					try {
    						emailErrorMessage(eperson.getEmail(),
    								ConfigurationManager
    								.getProperty("mail.from.address"),
    								additionalEmail, e1.getMessage());
    					} catch (Exception e) {
    						// wont throw here
    					}
    					throw new RuntimeException(e1);
    				}
    			}

    		};

    		go.isDaemon();
    		go.start();
    	}
    }

    /**
     * Create a file name based on the date and eperson
     * 
     * @param eperson -
     *            eperson who requested export and will be able to download it
     * @param date -
     *            the date the export process was created
     * @return String representing the file name in the form of
     *         'export_yyy_MMM_dd_count_epersonID'
     * @throws Exception
     */
    public static String assembleFileName(String type, EPerson eperson, Date date)
	throws Exception {
		// to format the date
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MMM_dd");
		String downloadDir = getExportDownloadDirectory(eperson.getID());
		// used to avoid name collision
		int count = 1;
		boolean exists = true;
		String fileName = null;
		while (exists) {
			fileName = type + "_export_" + sdf.format(date) + "_" + count + "_"
					+ eperson.getID();
			exists = new File(downloadDir
					+ System.getProperty("file.separator") + fileName + ".zip")
					.exists();
			count++;
		}
		return fileName;
	}

    /**
     * Use config file entry for org.dspace.app.itemexport.download.dir and id
     * of the eperson to create a download directory name
     * 
     * @param ePersonID -
     *            id of the eperson who requested export archive
     * @return String representing a directory in the form of
     *         org.dspace.app.itemexport.download.dir/epersonID
     * @throws Exception
     */
    public static String getExportDownloadDirectory(int ePersonID)
	throws Exception {
		String downloadDir = ConfigurationManager
				.getProperty("org.dspace.app.itemexport.download.dir");
		if (downloadDir == null) {
			throw new Exception(
					"A dspace.cfg entry for 'org.dspace.app.itemexport.download.dir' does not exist.");
		}
		
		return downloadDir + System.getProperty("file.separator") + ePersonID;
		
	}

    /**
     * Returns config file entry for org.dspace.app.itemexport.work.dir
     * 
     * @return String representing config file entry for
     *         org.dspace.app.itemexport.work.dir
     * @throws Exception
     */
    public static String getExportWorkDirectory() throws Exception {
    	String exportDir = ConfigurationManager
    	.getProperty("org.dspace.app.itemexport.work.dir");
    	if (exportDir == null) {
    		throw new Exception(
    				"A dspace.cfg entry for 'org.dspace.app.itemexport.work.dir' does not exist.");
    	}
    	return exportDir;
    }

    /**
     * Used to read the export archived. Inteded for download.
     * 
     * @param fileName
     *            the name of the file to download
     * @param eperson
     *            the eperson requesting the download
     * @return an input stream of the file to be downloaded
     * @throws Exception
     */
    public static InputStream getExportDownloadInputStream(String fileName,
    		EPerson eperson) throws Exception {
    	File file = new File(getExportDownloadDirectory(eperson.getID())
    			+ System.getProperty("file.separator") + fileName);
    	if (file.exists()) {
    		return new FileInputStream(file);
    	} else
    		return null;
    }

    /**
     * Get the file size of the export archive represented by the file name
     * 
     * @param fileName
     *            name of the file to get the size
     * @param eperson
     *            the eperson requesting file
     * @return
     * @throws Exception
     */
    public static long getExportFileSize(String fileName) throws Exception {
    	String strID = fileName.substring(fileName.lastIndexOf('_') + 1,
    			fileName.lastIndexOf('.'));
    	File file = new File(
    			getExportDownloadDirectory(Integer.parseInt(strID))
    			+ System.getProperty("file.separator") + fileName);
    	if (!file.exists() || !file.isFile()) {
    		throw new FileNotFoundException("The file " + getExportDownloadDirectory(Integer.parseInt(strID))
    				+ System.getProperty("file.separator") + fileName
    				+ " does not exist.");
    	}

    	return file.length();
    }

    public static long getExportFileLastModified(String fileName) throws Exception {
    	String strID = fileName.substring(fileName.lastIndexOf('_') + 1,
    			fileName.lastIndexOf('.'));
    	File file = new File(
    			getExportDownloadDirectory(Integer.parseInt(strID))
    			+ System.getProperty("file.separator") + fileName);
    	if (!file.exists() || !file.isFile()) {
    		throw new FileNotFoundException("The file " + getExportDownloadDirectory(Integer.parseInt(strID))
    				+ System.getProperty("file.separator") + fileName
    				+ " does not exist.");
    	}

    	return file.lastModified();
    }

    /**
     * The file name of the export archive contains the eperson id of the person
     * who created it When requested for download this method can check if the
     * person requesting it is the same one that created it
     * 
     * @param context
     *            dspace context
     * @param fileName
     *            the file name to check auths for
     * @return true if it is the same person false otherwise
     */
    public static boolean canDownload(Context context, String fileName) {
    	EPerson eperson = context.getCurrentUser();
    	if (eperson == null) {
    		return false;
    	}
    	String strID = fileName.substring(fileName.lastIndexOf('_') + 1,
    			fileName.lastIndexOf('.'));
    	try {
    		if (Integer.parseInt(strID) == eperson.getID()) {
    			return true;
    		}
    	} catch (Exception e) {
    		return false;
    	}
    	return false;
    }

    /**
     * Reads the download directory for the eperson to see if any export
     * archives are available
     * 
     * @param eperson
     * @return a list of file names representing export archives that have been
     *         processed
     * @throws Exception
     */
    public static List<String> getExportsAvailable(EPerson eperson)
    throws Exception {
    	File downloadDir = new File(getExportDownloadDirectory(eperson.getID()));
    	if (!downloadDir.exists() || !downloadDir.isDirectory()) {
    		return null;
    	}

    	List<String> fileNames = new ArrayList<String>();

    	for (String fileName : downloadDir.list()) {
    		if (fileName.contains("export") && fileName.endsWith(".zip")) {
    			fileNames.add(fileName);
    		}
    	}

    	if (fileNames.size() > 0) {
    		return fileNames;
    	}

    	return null;
    }

    /**
     * A clean up method that is ran before a new export archive is created. It
     * uses the config file entry 'org.dspace.app.itemexport.life.span.hours' to
     * determine if the current exports are too old and need pruging
     * 
     * @param epersonID -
     *            the id of the eperson to clean up
     * @throws Exception
     */
    public static void deleteOldExportArchives(int epersonID) throws Exception {
    	int hours = ConfigurationManager
    	.getIntProperty("org.dspace.app.itemexport.life.span.hours");
    	Calendar now = Calendar.getInstance();
    	now.setTime(new Date());
    	now.add(Calendar.HOUR, (-hours));
    	File downloadDir = new File(getExportDownloadDirectory(epersonID));
    	if (downloadDir.exists()) {
    		File[] files = downloadDir.listFiles();
    		for (File file : files) {
    			if (file.lastModified() < now.getTimeInMillis()) {
    				file.delete();
    			}
    		}
    	}

    }

    /**
     * Since the archive is created in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplis this
     * communication with email instead. Send a success email once the export
     * archive is complete and ready for download
     * 
     * @param toMail -
     *            email to send message to
     * @param fromMail -
     *            email for the from field
     * @param ccMail -
     *            carbon copy email
     * @param fileName -
     *            the file name to be downloaded. It is added to the url in the
     *            email
     * @throws MessagingException
     */
    public static void emailSuccessMessage(String toMail, String fromMail,
    		String ccMail, String fileName) throws MessagingException {
    	StringBuffer content = new StringBuffer();
    	content
    	.append("The item export you requested from the repositry is now ready for download.");
    	content.append(System.getProperty("line.separator"));
    	content.append(System.getProperty("line.separator"));
    	content
    	.append("You may download the compressed file using the following web address:");
    	content.append(System.getProperty("line.separator"));
    	content.append(ConfigurationManager.getProperty("dspace.url"));
    	content.append("/exportdownload/");
    	content.append(fileName);
    	content.append(System.getProperty("line.separator"));
    	content.append(System.getProperty("line.separator"));
    	content.append("Tis file will remain available for at least ");
    	content.append(ConfigurationManager
    			.getProperty("org.dspace.app.itemexport.life.span.hours"));
    	content.append(" hours.");
    	content.append(System.getProperty("line.separator"));
    	content.append(System.getProperty("line.separator"));
    	content.append("Thank you");

    	sendMessage(toMail, fromMail, ccMail,
    			"Item export requested is ready for download", content);
    }

    /**
     * Since the archive is created in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplis this
     * communication with email instead. Send an error email if the export
     * archive fails
     * 
     * @param toMail -
     *            email to send message to
     * @param fromMail -
     *            email for the from field
     * @param ccMail -
     *            carbon copy email
     * @param fileName -
     *            the file name to be downloaded. It is added to the url in the
     *            email
     * @throws MessagingException
     */
    public static void emailErrorMessage(String toMail, String fromMail,
    		String ccMail, String error) throws MessagingException {
    	StringBuffer content = new StringBuffer();
    	content.append("The item export you requested was not completed.");
    	content.append(System.getProperty("line.separator"));
    	content.append(System.getProperty("line.separator"));
    	content
    	.append("For more infrmation you may contact your system administrator.");
    	content.append(System.getProperty("line.separator"));
    	content.append(System.getProperty("line.separator"));
    	content.append("Error message received: ");
    	content.append(error);
    	content.append(System.getProperty("line.separator"));
    	content.append(System.getProperty("line.separator"));
    	content.append("Thank you");

    	sendMessage(toMail, fromMail, ccMail,
    			"Item export requested was not completed", content);
    }

    private static void sendMessage(String toMail, String fromMail,
    		String ccMail, String subject, StringBuffer content)
    throws MessagingException {
    	try {
    		if (toMail == null || !toMail.contains("@")) {
    			return;
    		}

    		// Get the mail configuration properties
    		String server = ConfigurationManager.getProperty("mail.server");

    		// Set up properties for mail session
    		Properties props = System.getProperties();
    		props.put("mail.smtp.host", server);

    		// Get session
    		Session session = Session.getDefaultInstance(props, null);

    		MimeMessage msg = new MimeMessage(session);
    		Multipart multipart = new MimeMultipart();

    		// create the first part of the email
    		BodyPart messageBodyPart = new MimeBodyPart();
    		messageBodyPart.setText(content.toString());

    		multipart.addBodyPart(messageBodyPart);
    		msg.setContent(multipart);
    		msg.setFrom(new InternetAddress(fromMail));
    		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
    				toMail));
    		if (ccMail != null && ccMail.contains("@")) {
    			msg.addRecipient(Message.RecipientType.CC, new InternetAddress(
    					ccMail));
    		}
    		msg.setSentDate(new Date());
    		msg.setSubject(subject);
    		Transport.send(msg);
    	} catch (MessagingException e) {
    		e.printStackTrace();
    		throw e;
    	}
    }

    public static void zip(String strSource, String target) throws Exception {
    	ZipOutputStream cpZipOutputStream = null;
    	String tempFileName = target + "_tmp";
    	try {
    		File cpFile = new File(strSource);
    		if (!cpFile.isFile() && !cpFile.isDirectory()) {
    			return;
    		}
    		File targetFile = new File(tempFileName);
    		if (!targetFile.exists()) {
    			targetFile.createNewFile();
    		}
    		FileOutputStream fos = new FileOutputStream(tempFileName);
    		cpZipOutputStream = new ZipOutputStream(fos);
    		cpZipOutputStream.setLevel(9);
    		zipFiles(cpFile, strSource, tempFileName, cpZipOutputStream);
    		cpZipOutputStream.finish();
    		cpZipOutputStream.close();
    		deleteDirectory(cpFile);
    		targetFile.renameTo(new File(target));
    	} catch (Exception e) {
    		throw e;
    	}
    }

    private static void zipFiles(File cpFile, String strSource,
    		String strTarget, ZipOutputStream cpZipOutputStream)
    throws Exception {
    	int byteCount;
    	final int DATA_BLOCK_SIZE = 2048;
    	FileInputStream cpFileInputStream;
    	if (cpFile.isDirectory()) {
    		File[] fList = cpFile.listFiles();
    		for (int i = 0; i < fList.length; i++) {
    			zipFiles(fList[i], strSource, strTarget, cpZipOutputStream);
    		}
    	} else {
    		try {
    			if (cpFile.getAbsolutePath().equalsIgnoreCase(strTarget)) {
    				return;
    			}
    			String strAbsPath = cpFile.getPath();
    			String strZipEntryName = strAbsPath.substring(strSource
    					.length() + 1, strAbsPath.length());

    			// byte[] b = new byte[ (int)(cpFile.length()) ];

    			cpFileInputStream = new FileInputStream(cpFile);
    			ZipEntry cpZipEntry = new ZipEntry(strZipEntryName);
    			cpZipOutputStream.putNextEntry(cpZipEntry);

    			byte[] b = new byte[DATA_BLOCK_SIZE];
    			while ((byteCount = cpFileInputStream.read(b, 0,
    					DATA_BLOCK_SIZE)) != -1) {
    				cpZipOutputStream.write(b, 0, byteCount);
    			}

    			// cpZipOutputStream.write(b, 0, (int)cpFile.length());
    			cpZipOutputStream.closeEntry();
    		} catch (Exception e) {
    			throw e;
    		}
    	}
    }

    private static boolean deleteDirectory(File path) {
    	if (path.exists()) {
    		File[] files = path.listFiles();
    		for (int i = 0; i < files.length; i++) {
    			if (files[i].isDirectory()) {
    				deleteDirectory(files[i]);
    			} else {
    				files[i].delete();
    			}
    		}
    	}

    	boolean pathDeleted = path.delete();
    	return (pathDeleted);
    }
}
