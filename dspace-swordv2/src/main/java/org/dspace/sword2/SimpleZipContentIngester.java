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
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class SimpleZipContentIngester extends AbstractSwordContentIngester
{
    protected BundleService bundleService = ContentServiceFactory.getInstance()
            .getBundleService();

    protected BitstreamService bitstreamService = ContentServiceFactory
            .getInstance().getBitstreamService();

    protected WorkspaceItemService workspaceItemService = ContentServiceFactory
            .getInstance().getWorkspaceItemService();

    public DepositResult ingestToCollection(Context context, Deposit deposit,
            Collection collection, VerboseDescription verboseDescription,
            DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException
    {
        try
        {
            // get deposited file as file object
            File depositFile = deposit.getFile();

            // decide whether we have a new item or an existing one
            Item item = null;
            WorkspaceItem wsi = null;
            if (result != null)
            {
                item = result.getItem();
            }
            else
            {
                result = new DepositResult();
            }
            if (item == null)
            {
                // simple zip ingester uses the item template, since there is no native metadata
                wsi = workspaceItemService.create(context, collection, true);
                item = wsi.getItem();
            }

            // get the original bundle
            List<Bundle> bundles = item.getBundles();
            Bundle original = null;
            for (Bundle bundle : bundles)
            {
                if (Constants.CONTENT_BUNDLE_NAME.equals(bundle.getName()))
                {
                    original = bundle;
                    break;
                }
            }
            if (original == null)
            {
                original = bundleService
                        .create(context, item, Constants.CONTENT_BUNDLE_NAME);
            }

            // unzip the file into the bundle
            List<Bitstream> derivedResources = this
                    .unzipToBundle(context, depositFile, original);

            // now we have an item in the workspace, and we need to consider adding some metadata to it,
            // but since the zip file didn't contain anything, what do we do?
            itemService.addMetadata(context, item, "dc", "title", null, null,
                    "Untitled: " + deposit.getFilename());
            itemService
                    .addMetadata(context, item, "dc", "description", null, null,
                            "Zip file deposted by SWORD without accompanying metadata");

            // update the item metadata to inclue the current time as
            // the updated date
            this.setUpdatedDate(context, item, verboseDescription);

            // DSpace ignores the slug value as suggested identifier, but
            // it does store it in the metadata
            this.setSlug(context, item, deposit.getSlug(), verboseDescription);

            // in order to write these changes, we need to bypass the
            // authorisation briefly, because although the user may be
            // able to add stuff to the repository, they may not have
            // WRITE permissions on the archive.
            context.turnOffAuthorisationSystem();
            itemService.update(context, item);
            context.restoreAuthSystemState();

            verboseDescription.append("Ingest successful");
            verboseDescription
                    .append("Item created with internal identifier: " +
                            item.getID());

            result.setItem(item);
            result.setTreatment(this.getTreatment());
            result.setDerivedResources(derivedResources);

            return result;
        }
        catch (AuthorizeException e)
        {
            throw new SwordAuthException(e);
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    private List<Bitstream> unzipToBundle(Context context, File depositFile,
            Bundle target)
            throws DSpaceSwordException, SwordError, SwordAuthException
    {
        try
        {
            // get the zip file into a usable form
            ZipFile zip = new ZipFile(depositFile);

            List<Bitstream> derivedResources = new ArrayList<Bitstream>();
            Enumeration zenum = zip.entries();
            while (zenum.hasMoreElements())
            {
                ZipEntry entry = (ZipEntry) zenum.nextElement();
                InputStream stream = zip.getInputStream(entry);
                Bitstream bs = bitstreamService.create(context, target, stream);
                BitstreamFormat format = this
                        .getFormat(context, entry.getName());
                bs.setFormat(context, format);
                bs.setName(context, entry.getName());
                bitstreamService.update(context, bs);
                derivedResources.add(bs);
            }

            return derivedResources;
        }
        catch (ZipException e)
        {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST,
                    "unable to unzip provided package", e);
        }
        catch (IOException | SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (AuthorizeException e)
        {
            throw new SwordAuthException(e);
        }
    }

    public DepositResult ingestToItem(Context context, Deposit deposit,
            Item item, VerboseDescription verboseDescription,
            DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException
    {
        try
        {
            if (result == null)
            {
                result = new DepositResult();
            }
            result.setItem(item);

            // get deposited file as file object
            File depositFile = deposit.getFile();

            // get the original bundle
            List<Bundle> bundles = item.getBundles();
            Bundle original = null;
            for (Bundle bundle : bundles)
            {
                if (Constants.CONTENT_BUNDLE_NAME.equals(bundle.getName()))
                {
                    original = bundle;
                    break;
                }
            }
            if (original == null)
            {
                original = bundleService
                        .create(context, item, Constants.CONTENT_BUNDLE_NAME);
            }

            // we are now free to go and unpack the new zip into the original bundle
            List<Bitstream> derivedResources = this
                    .unzipToBundle(context, depositFile, original);

            // update the item metadata to inclue the current time as
            // the updated date
            this.setUpdatedDate(context, item, verboseDescription);

            // in order to write these changes, we need to bypass the
            // authorisation briefly, because although the user may be
            // able to add stuff to the repository, they may not have
            // WRITE permissions on the archive.
            context.turnOffAuthorisationSystem();
            itemService.update(context, item);
            context.restoreAuthSystemState();

            verboseDescription.append("Replace successful");

            result.setItem(item);
            result.setTreatment(this.getTreatment());
            result.setDerivedResources(derivedResources);

            return result;
        }
        catch (AuthorizeException e)
        {
            throw new SwordAuthException(e);
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * The human readable description of the treatment this ingester has
     * put the deposit through
     *
     * @return
     * @throws DSpaceSwordException
     */
    private String getTreatment() throws DSpaceSwordException
    {
        return "The package has been ingested and unpacked into the item.  Template metadata for " +
                "the collection has been used, and a default title with the name of the file has " +
                "been set";
    }
}
