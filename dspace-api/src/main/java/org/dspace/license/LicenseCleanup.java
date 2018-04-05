/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

/**
 * Cleanup class for CC Licenses, corrects XML formating errors by replacing the license_rdf bitstream.
 * 
 * @author mdiggory
 */
public class LicenseCleanup
{

    private static final Logger log = Logger.getLogger(LicenseCleanup.class);

    protected static final Templates templates;

    protected static final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected static final BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    static
    {

        try
        {
            templates = TransformerFactory.newInstance().newTemplates(
                    new StreamSource(CreativeCommonsServiceImpl.class
                            .getResourceAsStream("LicenseCleanup.xsl")));
        }
        catch (TransformerConfigurationException e)
        {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * @param args
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws AuthorizeException if authorization error
     */
    public static void main(String[] args) throws SQLException,
            AuthorizeException, IOException
    {

        Context ctx = new Context();
        ctx.turnOffAuthorisationSystem();
        Iterator<Item> iter = itemService.findAll(ctx);

        Properties props = new Properties();

        File processed = new File("license.processed");

        if (processed.exists())
        {
            props.load(new FileInputStream(processed));
        }

        int i = 0;

        try
        {
            while (iter.hasNext())
            {
                if (i == 100)
                {
                    props.store(new FileOutputStream(processed),
                                    "processed license files, remove to restart processing from scratch");
                    i = 0;
                }

                Item item = (Item) iter.next();
                log.info("checking: " + item.getID());
                if (!props.containsKey("I" + item.getID()))
                {
                    handleItem(ctx, item);
                    log.info("processed: " + item.getID());
                }

                props.put("I" + item.getID(), "done");
                i++;

            }

        }
        finally
        {
            props
                    .store(new FileOutputStream(processed),
                            "processed license files, remove to restart processing from scratch");
        }

    }

    /**
     * Process Item, correcting CC-License if encountered.
     * @param item
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     */
    protected static void handleItem(Context context, Item item) throws SQLException,
            AuthorizeException, IOException
    {
        List<Bundle> bundles = itemService.getBundles(item, "CC-LICENSE");

        if (bundles == null || bundles.size() == 0)
        {
            return;
        }

        Bundle bundle = bundles.get(0);

        Bitstream bitstream = bundleService.getBitstreamByName(bundle, "license_rdf");

        String license_rdf = new String(copy(context, bitstream));

        /* quickly fix xml by ripping out offensive parts */
        license_rdf = license_rdf.replaceFirst("<license", "");
        license_rdf = license_rdf.replaceFirst("</license>", "");

        StringWriter result = new StringWriter();

        try
        {
            templates.newTransformer().transform(
                    new StreamSource(new ByteArrayInputStream(license_rdf
                            .getBytes())), new StreamResult(result));
        }
        catch (TransformerException e)
        {
            throw new IllegalStateException(e.getMessage(), e);
        }

        StringBuffer buffer = result.getBuffer();

        Bitstream newBitstream = bitstreamService
                .create(context, bundle, new ByteArrayInputStream(buffer.toString()
                        .getBytes()));

        newBitstream.setName(context, bitstream.getName());
        newBitstream.setDescription(context, bitstream.getDescription());
        newBitstream.setFormat(context, bitstream.getFormat(context));
        newBitstream.setSource(context, bitstream.getSource());
        newBitstream.setUserFormatDescription(context, bitstream
                .getUserFormatDescription());
        bitstreamService.update(context, newBitstream);

        bundleService.removeBitstream(context, bundle, bitstream);

        bundleService.update(context, bundle);

    }

    static final int BUFF_SIZE = 100000;

    static final byte[] buffer = new byte[BUFF_SIZE];

    /**
     * Fast stream copy routine
     * 
     * @param b the Bitstream to be copied.
     * @return copy of the content of {@code b}.
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public static byte[] copy(Context context, Bitstream b) throws IOException, SQLException,
            AuthorizeException
    {
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try
        {
            in = bitstreamService.retrieve(context, b);
            out = new ByteArrayOutputStream();
            while (true)
            {
                synchronized (buffer)
                {
                    int amountRead = in.read(buffer);
                    if (amountRead == -1)
                    {
                        break;
                    }
                    out.write(buffer, 0, amountRead);
                }
            }
        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
            if (out != null)
            {
                out.close();
            }
        }

        return out.toByteArray();
    }

}
