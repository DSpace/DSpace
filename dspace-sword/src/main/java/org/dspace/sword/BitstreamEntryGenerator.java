/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.purl.sword.atom.Content;
import org.purl.sword.atom.ContentType;
import org.purl.sword.atom.InvalidMediaTypeException;
import org.purl.sword.atom.Link;
import org.purl.sword.atom.Rights;
import org.purl.sword.atom.Title;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Richard Jones
 *
 * Class to generate ATOM Entry documents for DSpace Bitstreams
 */
public class BitstreamEntryGenerator extends DSpaceATOMEntry
{
    /** logger */
    private static Logger log = Logger.getLogger(BitstreamEntryGenerator.class);

    /**
     * Create a new ATOM Entry generator which can provide a SWORD Entry for
     * a bitstream
     *
     * @param service
     */
    protected BitstreamEntryGenerator(SWORDService service)
    {
        super(service);
        log.debug("Create new instance of BitstreamEntryGenerator");
    }

    /**
     * Add all the subject classifications from the bibliographic
     * metadata.
     *
     */
    protected void addCategories()
    {
        // do nothing
    }

    /**
     * Set the content type that DSpace received.
     *
     */
    protected void addContentElement()
            throws DSpaceSWORDException
    {
        try
        {
            // get the things we need out of the service
            SWORDUrlManager urlManager = swordService.getUrlManager();

            // if this is a deposit which is no op we can't do anything here
            if (this.deposit != null && this.deposit.isNoOp())
            {
                return;
            }

            String bsurl = urlManager.getBitstreamUrl(this.bitstream);
            BitstreamFormat bf = null;
            try
            {
                bf = this.bitstream.getFormat(swordService.getContext());
            }
            catch (SQLException e)
            {
                log.error("Exception caught: ", e);
                throw new DSpaceSWORDException(e);
            }
            String format = "application/octet-stream";
            if (bf != null)
            {
                format = bf.getMIMEType();
            }

            Content con = new Content();
            con.setType(format);
            con.setSource(bsurl);
            entry.setContent(con);

            log.debug("Adding content element with url=" + bsurl);
        }
        catch (InvalidMediaTypeException e)
        {
            log.error("caught and swallowed exception: ", e);
            // do nothing; we'll live without the content type declaration!
        }
    }

    /**
     * Add the identifier for the item.  If the item object has
     * a handle already assigned, this is used, otherwise, the
     * passed handle is used.  It is set in the form that
     * they can be used to access the resource over http (i.e.
     * a real URL).
     */
    protected void addIdentifier()
            throws DSpaceSWORDException
    {
        // if this is a deposit which is no op we can't do anything here
        if (this.deposit != null && this.deposit.isNoOp())
        {
            // just use the dspace url as the
            // property
            String cfg = ConfigurationManager.getProperty("dspace.url");
            entry.setId(cfg);

            return;
        }

        SWORDUrlManager urlManager = swordService.getUrlManager();

        // for a bitstream, we just use the url for the bitstream
        // as the identifier
        String bsurl = urlManager.getBitstreamUrl(this.bitstream);
        entry.setId(bsurl);
        log.debug("Added identifier for bitstream with url=" + bsurl);
        return;

        // FIXME: later on we will maybe have a workflow page supplied
        // by the sword interface?
    }

    /**
     * Add links associated with this item.
     *
     */
    protected void addLinks()
            throws DSpaceSWORDException
    {
        // if this is a deposit which is no op we can't do anything here
        if (this.deposit != null && this.deposit.isNoOp())
        {
            return;
        }

        // get the things we need out of the service
        SWORDUrlManager urlManager = swordService.getUrlManager();

        String bsurl = urlManager.getBitstreamUrl(this.bitstream);
        BitstreamFormat bf;
        try
        {
            bf = this.bitstream.getFormat(swordService.getContext());
        }
        catch (SQLException e)
        {
            log.error("Exception caught: ", e);
            throw new DSpaceSWORDException(e);
        }
        String format = "application/octet-stream";
        if (bf != null)
        {
            format = bf.getMIMEType();
        }

        Link link = new Link();
        link.setType(format);
        link.setHref(bsurl);
        link.setRel("alternate");
        entry.addLink(link);

        log.debug("Added link entity to entry for url " + bsurl);
    }

    /**
     * Add the date of publication from the bibliographic metadata
     *
     */
    protected void addPublishDate()
    {
        // do nothing
    }

    /**
     * Add rights information.  This attaches an href to the URL
     * of the item's licence file
     *
     */
    protected void addRights()
            throws DSpaceSWORDException
    {
        try
        {
            // work our way up to the item
            List<Bundle> bundle2bitstreams = this.bitstream
                    .getBundles();
            if (bundle2bitstreams.isEmpty())
            {
                log.error("Found orphaned bitstream: " + bitstream.getID());
                throw new DSpaceSWORDException("Orphaned bitstream discovered");
            }
            Bundle bundle = bundle2bitstreams.get(0);
            List<Item> items = bundle.getItems();
            if (items.isEmpty())
            {
                log.error("Found orphaned bundle: " + bundle.getID());
                throw new DSpaceSWORDException("Orphaned bundle discovered");
            }
            Item item = items.get(0);

            // now get the licence out of the item
            SWORDUrlManager urlManager = swordService.getUrlManager();
            StringBuilder rightsString = new StringBuilder();
            List<Bundle> lbundles = item.getBundles();
            for (Bundle lbundle : lbundles)
            {
                if (!Constants.LICENSE_BUNDLE_NAME.equals(lbundle.getName()))
                {
                    // skip non-license bundles
                    continue;
                }
                List<Bitstream> bss = lbundle.getBitstreams();
                for (Bitstream bs : bss)
                {
                    String url = urlManager.getBitstreamUrl(bs);
                    rightsString.append(url).append(" ");
                }
            }

            Rights rights = new Rights();
            rights.setContent(rightsString.toString());
            rights.setType(ContentType.TEXT);
            entry.setRights(rights);
            log.debug("Added rights entry to entity");
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new DSpaceSWORDException(e);
        }
    }

    /**
     * Add the summary/abstract from the bibliographic metadata
     *
     */
    protected void addSummary()
    {
        // do nothing
    }

    /**
     * Add the title from the bibliographic metadata
     *
     */
    protected void addTitle()
    {
        Title title = new Title();
        title.setContent(this.bitstream.getName());
        title.setType(ContentType.TEXT);
        entry.setTitle(title);
        log.debug("Added title to entry");
    }

    /**
     * Add the date that this item was last updated
     *
     */
    protected void addLastUpdatedDate()
    {
        // do nothing
    }
}
