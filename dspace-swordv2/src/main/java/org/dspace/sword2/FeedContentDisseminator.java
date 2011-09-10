/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.*;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

public class FeedContentDisseminator implements SwordContentDisseminator
{
    public InputStream disseminate(Context context, Item item)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        try
        {
            Abdera abdera = new Abdera();
            Feed feed = abdera.newFeed();

            Bundle[] originals = item.getBundles("ORIGINAL");
            for (Bundle original : originals)
            {
                Bitstream[] bss = original.getBitstreams();
                for (Bitstream bitstream : bss)
                {
                    Entry entry = feed.addEntry();
                    this.populateEntry(context, entry, bitstream);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            feed.writeTo(baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            return bais;
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (IOException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    private void populateEntry(Context context, Entry entry, Bitstream bitstream)
            throws DSpaceSwordException
    {
        BitstreamFormat format = bitstream.getFormat();
        String contentType = null;
        if (format != null)
        {
            contentType = format.getMIMEType();
        }

        entry.setTitle(bitstream.getName());
        entry.setSummary(bitstream.getDescription());

        SwordUrlManager urlManager = new SwordUrlManager(new SwordConfigurationDSpace(), context);

        // set the content of the bitstream
        String bsUrl = urlManager.getBitstreamUrl(bitstream);
        entry.setContent(new IRI(bsUrl), contentType);
    }

    public boolean disseminatesContentType(String contentType)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        return "application/atom+xml".equals(contentType) || "application/atom+xml;type=feed".equals(contentType);
    }

    public boolean disseminatesPackage(String contentType) throws DSpaceSwordException, SwordError, SwordServerException
    {
        // we're just going to ignore packaging formats here
        return true;
    }

    public void setContentType(String contentType)
    {
        // we just return the one format, so ignore this
    }

    public void setPackaging(String packaging)
    {
        // we just return the one format, so ignore this
    }

    public String getContentType()
    {
        return "application/atom+xml;type=feed";
    }

    public String getPackaging()
    {
        // no packaging
        return null;
    }
}
