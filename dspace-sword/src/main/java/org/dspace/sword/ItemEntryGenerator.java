/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.purl.sword.atom.*;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Richard Jones
 *
 * Class to generate an ATOM Entry document for a DSpace Item
 */
public class ItemEntryGenerator extends DSpaceATOMEntry
{
    /** logger */
    private static Logger log = Logger.getLogger(ItemEntryGenerator.class);

    protected HandleService handleService = HandleServiceFactory.getInstance()
            .getHandleService();

    protected ItemService itemService = ContentServiceFactory.getInstance()
            .getItemService();

    protected ItemEntryGenerator(SWORDService service)
    {
        super(service);
    }

    /**
     * Add all the subject classifications from the bibliographic
     * metadata.
     *
     */
    protected void addCategories()
    {
        List<MetadataValue> dcv = itemService
                .getMetadataByMetadataString(item, "dc.subject.*");
        if (dcv != null)
        {
            for (MetadataValue aDcv : dcv)
            {
                entry.addCategory(aDcv.getValue());
            }
        }
    }

    /**
     * Set the content type that DSpace received.  This is just
     * "application/zip" in this default implementation.
     *
     */
    protected void addContentElement()
            throws DSpaceSWORDException
    {
        // get the things we need out of the service
        SWORDUrlManager urlManager = swordService.getUrlManager();

        try
        {
            if (!this.deposit.isNoOp())
            {
                String handle = "";
                if (item.getHandle() != null)
                {
                    handle = item.getHandle();
                }

                if (StringUtils.isNotBlank(handle))
                {
                    boolean keepOriginal = ConfigurationManager
                            .getBooleanProperty("sword-server",
                                    "keep-original-package");
                    String swordBundle = ConfigurationManager
                            .getProperty("sword-server", "bundle.name");
                    if (StringUtils.isBlank(swordBundle))
                    {
                        swordBundle = "SWORD";
                    }

                    // if we keep the original, then expose this as the content element
                    // otherwise, expose the unpacked version
                    if (keepOriginal)
                    {
                        Content con = new Content();
                        List<Bundle> bundles = item.getBundles();
                        for (Bundle bundle : bundles)
                        {
                            if (swordBundle.equals(bundle.getName()))
                            {
                                List<Bitstream> bss = bundle
                                        .getBitstreams();
                                for (Bitstream bs : bss)
                                {
                                    BitstreamFormat bf = bs
                                            .getFormat(
                                                    swordService.getContext());
                                    String format = "application/octet-stream";
                                    if (bf != null)
                                    {
                                        format = bf.getMIMEType();
                                    }
                                    con.setType(format);

                                    // calculate the bitstream link.
                                    String bsLink = urlManager
                                            .getBitstreamUrl(bs);
                                    con.setSource(bsLink);

                                    entry.setContent(con);
                                }
                                break;
                            }
                        }
                    }
                    else
                    {
                        // return a link to the DSpace entry page
                        Content content = new Content();
                        content.setType("text/html");
                        content.setSource(
                                handleService.getCanonicalForm(handle));
                        entry.setContent(content);
                    }
                }
            }
        }
        catch (InvalidMediaTypeException e)
        {
            // do nothing; we'll live without the content type declaration!
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new DSpaceSWORDException(e);
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
    {
        // it's possible that the item hasn't been assigned a handle yet
        if (!this.deposit.isNoOp())
        {
            String handle = "";
            if (item.getHandle() != null)
            {
                handle = item.getHandle();
            }

            if (StringUtils.isNotBlank(handle))
            {
                entry.setId(handleService.getCanonicalForm(handle));
                return;
            }
        }

        // if we get this far, then we just use the dspace url as the
        // property
        String cfg = ConfigurationManager.getProperty("dspace.url");
        entry.setId(cfg);

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
        SWORDUrlManager urlManager = swordService.getUrlManager();

        try
        {
            // if there is no handle, we can't generate links
            String handle = "";
            if (item.getHandle() != null)
            {
                handle = item.getHandle();
            }
            else
            {
                return;
            }

            // link to all the files in the item
            List<Bundle> bundles = item.getBundles();
            for (Bundle bundle : bundles)
            {
                if (Constants.CONTENT_BUNDLE_NAME.equals(bundle.getName()))
                {
                    List<Bitstream> bss = bundle.getBitstreams();
                    for (Bitstream bs : bss)
                    {
                        Link link = new Link();
                        String url = urlManager
                                .getBitstreamUrl(bs);
                        link.setHref(url);
                        link.setRel("part");

                        BitstreamFormat bsf = bs
                                .getFormat(swordService.getContext());
                        if (bsf != null)
                        {
                            link.setType(bsf.getMIMEType());
                        }

                        entry.addLink(link);
                    }
                    break;
                }
            }

            // link to the item splash page
            Link splash = new Link();
            splash.setHref(handleService.getCanonicalForm(handle));
            splash.setRel("alternate");
            splash.setType("text/html");
            entry.addLink(splash);
        }
        catch (SQLException e)
        {
            throw new DSpaceSWORDException(e);
        }
    }

    /**
     * Add the date of publication from the bibliographic metadata
     *
     */
    protected void addPublishDate()
    {
        List<MetadataValue> dcv = itemService
                .getMetadataByMetadataString(item, "dc.date.issued");
        if (dcv != null && !dcv.isEmpty())
        {
            entry.setPublished(dcv.get(0).getValue());
        }
    }

    /**
     * Add rights information.  This attaches an href to the URL
     * of the item's licence file
     *
     */
    protected void addRights()
            throws DSpaceSWORDException
    {
        SWORDUrlManager urlManager = swordService.getUrlManager();

        String handle = this.item.getHandle();

        // if there's no handle, we can't give a link
        if (StringUtils.isBlank(handle))
        {
            return;
        }

        String base = ConfigurationManager.getProperty("dspace.url");

        // if there's no base URL, we are stuck
        if (base == null)
        {
            return;
        }

        StringBuilder rightsString = new StringBuilder();
        List<Bundle> bundles = item.getBundles();
        for (Bundle bundle : bundles)
        {
            if (Constants.LICENSE_BUNDLE_NAME.equals(bundle.getName()))
            {
                List<Bitstream> bss = bundle.getBitstreams();
                for (Bitstream bs : bss)
                {
                    String url = urlManager.getBitstreamUrl(bs);
                    rightsString.append(url).append(" ");
                }
                break;
            }
        }

        Rights rights = new Rights();
        rights.setContent(rightsString.toString());
        rights.setType(ContentType.TEXT);
        entry.setRights(rights);
    }

    /**
     * Add the summary/abstract from the bibliographic metadata
     *
     */
    protected void addSummary()
    {
        List<MetadataValue> dcv = itemService
                .getMetadataByMetadataString(item, "dc.description.abstract");
        if (dcv != null)
        {
            for (MetadataValue aDcv : dcv)
            {
                Summary summary = new Summary();
                summary.setContent(aDcv.getValue());
                summary.setType(ContentType.TEXT);
                entry.setSummary(summary);
            }
        }
    }

    /**
     * Add the title from the bibliographic metadata
     *
     */
    protected void addTitle()
    {
        List<MetadataValue> dcv = itemService
                .getMetadataByMetadataString(item, "dc.title");
        if (dcv != null)
        {
            for (MetadataValue aDcv : dcv)
            {
                Title title = new Title();
                title.setContent(aDcv.getValue());
                title.setType(ContentType.TEXT);
                entry.setTitle(title);
            }
        }
    }

    /**
     * Add the date that this item was last updated
     *
     */
    protected void addLastUpdatedDate()
    {
        String config = ConfigurationManager
                .getProperty("sword-server", "updated.field");
        List<MetadataValue> dcv = itemService
                .getMetadataByMetadataString(item, config);
        if (dcv != null && dcv.size() == 1)
        {
            DCDate dcd = new DCDate(dcv.get(0).getValue());
            entry.setUpdated(dcd.toString());
        }
    }
}
