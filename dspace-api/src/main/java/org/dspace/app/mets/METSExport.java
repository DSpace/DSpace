/*
 * METSExport.java
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
package org.dspace.app.mets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;
import org.dspace.app.util.Util;

import edu.harvard.hul.ois.mets.Agent;
import edu.harvard.hul.ois.mets.AmdSec;
import edu.harvard.hul.ois.mets.BinData;
import edu.harvard.hul.ois.mets.Checksumtype;
import edu.harvard.hul.ois.mets.Div;
import edu.harvard.hul.ois.mets.DmdSec;
import edu.harvard.hul.ois.mets.FLocat;
import edu.harvard.hul.ois.mets.FileGrp;
import edu.harvard.hul.ois.mets.FileSec;
import edu.harvard.hul.ois.mets.Loctype;
import edu.harvard.hul.ois.mets.MdWrap;
import edu.harvard.hul.ois.mets.Mdtype;
import edu.harvard.hul.ois.mets.Mets;
import edu.harvard.hul.ois.mets.MetsHdr;
import edu.harvard.hul.ois.mets.Name;
import edu.harvard.hul.ois.mets.RightsMD;
import edu.harvard.hul.ois.mets.Role;
import edu.harvard.hul.ois.mets.StructMap;
import edu.harvard.hul.ois.mets.Type;
import edu.harvard.hul.ois.mets.XmlData;
import edu.harvard.hul.ois.mets.helper.Base64;
import edu.harvard.hul.ois.mets.helper.MetsException;
import edu.harvard.hul.ois.mets.helper.MetsValidator;
import edu.harvard.hul.ois.mets.helper.MetsWriter;
import edu.harvard.hul.ois.mets.helper.PCData;
import edu.harvard.hul.ois.mets.helper.PreformedXML;

/**
 * Tool for exporting DSpace AIPs with the metadata serialised in METS format
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class METSExport
{
    private static int licenseFormat = -1;

    private static Properties dcToMODS;

    public static void main(String[] args) throws Exception
    {
        Context context = new Context();

        init(context);

        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("c", "collection", true,
                "Handle of collection to export");
        options.addOption("i", "item", true, "Handle of item to export");
        options.addOption("a", "all", false, "Export all items in the archive");
        options.addOption("d", "destination", true, "Destination directory");
        options.addOption("h", "help", false, "Help");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("metsexport", options);
            System.out
                    .println("\nExport a collection:  metsexport -c hdl:123.456/789");
            System.out
                    .println("Export an item:       metsexport -i hdl:123.456/890");
            System.out.println("Export everything:    metsexport -a");

            System.exit(0);
        }

        String dest = "";

        if (line.hasOption('d'))
        {
            dest = line.getOptionValue('d');

            // Make sure it ends with a file separator
            if (!dest.endsWith(File.separator))
            {
                dest = dest + File.separator;
            }
        }

        if (line.hasOption('i'))
        {
            String handle = getHandleArg(line.getOptionValue('i'));

            // Exporting a single item
            DSpaceObject o = HandleManager.resolveToObject(context, handle);

            if ((o != null) && o instanceof Item)
            {
                writeAIP(context, (Item) o, dest);
                System.exit(0);
            }
            else
            {
                System.err.println(line.getOptionValue('i')
                        + " is not a valid item Handle");
                System.exit(1);
            }
        }

        ItemIterator items = null;

        if (line.hasOption('c'))
        {
            String handle = getHandleArg(line.getOptionValue('c'));

            // Exporting a collection's worth of items
            DSpaceObject o = HandleManager.resolveToObject(context, handle);

            if ((o != null) && o instanceof Collection)
            {
                items = ((Collection) o).getItems();
            }
            else
            {
                System.err.println(line.getOptionValue('c')
                        + " is not a valid collection Handle");
                System.exit(1);
            }
        }

        if (line.hasOption('a'))
        {
            items = Item.findAll(context);
        }

        if (items == null)
        {
            System.err.println("Nothing to export specified!");
            System.exit(1);
        }

        while (items.hasNext())
        {
            writeAIP(context, items.next(), dest);
        }

        context.abort();
        System.exit(0);
    }

    /**
     * Initialise various variables, read in config etc.
     * 
     * @param context
     *            DSpace context
     */
    private static void init(Context context) throws SQLException, IOException
    {
        // Don't init again if initialised already
        if (licenseFormat != -1)
        {
            return;
        }

        // Find the License format
        BitstreamFormat bf = BitstreamFormat.findByShortDescription(context,
                "License");
        licenseFormat = bf.getID();

        // get path to DC->MODS map info file
        String configFile = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator + "dc2mods.cfg";

        // Read it in
        InputStream is = new FileInputStream(configFile);
        dcToMODS = new Properties();
        dcToMODS.load(is);
    }

    /**
     * Write out the AIP for the given item to the given directory. A new
     * directory will be created with the Handle (URL-encoded) as the directory
     * name, and inside, a mets.xml file written, together with the bitstreams.
     * 
     * @param context
     *            DSpace context to use
     * @param item
     *            Item to write
     * @param dest
     *            destination directory
     */
    public static void writeAIP(Context context, Item item, String dest)
            throws SQLException, IOException, AuthorizeException, MetsException
    {
        System.out.println("Exporting item hdl:" + item.getHandle());

        // Create aip directory
        java.io.File aipDir = new java.io.File(dest
                + URLEncoder.encode("hdl:" + item.getHandle(), "UTF-8"));

        if (!aipDir.mkdir())
        {
            // Couldn't make the directory for some reason
            throw new IOException("Couldn't create " + aipDir.toString());
        }

        // Write the METS file
        FileOutputStream out = new FileOutputStream(aipDir.toString()
                + java.io.File.separator + "mets.xml");
        writeMETS(context, item, out, false);
        out.close();

        // Write bitstreams
        Bundle[] bundles = item.getBundles();

        for (int i = 0; i < bundles.length; i++)
        {
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            for (int b = 0; b < bitstreams.length; b++)
            {
                // Skip license bitstream and unauthorized resources
                if ((bitstreams[b].getFormat().getID() != licenseFormat)
                        && AuthorizeManager.authorizeActionBoolean(context,
                                bitstreams[b], Constants.READ))
                {
                    out = new FileOutputStream(aipDir.toString()
                            + java.io.File.separator
                            + bitstreams[b].getName());

                    InputStream in = bitstreams[b].retrieve();
                    Utils.bufferedCopy(in, out);
                    out.close();
                    in.close();
                }
            }
        }
    }

    /**
     * Write METS metadata corresponding to the metadata for an item
     * 
     * @param context
     *            DSpace context
     * @param item
     *            DSpace item to create METS object for
     * @param os
     *            A stream to write METS package to (UTF-8 encoding will be used)
     * @param fullURL
     *            if <code>true</code>, the &lt;FLocat&gt; values for each
     *            bitstream will be the full URL for that bitstream. Otherwise,
     *            only the filename itself will be used.
     */
    public static void writeMETS(Context context, Item item, OutputStream os, boolean fullURL)
            throws SQLException, IOException, AuthorizeException
    {
        try
        {
            init(context);

            // Create the METS file
            Mets mets = new Mets();

            // Top-level stuff
            mets.setOBJID("hdl:" + item.getHandle());
            mets.setLABEL("DSpace Item");
            mets.setSchema("mods", "http://www.loc.gov/mods/v3",
                    "http://www.loc.gov/standards/mods/v3/mods-3-0.xsd");

            // MetsHdr
            MetsHdr metsHdr = new MetsHdr();
            metsHdr.setCREATEDATE(new Date()); // FIXME: CREATEDATE is now:
                                               // maybe should be item create
                                               // date?

            // Agent
            Agent agent = new Agent();
            agent.setROLE(Role.CUSTODIAN);
            agent.setTYPE(Type.ORGANIZATION);

            Name name = new Name();
            name.getContent()
                    .add(
                            new PCData(ConfigurationManager
                                    .getProperty("dspace.name")));
            agent.getContent().add(name);

            metsHdr.getContent().add(agent);

            mets.getContent().add(metsHdr);

            DmdSec dmdSec = new DmdSec();
            dmdSec.setID("DMD_hdl_" + item.getHandle());

            MdWrap mdWrap = new MdWrap();
            mdWrap.setMDTYPE(Mdtype.MODS);

            XmlData xmlData = new XmlData();
            createMODS(item, xmlData);

            mdWrap.getContent().add(xmlData);
            dmdSec.getContent().add(mdWrap);
            mets.getContent().add(dmdSec);

            // amdSec
            AmdSec amdSec = new AmdSec();
            amdSec.setID("TMD_hdl_" + item.getHandle());

            // FIXME: techMD here
            // License as <rightsMD><mdWrap><binData>base64encoded</binData>...
            InputStream licenseStream = findLicense(context, item);

            if (licenseStream != null)
            {
                RightsMD rightsMD = new RightsMD();
                MdWrap rightsMDWrap = new MdWrap();
                rightsMDWrap.setMIMETYPE("text/plain");
                rightsMDWrap.setMDTYPE(Mdtype.OTHER);
                rightsMDWrap.setOTHERMDTYPE("TEXT");

                BinData binData = new BinData();
                Base64 base64 = new Base64(licenseStream);

                binData.getContent().add(base64);
                rightsMDWrap.getContent().add(binData);
                rightsMD.getContent().add(rightsMDWrap);
                amdSec.getContent().add(rightsMD);
            }

            // FIXME: History data???? Nooooo!!!!
            mets.getContent().add(amdSec);

            // fileSec
            FileSec fileSec = new FileSec();
            boolean fileSecEmpty = true;

            Bundle[] bundles = item.getBundles();

            for (int i = 0; i < bundles.length; i++)
            {
                Bitstream[] bitstreams = bundles[i].getBitstreams();

                // First: we skip the license bundle, since it's included
                // elsewhere
                if (bitstreams[0].getFormat().getID() == licenseFormat)
                {
                    continue;
                }

                // Create a fileGrp
                FileGrp fileGrp = new FileGrp();

                // Bundle name for USE attribute
                if ((bundles[i].getName() != null)
                        && !bundles[i].getName().equals(""))
                {
                    fileGrp.setUSE(bundles[i].getName());
                }

                for (int bits = 0; bits < bitstreams.length; bits++)
                {
                    // What's the persistent(-ish) ID?
                    String bitstreamPID = ConfigurationManager
                            .getProperty("dspace.url")
                            + "/bitstream/"
                            + item.getHandle()
                            + "/"
                            + bitstreams[bits].getSequenceID()
                            + "/"
                            + Util.encodeBitstreamName(bitstreams[bits].getName(),
                                    "UTF-8");

                    edu.harvard.hul.ois.mets.File file = new edu.harvard.hul.ois.mets.File();

                    /*
                     * ID: we use the unique part of the persistent ID, i.e. the
                     * Handle + sequence number, but with _'s instead of /'s so
                     * it's a legal xsd:ID.
                     */
                    String xmlIDstart = item.getHandle().replaceAll("/", "_")
                            + "_";

                    file.setID(xmlIDstart + bitstreams[bits].getSequenceID());

                    String groupID = "GROUP_" + xmlIDstart
                            + bitstreams[bits].getSequenceID();

                    /*
                     * If we're in THUMBNAIL or TEXT bundles, the bitstream is
                     * extracted text or a thumbnail, so we use the name to work
                     * out which bitstream to be in the same group as
                     */
                    if ((bundles[i].getName() != null)
                            && (bundles[i].getName().equals("THUMBNAIL") || bundles[i]
                                    .getName().equals("TEXT")))
                    {
                        // Try and find the original bitstream, and chuck the
                        // derived
                        // bitstream in the same group
                        Bitstream original = findOriginalBitstream(item,
                                bitstreams[bits]);

                        if (original != null)
                        {
                            groupID = "GROUP_" + xmlIDstart
                                    + original.getSequenceID();
                        }
                    }

                    file.setGROUPID(groupID);
                    file.setOWNERID(bitstreamPID);

                    // FIXME: ADMID should point to appropriate TechMD section
                    // above
                    file
                            .setMIMETYPE(bitstreams[bits].getFormat()
                                    .getMIMEType());

                    // FIXME: CREATED: no date
                    file.setSIZE(bitstreams[bits].getSize());
                    file.setCHECKSUM(bitstreams[bits].getChecksum());
                    file.setCHECKSUMTYPE(Checksumtype.MD5);

                    // FLocat: filename is as in records, or full URL
                    // FIXME: Duplicate filenames and characters illegal to
                    // local OS may cause problems
                    FLocat flocat = new FLocat();
                    flocat.setLOCTYPE(Loctype.URL);
                    if (fullURL)
                    {
                        flocat.setXlinkHref(bitstreamPID);
                    }
                    else
                    {
                        flocat.setXlinkHref(bitstreams[bits].getName());
                    }

                    // Add FLocat to File, and File to FileGrp
                    file.getContent().add(flocat);
                    fileGrp.getContent().add(file);
                }

                // Add fileGrp to fileSec
                fileSec.getContent().add(fileGrp);
                fileSecEmpty = false;
            }

            // Add fileSec to document
            if (!fileSecEmpty)
            {
                mets.getContent().add(fileSec);
            }
            
            // FIXME: Add Structmap here, but it is empty and we won't use it now.
            StructMap structMap = new StructMap();
            Div div = new Div();
            structMap.getContent().add(div);
            mets.getContent().add(structMap);

            
            mets.validate(new MetsValidator());

            mets.write(new MetsWriter(os));
        }
        catch (MetsException e)
        {
            // We don't pass up a MetsException, so callers don't need to
            // know the details of the METS toolkit
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Utility to find the license bitstream from an item
     * 
     * @param context
     *            DSpace context
     * @param item
     *            the item
     * @return the license as a string
     * 
     * @throws IOException
     *             if the license bitstream can't be read
     */
    private static InputStream findLicense(Context context, Item item)
            throws SQLException, IOException, AuthorizeException
    {
        Bundle[] bundles = item.getBundles();

        for (int i = 0; i < bundles.length; i++)
        {
            // Assume license will be in its own bundle
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            if (bitstreams[0].getFormat().getID() == licenseFormat)
            {
                // Read the license into a string
                return bitstreams[0].retrieve();
            }
        }

        // Oops! No license!
        return null;
    }

    /**
     * For a bitstream that's a thumbnail or extracted text, find the
     * corresponding bitstream in the ORIGINAL bundle
     * 
     * @param item
     *            the item we're dealing with
     * @param derived
     *            the derived bitstream
     * 
     * @return the corresponding original bitstream (or null)
     */
    private static Bitstream findOriginalBitstream(Item item, Bitstream derived)
    				throws SQLException
    {
        Bundle[] bundles = item.getBundles();

        // Filename of original will be filename of the derived bitstream
        // minus the extension (last 4 chars - .jpg or .txt)
        String originalFilename = derived.getName().substring(0,
                derived.getName().length() - 4);

        // First find "original" bundle
        for (int i = 0; i < bundles.length; i++)
        {
            if ((bundles[i].getName() != null)
                    && bundles[i].getName().equals("ORIGINAL"))
            {
                // Now find the corresponding bitstream
                Bitstream[] bitstreams = bundles[i].getBitstreams();

                for (int bsnum = 0; bsnum < bitstreams.length; bsnum++)
                {
                    if (bitstreams[bsnum].getName().equals(originalFilename))
                    {
                        return bitstreams[bsnum];
                    }
                }
            }
        }

        // Didn't find it
        return null;
    }

    /**
     * Create MODS metadata from the DC in the item, and add to the given
     * XmlData METS object.
     * 
     * @param item
     *            the item
     * @param xmlData
     *            xmlData to add MODS to.
     */
    private static void createMODS(Item item, XmlData xmlData)
    {
        DCValue[] dc = item.getDC(Item.ANY, Item.ANY, Item.ANY);

        StringBuffer modsXML = new StringBuffer();

        for (int i = 0; i < dc.length; i++)
        {
            // Get the property name - element[.qualifier]
            String propName = ((dc[i].qualifier == null) ? dc[i].element
                    : (dc[i].element + "." + dc[i].qualifier));

            String modsMapping = dcToMODS.getProperty(propName);

            if (modsMapping == null)
            {
                System.err.println("WARNING: No MODS mapping for " + propName);
            }
            else
            {
                // Replace '%s' with DC value (with entities encoded)
                modsXML.append(modsMapping.replaceAll("%s", Utils
                        .addEntities(dc[i].value)));
                modsXML.append("\n"); // For readability
            }
        }

        PreformedXML pXML = new PreformedXML(modsXML.toString());
        xmlData.getContent().add(pXML);
    }

    /**
     * Get the handle from the command line in the form 123.456/789. Doesn't
     * matter if incoming handle has 'hdl:' or 'http://hdl....' before it.
     * 
     * @param original
     *            Handle as passed in by user
     * @return Handle as can be looked up in our table
     */
    private static String getHandleArg(String original)
    {
        if (original.startsWith("hdl:"))
        {
            return original.substring(4);
        }

        if (original.startsWith("http://hdl.handle.net/"))
        {
            return original.substring(22);
        }

        return original;
    }
}
