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
import org.dspace.content.ItemIterator;
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

    static
    {

        try
        {
            templates = TransformerFactory.newInstance().newTemplates(
                    new StreamSource(CreativeCommons.class
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
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public static void main(String[] args) throws SQLException,
            AuthorizeException, IOException
    {

        Context ctx = new Context();
        ctx.turnOffAuthorisationSystem();
        ItemIterator iter = Item.findAll(ctx);

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
                    handleItem(item);
                    log.info("processed: " + item.getID());
                }

                item.decache();
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
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    protected static void handleItem(Item item) throws SQLException,
            AuthorizeException, IOException
    {
        Bundle[] bundles = item.getBundles("CC-LICENSE");

        if (bundles == null || bundles.length == 0)
        {
            return;
        }

        Bundle bundle = bundles[0];

        Bitstream bitstream = bundle.getBitstreamByName("license_rdf");

        String license_rdf = new String(copy(bitstream));

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

        Bitstream newBitstream = bundle
                .createBitstream(new ByteArrayInputStream(buffer.toString()
                        .getBytes()));

        newBitstream.setName(bitstream.getName());
        newBitstream.setDescription(bitstream.getDescription());
        newBitstream.setFormat(bitstream.getFormat());
        newBitstream.setSource(bitstream.getSource());
        newBitstream.setUserFormatDescription(bitstream
                .getUserFormatDescription());
        newBitstream.update();

        bundle.removeBitstream(bitstream);

        bundle.update();

    }

    static final int BUFF_SIZE = 100000;

    static final byte[] buffer = new byte[BUFF_SIZE];

    /**
     * Fast stream copy routine
     * 
     * @param b the Bitstream to be copied.
     * @return copy of the content of {@code b}.
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    public static byte[] copy(Bitstream b) throws IOException, SQLException,
            AuthorizeException
    {
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try
        {
            in = b.retrieve();
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
