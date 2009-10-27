/*
 * PDFPackager.java
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

package org.dspace.content.packager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Calendar;

import org.apache.log4j.Logger;
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
import org.pdfbox.cos.COSDocument;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.PDDocumentInformation;
                                    
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
 */
public class PDFPackager
       extends SelfNamedPlugin
       implements PackageIngester, PackageDisseminator
{
    /** log4j category */
    private static Logger log = Logger.getLogger(PDFPackager.class);

    private final static String BITSTREAM_FORMAT_NAME = "Adobe PDF";

    private static String aliases[] = { "PDF", "Adobe PDF", "pdf", "application/pdf" };

    public static String[] getPluginNames()
    {
        return aliases;
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
     * @param collection  collection under which to create new item.
     * @param pkg  input stream containing package to ingest.
     * @param params  package parameters (none recognized)
     * @param license  may be null, which takes default license.
     * @return workspace item created by ingest.
     * @throws PackageException if package is unacceptable or there is
     *  a fatal error turning it into an Item.
     */
    public WorkspaceItem ingest(Context context, Collection collection,
                                InputStream pkg, PackageParameters params,
                                String license)
        throws PackageValidationException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        InputStream bis = null;
        COSDocument cos = null;
        boolean success = false;
        Bundle original = null;
        Bitstream bs = null;
        WorkspaceItem wi = null;

        /** XXX comment out for now
          // XXX for debugging of parameter handling
          if (params != null)
          {
              Enumeration pe = params.propertyNames();
              while (pe.hasMoreElements())
              {
                  String name = (String)pe.nextElement();
                  String v[] = params.getProperties(name);
                  StringBuffer msg = new StringBuffer("PackageParam: ");
                  msg.append(name).append(" = ");
                  for (int i = 0; i < v.length; ++i)
                  {
                      if (i > 0)
                          msg.append(", ");
                      msg.append(v[i]);
                  }
                  log.debug(msg);
              }
          }
        **/
           
        try
        {
            // Save the PDF in a bitstream first, since the parser
            // has to read it as well, and we cannot "rewind" it after that.
            wi = WorkspaceItem.create(context, collection, false);
            Item myitem = wi.getItem();
            original = myitem.createBundle("ORIGINAL");
            bs = original.createBitstream(pkg);
            pkg.close();
            bs.setName("package.pdf");
            setFormatToMIMEType(context, bs, "application/pdf");
            bs.update();
            log.debug("Created bitstream ID="+String.valueOf(bs.getID())+", parsing...");

            crosswalkPDF(context, myitem, bs.retrieve());

            wi.update();
            context.commit();
            success = true;
            log.info(LogManager.getHeader(context, "ingest",
                "Created new Item, db ID="+String.valueOf(myitem.getID())+
                ", WorkspaceItem ID="+String.valueOf(wi.getID())));
            return wi;
        }
        finally
        {
            try
            {
                // Close bitstream input stream and PDF file.
                if (bis != null)
                    bis.close();
                if (cos != null)
                    cos.close();
            }
            catch (IOException ie)
            { }

            // get rid of bitstream and item if ingest fails
            if (!success)
            {
                if (original != null && bs != null)
                    original.removeBitstream(bs);
                if (wi != null)
                    wi.deleteAll();
            }
            context.commit();
        }
    }

    /**
     * Replace is not implemented.
     */
    public Item replace(Context ctx, Item item, InputStream pckage, PackageParameters params)
        throws PackageValidationException, CrosswalkException,
               AuthorizeException, SQLException, IOException,
               UnsupportedOperationException
    {
        throw new UnsupportedOperationException("The replace operation is not implemented.");
    }

    /**
     * VERY crude dissemination: just look for the first
     * bitstream with the PDF package type, and toss it out.
     * Works on packages importer with this packager, and maybe some others.
     */
    public void disseminate(Context context, DSpaceObject dso,
                            PackageParameters params, OutputStream out)
        throws PackageValidationException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        if (dso.getType() != Constants.ITEM)
            throw new PackageValidationException("This disseminator can only handle objects of type ITEM.");

        Item item = (Item)dso;
        try
        {
            BitstreamFormat pdff = BitstreamFormat.findByShortDescription(context,
                                    BITSTREAM_FORMAT_NAME);
            if (pdff == null)
                throw new PackageValidationException("Cannot find BitstreamFormat \""+BITSTREAM_FORMAT_NAME+"\"");
            Bitstream pkgBs = PackageUtils.getBitstreamByFormat(item, pdff, Constants.DEFAULT_BUNDLE_NAME);
            if (pkgBs == null)
                throw new PackageValidationException("Cannot find Bitstream with format \""+BITSTREAM_FORMAT_NAME+"\"");
            Utils.copy(pkgBs.retrieve(), out);
        }
            finally {}
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
                throw new MetadataValidationException("This packager cannot accept an encrypted PDF document.");

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
                throw new MetadataValidationException("This PDF file is unacceptable, it does not have a value for \"Title\" in its Info dictionary.");
            log.debug("PDF Info dict title=\""+title+"\"");
            item.addDC("title", null, "en", title);
            String value;
            Calendar date;
            if ((value = docinfo.getAuthor()) != null)
            {
                item.addDC("contributor", "author", null, value);
                log.debug("PDF Info dict author=\""+value+"\"");
            }
            if ((value = docinfo.getCreator()) != null)
                item.addDC("description", "provenance", "en",
                              "Application that created the original document: "+value);
            if ((value = docinfo.getProducer()) != null)
                item.addDC("description", "provenance", "en",
                              "Original document converted to PDF by: "+value);
            if ((value = docinfo.getSubject()) != null)
                item.addDC("description", "abstract", null, value);
            if ((value = docinfo.getKeywords()) != null)
                item.addDC("subject", "other", null, value);

            // Take either CreationDate or ModDate as "date.created",
            // Too bad there's no place to put "last modified" in the DC.
            Calendar calValue;
            if ((calValue = docinfo.getCreationDate()) == null)
                calValue = docinfo.getModificationDate();
            if (calValue != null)
                item.addDC("date", "created", null,
                             (new DCDate(calValue.getTime())).toString());
            item.update();
        }
        finally
        {
            if (cos != null)
                cos.close();
        }
    }
}
