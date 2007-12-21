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
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataSchema;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ExternalIdentifierMint;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
            ExternalIdentifier eid = ExternalIdentifierMint.parseCanonicalForm(c, myIDString);
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

        ObjectIdentifier oi = IdentifierUtils.fromString(c, myIDString);
*/

        if (oi == null)
        {
            System.err.println("Identifier " + myIDString + " not recognised.");
            System.exit(1);
        }

        if (myType == Constants.ITEM)
        {
            // first, do we have a persistent identifier for the item?
            myItem = (Item) oi.getObject(c);

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
            mycollection = (Collection) oi.getObject(c);

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
}
