/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.core.Utils;

/**
 * Accept a PDF file by itself as a SIP.
 * <p>
 * This is mainly a proof-of-concept to demonstrate the flexibility
 * of the packager and crosswalk plugins.
 * <p>
 * To import, open up the PDF and try to extract sufficient metadata
 * from its InfoDict.
 * <p>
 * Export is a crude hack: if the item has a bitstream containing PDF,
 * send that, otherwise it fails. Do not attempt to insert metadata.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see PackageIngester
 * @see PackageDisseminator
 */
public class PDFPackager
       extends SelfNamedPlugin
       implements PackageIngester, PackageDisseminator
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(PDFPackager.class);

    private static final String BITSTREAM_FORMAT_NAME = "Adobe PDF";

    private static String aliases[] = { "PDF", "Adobe PDF", "pdf", "application/pdf" };

    public static String[] getPluginNames()
    {
        return (String[]) ArrayUtils.clone(aliases);
    }

    // utility to grovel bitstream formats..
    private static void setFormatToMIMEType(Context context, Bitstream bs, String mimeType)
        throws SQLException
    {
        BitstreamFormat bf[] = BitstreamFormat.findNonInternal(context);
        for (int i = 0; i < bf.length; ++i)
        {
            if (bf[i].getMIMEType().equalsIgnoreCase(mimeType))
            {
                bs.setFormat(bf[i]);
                break;
            }
        }
    }

    /**
     * Create new Item out of the ingested package, in the indicated
     * collection.  It creates a workspace item, which the application
     * can then install if it chooses to bypass Workflow.
     * <p>
     * This is a VERY crude import of a single Adobe PDF (Portable
     * Document Format) file, using the document's embedded metadata
     * for package metadata.  If the PDF file hasn't got the minimal
     * metadata available, it is rejected.
     * <p>
     * @param context  DSpace context.
     * @param parent  collection under which to create new item.
     * @param pkgFile  The package file to ingest
     * @param params  package parameters (none recognized)
     * @param license  may be null, which takes default license.
     * @return workspace item created by ingest.
     * @throws PackageException if package is unacceptable or there is
     *  a fatal error turning it into an Item.
     */
    public DSpaceObject ingest(Context context, DSpaceObject parent,
                                File pkgFile, PackageParameters params,
                                String license)
        throws PackageValidationException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        boolean success = false;
        Bundle original = null;
        Bitstream bs = null;
        WorkspaceItem wi = null;

        try
        {
            // Save the PDF in a bitstream first, since the parser
            // has to read it as well, and we cannot "rewind" it after that.
            wi = WorkspaceItem.create(context, (Collection)parent, false);
            Item myitem = wi.getItem();
            original = myitem.createBundle("ORIGINAL");

            InputStream fileStream = null;
            try
            {
                fileStream = new FileInputStream(pkgFile);
                bs = original.createBitstream(fileStream);
            }
            finally
            {
                if (fileStream != null)
                {
                    fileStream.close();
                }
            }

            bs.setName("package.pdf");
            setFormatToMIMEType(context, bs, "application/pdf");
            bs.update();
            if (log.isDebugEnabled())
            {
                log.debug("Created bitstream ID=" + String.valueOf(bs.getID()) + ", parsing...");
            }

            crosswalkPDF(context, myitem, bs.retrieve());

            wi.update();
            context.commit();
            success = true;
            log.info(LogManager.getHeader(context, "ingest",
                "Created new Item, db ID="+String.valueOf(myitem.getID())+
                ", WorkspaceItem ID="+String.valueOf(wi.getID())));

            myitem = PackageUtils.finishCreateItem(context, wi, null, params);
            return myitem;
        }
        finally
        {
            // get rid of bitstream and item if ingest fails
            if (!success)
            {
                if (original != null && bs != null)
                {
                    original.removeBitstream(bs);
                }
                if (wi != null)
                {
                    wi.deleteAll();
                }
            }
            context.commit();
        }
    }

    /**
     * IngestAll() cannot be implemented for a PDF ingester, because there's only one PDF to ingest
     */
    public List<String> ingestAll(Context context, DSpaceObject parent, File pkgFile,
                                PackageParameters params, String license)
        throws PackageException, UnsupportedOperationException,
               CrosswalkException, AuthorizeException,
               SQLException, IOException
    {
        throw new UnsupportedOperationException("PDF packager does not support the ingestAll() operation at this time.");
    }


    /**
     * Replace is not implemented.
     */
    public DSpaceObject replace(Context context, DSpaceObject dso,
                            File pkgFile, PackageParameters params)
        throws PackageException, UnsupportedOperationException,
               CrosswalkException, AuthorizeException,
               SQLException, IOException
    {
        throw new UnsupportedOperationException("PDF packager does not support the replace() operation at this time.");
    }

    /**
     * ReplaceAll() cannot be implemented for a PDF ingester, because there's only one PDF to ingest
     */
    public List<String> replaceAll(Context context, DSpaceObject dso,
                                File pkgFile, PackageParameters params)
        throws PackageException, UnsupportedOperationException,
               CrosswalkException, AuthorizeException,
               SQLException, IOException
    {
        throw new UnsupportedOperationException("PDF packager does not support the replaceAll() operation at this time.");
    }

    /**
     * VERY crude dissemination: just look for the first
     * bitstream with the PDF package type, and toss it out.
     * Works on packages importer with this packager, and maybe some others.
     */
    public void disseminate(Context context, DSpaceObject dso,
                            PackageParameters params, File pkgFile)
        throws PackageValidationException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        if (dso.getType() != Constants.ITEM)
        {
            throw new PackageValidationException("This disseminator can only handle objects of type ITEM.");
        }

        Item item = (Item)dso;
        BitstreamFormat pdff = BitstreamFormat.findByShortDescription(context,
                                BITSTREAM_FORMAT_NAME);
        if (pdff == null)
        {
            throw new PackageValidationException("Cannot find BitstreamFormat \"" + BITSTREAM_FORMAT_NAME + "\"");
        }
        Bitstream pkgBs = PackageUtils.getBitstreamByFormat(item, pdff, Constants.DEFAULT_BUNDLE_NAME);
        if (pkgBs == null)
        {
            throw new PackageValidationException("Cannot find Bitstream with format \"" + BITSTREAM_FORMAT_NAME + "\"");
        }

        //Make sure our package file exists
        if(!pkgFile.exists())
        {
            PackageUtils.createFile(pkgFile);
        }

        //open up output stream to copy bitstream to file
        FileOutputStream out = null;
        try
        {
            //open up output stream to copy bitstream to file
            out = new FileOutputStream(pkgFile);
            Utils.copy(pkgBs.retrieve(), out);
        }
        finally
        {
            if (out != null)
            {
                out.close();
            }
        }
    }

    /**
     * disseminateAll() cannot be implemented for a PDF disseminator, because there's only one PDF to disseminate
     */
    public List<File> disseminateAll(Context context, DSpaceObject dso,
                     PackageParameters params, File pkgFile)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        throw new UnsupportedOperationException("PDF packager does not support the disseminateAll() operation at this time.");
    }


    /**
     * Identifies the MIME-type of this package, i.e. "application/pdf".
     *
     * @return the MIME type (content-type header) of the package to be returned
     */
    public String getMIMEType(PackageParameters params)
    {
        return "application/pdf";
    }

    private void crosswalkPDF(Context context, Item item, InputStream metadata)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        COSDocument cos = null;

        try
        {
            PDFParser parser = new PDFParser(metadata);
            parser.parse();
            cos = parser.getDocument();

            // sanity check: PDFBox breaks on encrypted documents, so give up.
            if(cos.getEncryptionDictionary() != null)
            {
                throw new MetadataValidationException("This packager cannot accept an encrypted PDF document.");
            }

            /* PDF to DC "crosswalk":
             *
             * NOTE: This is not in a crosswalk plugin because (a) it isn't
             * useful anywhere else, and more importantly, (b) the source
             * data is not XML so it doesn't fit the plugin's interface.
             *
             * pattern of crosswalk -- PDF dict entries to DC:
             *   Title -> title.null
             *   Author -> contributor.author
             *   CreationDate -> date.created
             *   ModDate -> date.created
             *   Creator -> description.provenance (application that created orig)
             *   Producer -> description.provenance (convertor to pdf)
             *   Subject -> description.abstract
             *   Keywords -> subject.other
             *    date is java.util.Calendar
             */
            PDDocument pd = new PDDocument(cos);
            PDDocumentInformation docinfo = pd.getDocumentInformation();
            String title = docinfo.getTitle();

            // sanity check: item must have a title.
            if (title == null)
            {
                throw new MetadataValidationException("This PDF file is unacceptable, it does not have a value for \"Title\" in its Info dictionary.");
            }
            if (log.isDebugEnabled())
            {
                log.debug("PDF Info dict title=\"" + title + "\"");
            }
            item.addDC("title", null, "en", title);
            String value = docinfo.getAuthor();
            if (value != null)
            {
                item.addDC("contributor", "author", null, value);
                if (log.isDebugEnabled())
                {
                    log.debug("PDF Info dict author=\"" + value + "\"");
                }
            }

            value = docinfo.getCreator();
            if (value != null)
            {
                item.addDC("description", "provenance", "en",
                        "Application that created the original document: " + value);
            }

            value = docinfo.getProducer();
            if (value != null)
            {
                item.addDC("description", "provenance", "en",
                        "Original document converted to PDF by: " + value);
            }

            value = docinfo.getSubject();
            if (value != null)
            {
                item.addDC("description", "abstract", null, value);
            }

            value = docinfo.getKeywords();
            if (value != null)
            {
                item.addDC("subject", "other", null, value);
            }

            // Take either CreationDate or ModDate as "date.created",
            // Too bad there's no place to put "last modified" in the DC.
            Calendar calValue = docinfo.getCreationDate();
            if (calValue == null)
            {
                calValue = docinfo.getModificationDate();
            }

            if (calValue != null)
            {
                item.addDC("date", "created", null,
                        (new DCDate(calValue.getTime())).toString());
            }
            item.update();
        }
        finally
        {
            if (cos != null)
            {
                cos.close();
            }
        }
    }


    /**
     * Returns a user help string which should describe the
     * additional valid command-line options that this packager
     * implementation will accept when using the <code>-o</code> or
     * <code>--option</code> flags with the Packager script.
     *
     * @return a string describing additional command-line options available
     * with this packager
     */
    @Override
    public String getParameterHelp()
    {
        return "No additional options available.";
    }

}
