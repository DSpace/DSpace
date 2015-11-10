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
import org.apache.abdera.model.Link;
import org.dspace.content.*;
import org.dspace.core.*;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class FeedContentDisseminator extends AbstractSimpleDC
        implements SwordContentDisseminator
{
    public InputStream disseminate(Context context, Item item)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        try
        {
            Abdera abdera = new Abdera();
            Feed feed = abdera.newFeed();

            this.addMetadata(feed, item);

            List<Bundle> bundles = item.getBundles();
            for (Bundle bundle : bundles)
            {
                if (Constants.CONTENT_BUNDLE_NAME.equals(bundle.getName()))
                {
                    List<Bitstream> bitstreams = bundle
                            .getBitstreams();
                    for (Bitstream bitstream : bitstreams)
                    {
                        Entry entry = feed.addEntry();
                        this.populateEntry(context, entry,
                                bitstream);
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            feed.writeTo(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        }
        catch (IOException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    private void addMetadata(Feed feed, Item item)
    {
        SimpleDCMetadata md = this.getMetadata(item);

        /* not necessary ...
        Map<String, String> dc = md.getDublinCore();
        for (String element : dc.keySet())
        {
            String value = dc.get(element);
            feed.addSimpleExtension(new QName(UriRegistry.DC_NAMESPACE, element), value);
        }
        */

        Map<String, String> atom = md.getAtom();
        for (String element : atom.keySet())
        {
            if ("author".equals(element))
            {
                feed.addAuthor(atom.get(element));
            }
        }

        // ensure that the feed has one author or more
        if (feed.getAuthors().size() == 0)
        {
            feed.addAuthor(ConfigurationManager.getProperty("dspace.name"));
        }
    }

    private void populateEntry(Context context, Entry entry,
            Bitstream bitstream)
            throws DSpaceSwordException
    {
        BitstreamFormat format = null;
        try
        {
            format = bitstream.getFormat(context);
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
        String contentType = null;
        if (format != null)
        {
            contentType = format.getMIMEType();
        }

        SwordUrlManager urlManager = new SwordUrlManager(
                new SwordConfigurationDSpace(), context);
        String bsUrl = urlManager.getBitstreamUrl(bitstream);

        entry.setId(bsUrl);
        entry.setTitle(bitstream.getName());
        String desc = bitstream.getDescription();
        if ("".equals(desc) || desc == null)
        {
            desc = bitstream.getName();
        }
        entry.setSummary(desc);
        entry.setUpdated(new Date()); // required, though content is spurious

        // add an edit-media link for the bitstream ...
        Abdera abdera = new Abdera();
        Link link = abdera.getFactory().newLink();
        link.setHref(urlManager.getActionableBitstreamUrl(bitstream));
        link.setMimeType(contentType);
        link.setRel("edit-media");
        entry.addLink(link);

        // set the content of the bitstream
        entry.setContent(new IRI(bsUrl), contentType);
    }

    public boolean disseminatesContentType(String contentType)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        return "application/atom+xml".equals(contentType) ||
                "application/atom+xml;type=feed".equals(contentType);
    }

    public boolean disseminatesPackage(String contentType)
            throws DSpaceSwordException, SwordError, SwordServerException
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
