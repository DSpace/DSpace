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
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class SimpleZipContentIngester extends AbstractSwordContentIngester
{
	public DepositResult ingestToCollection(Context context, Deposit deposit, Collection collection, VerboseDescription verboseDescription, DepositResult result)
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
                wsi = WorkspaceItem.create(context, collection, true);
                item = wsi.getItem();
            }

            // get the original bundle
            Bundle[] originals = item.getBundles("ORIGINAL");
            Bundle bundle = null;
            if (originals.length > 0)
            {
                bundle = originals[0];
            }
            else
            {
                bundle = item.createBundle("ORIGINAL");
            }

			// unzip the file into the bundle
			List<Bitstream> derivedResources = this.unzipToBundle(context, depositFile, bundle);

			// now we have an item in the workspace, and we need to consider adding some metadata to it,
			// but since the zip file didn't contain anything, what do we do?
			item.addMetadata("dc", "title", null, null, "Untitled: " + deposit.getFilename());
		    item.addMetadata("dc", "description", null, null, "Zip file deposted by SWORD without accompanying metadata");

			// update the item metadata to inclue the current time as
			// the updated date
			this.setUpdatedDate(item, verboseDescription);

			// DSpace ignores the slug value as suggested identifier, but
			// it does store it in the metadata
			this.setSlug(item, deposit.getSlug(), verboseDescription);

			// in order to write these changes, we need to bypass the
			// authorisation briefly, because although the user may be
			// able to add stuff to the repository, they may not have
			// WRITE permissions on the archive.
			context.turnOffAuthorisationSystem();
			item.update();
			context.restoreAuthSystemState();

			verboseDescription.append("Ingest successful");
			verboseDescription.append("Item created with internal identifier: " + item.getID());

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
		catch (IOException e)
		{
			throw new DSpaceSwordException(e);
		}
    }

	private List<Bitstream> unzipToBundle(Context context, File depositFile, Bundle target)
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
				Bitstream bs = target.createBitstream(stream);
				BitstreamFormat format = this.getFormat(context, entry.getName());
				bs.setFormat(format);
				bs.setName(entry.getName());
				bs.update();
				derivedResources.add(bs);
			}

			return derivedResources;
		}
		catch (ZipException e)
		{
			throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "unable to unzip provided package", e);
		}
		catch (IOException e)
		{
			throw new DSpaceSwordException(e);
		}
		catch (SQLException e)
		{
			throw new DSpaceSwordException(e);
		}
		catch (AuthorizeException e)
		{
			throw new SwordAuthException(e);
		}
	}

    public DepositResult ingestToItem(Context context, Deposit deposit, Item item, VerboseDescription verboseDescription, DepositResult result)
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
            Bundle[] originals = item.getBundles("ORIGINAL");
            Bundle original = null;
            if (originals.length > 0)
            {
                original = originals[0];
            }
            else
            {
                original = item.createBundle("ORIGINAL");
            }

			// we are now free to go and unpack the new zip into the original bundle
            List<Bitstream> derivedResources = this.unzipToBundle(context, depositFile, original);

			// update the item metadata to inclue the current time as
			// the updated date
			this.setUpdatedDate(item, verboseDescription);

			// in order to write these changes, we need to bypass the
			// authorisation briefly, because although the user may be
			// able to add stuff to the repository, they may not have
			// WRITE permissions on the archive.
			context.turnOffAuthorisationSystem();
			item.update();
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
