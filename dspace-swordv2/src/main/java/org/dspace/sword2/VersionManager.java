/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class VersionManager
{
    protected ItemService itemService = ContentServiceFactory.getInstance()
            .getItemService();

    protected BundleService bundleService = ContentServiceFactory.getInstance()
            .getBundleService();

    protected BitstreamService bitstreamService = ContentServiceFactory
            .getInstance().getBitstreamService();

    public void removeBundle(Context context, Item item, String name)
            throws SQLException, AuthorizeException, IOException
    {
        boolean keep = ConfigurationManager
                .getBooleanProperty("swordv2-server", "versions.keep");
        Iterator<Bundle> bundles = item.getBundles().iterator();
        while (bundles.hasNext())
        {
            Bundle b = bundles.next();
            if (name.equals(b.getName()))
            {
                bundles.remove();
                this.removeBundle(context, item, b, keep);
            }
        }
    }

    public void removeBundle(Context context, Item item, Bundle source)
            throws SQLException, AuthorizeException, IOException
    {
        boolean keep = ConfigurationManager
                .getBooleanProperty("swordv2-server", "versions.keep");
        this.removeBundle(context, item, source, keep);
    }

    public void removeBundle(Context context, Item item, Bundle source,
            boolean archive)
            throws SQLException, AuthorizeException, IOException
    {
        // archive the bundle contents if desired
        if (archive)
        {
            this.archiveBundle(context, item, source);
        }

        // remove all the bitstreams from the bundle
        Iterator<Bitstream> bitstreams = source.getBitstreams()
                .iterator();
        while (bitstreams.hasNext())
        {
            Bitstream bitstream = bitstreams.next();
            bitstreams.remove();
            bundleService.removeBitstream(context, source,
                    bitstream);
        }

        // delete the bundle itself
        itemService.removeBundle(context, item, source);
    }

    public void removeBitstream(Context context, Item item, Bitstream bitstream)
            throws SQLException, AuthorizeException, IOException
    {
        boolean keep = ConfigurationManager
                .getBooleanProperty("swordv2-server", "versions.keep");
        this.removeBitstream(context, item, bitstream, keep);
    }

    public void removeBitstream(Context context, Item item, Bitstream bitstream,
            boolean keep)
            throws SQLException, AuthorizeException, IOException
    {
        Bundle exempt = null;
        if (keep)
        {
            exempt = this.archiveBitstream(context, item, bitstream);
        }

        Iterator<Bundle> bundles = bitstream.getBundles()
                .iterator();
        while (bundles.hasNext())
        {
            Bundle bundle = bundles.next();
            if (exempt != null &&
                    bundle.getID() != exempt.getID())
            {
                bundles.remove();
                bundleService
                        .removeBitstream(context, bundle,
                                bitstream);
            }
        }

        // there is nowhere in the metadata to say when this file was moved, so we
        // are going to drop it into the description
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String desc = bitstream.getDescription();
        String newDesc = "[Deleted on: " + sdf.format(new Date()) + "] ";
        if (desc != null)
        {
            newDesc += desc;
        }
        bitstream.setDescription(context, newDesc);
        bitstreamService.update(context, bitstream);
    }

    private Bundle archiveBitstream(Context context, Item item,
            Bitstream bitstream)
            throws SQLException, AuthorizeException, IOException
    {
        String swordBundle = ConfigurationManager
                .getProperty("swordv2-server", "bundle.deleted");
        if (swordBundle == null)
        {
            swordBundle = "DELETED";
        }

        List<Bundle> bundles = item.getBundles();
        Bundle archive = null;
        for (Bundle bundle : bundles)
        {
            if (swordBundle.equals(bundle.getName()))
            {
                archive = bundle;
                break;
            }
        }
        if (archive == null)
        {
            archive = bundleService.create(context, item, swordBundle);
        }
        this.archiveBitstream(context, archive, bitstream);
        return archive;
    }

    private void archiveBitstream(Context context, Bundle target,
            Bitstream bitstream)
            throws SQLException, AuthorizeException, IOException
    {
        bundleService.addBitstream(context, target, bitstream);
    }

    private void archiveBundle(Context context, Item item, Bundle source)
            throws SQLException, AuthorizeException, IOException
    {
        // get the datestamped root bundle name
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String oldName = "VER" + sdf.format(new Date());
        oldName = this.getNumberedName(item, oldName, 0);

        Bundle old = bundleService.create(context, item, oldName);
        List<Bitstream> bitstreams = source.getBitstreams();
        for (Bitstream bitstream : bitstreams)
        {
            bundleService
                    .addBitstream(context, old, bitstream);
        }
    }

    private String getNumberedName(Item item, String name, int number)
            throws SQLException
    {
        String nName = name + "." + Integer.toString(number);
        List<Bundle> bundles = item.getBundles();
        for (Bundle bundle : bundles)
        {
            if (nName.equals(bundle.getName()))
            {
                return this.getNumberedName(item, name, number + 1);
            }
        }
        return nName;
    }
}
