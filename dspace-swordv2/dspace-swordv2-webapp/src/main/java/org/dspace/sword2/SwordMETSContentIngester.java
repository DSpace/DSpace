/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import java.io.File;

import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;

import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public class SwordMETSContentIngester extends AbstractSwordContentIngester
{
	/** Log4j logger */
	public static final Logger log = Logger.getLogger(SwordMETSContentIngester.class);


    public DepositResult ingest(Context context, Deposit deposit, DSpaceObject dso, VerboseDescription verboseDescription)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        return this.ingest(context, deposit, dso, verboseDescription, null);
    }

	/* (non-Javadoc)
	 * @see org.dspace.sword.SWORDIngester#ingest(org.dspace.core.Context, org.purl.sword.base.Deposit)
	 */
	public DepositResult ingest(Context context, Deposit deposit, DSpaceObject dso, VerboseDescription verboseDescription, DepositResult result)
			throws DSpaceSwordException, SwordError
	{
        // FIXME: it's not clear how to make the METS ingester work over an existing item
        
		try
		{
			// first, make sure this is the right kind of ingester, and set the collection
			if (!(dso instanceof Collection))
			{
				throw new DSpaceSwordException("Tried to run an ingester on wrong target type");
			}
			Collection collection = (Collection) dso;

			// get deposited file as InputStream
			File depositFile = deposit.getFile();

			// load the plugin manager for the required configuration
			String cfg = ConfigurationManager.getProperty("swordv2-server", "mets-ingester.package-ingester");
			if (cfg == null || "".equals(cfg))
			{
				cfg = "METS";  // default to METS
			}
			verboseDescription.append("Using package manifest format: " + cfg);

			PackageIngester pi = (PackageIngester)PluginManager.getNamedPlugin("swordv2-server", PackageIngester.class, cfg);
			verboseDescription.append("Loaded package ingester: " + pi.getClass().getName());

			// the licence is either in the zip or the mets manifest.  Either way
			// it's none of our business here
			String licence = null;

			// Initialize parameters to packager
			PackageParameters params = new PackageParameters();
			// Force package ingester to respect Collection workflows
			params.setWorkflowEnabled(true);

			// Should restore mode be enabled, i.e. keep existing handle?
			if (ConfigurationManager.getBooleanProperty("swordv2-server", "restore-mode.enable",false))
				params.setRestoreModeEnabled(true);

			// ingest the item from the temp file
			DSpaceObject ingestedObject = pi.ingest(context, collection, depositFile, params, licence);
			if (ingestedObject == null)
			{
				verboseDescription.append("Failed to ingest the package; throwing exception");
				throw new SwordError(DSpaceUriRegistry.UNPACKAGE_FAIL, "METS package ingester failed to unpack package");
			}

			//Verify we have an Item as a result -- SWORD can only ingest Items
			if (!(ingestedObject instanceof Item))
			{
				throw new DSpaceSwordException("DSpace Ingester returned wrong object type -- not an Item result.");
			}
			else
			{
				//otherwise, we have an item, and a workflow should have already been started for it.
				verboseDescription.append("Workflow process started");
			}

			// get reference to item so that we can report on it
			Item installedItem = (Item)ingestedObject;

			// update the item metadata to inclue the current time as
			// the updated date
			this.setUpdatedDate(installedItem, verboseDescription);

			// DSpace ignores the slug value as suggested identifier, but
			// it does store it in the metadata
			this.setSlug(installedItem, deposit.getSlug(), verboseDescription);

			// in order to write these changes, we need to bypass the
			// authorisation briefly, because although the user may be
			// able to add stuff to the repository, they may not have
			// WRITE permissions on the archive.
			boolean ignore = context.ignoreAuthorization();
			context.setIgnoreAuthorization(true);
			installedItem.update();
			context.setIgnoreAuthorization(ignore);

			// for some reason, DSpace will not give you the handle automatically,
			// so we have to look it up
			String handle = HandleManager.findHandle(context, installedItem);

			verboseDescription.append("Ingest successful");
			verboseDescription.append("Item created with internal identifier: " + installedItem.getID());
			if (handle != null)
			{
				verboseDescription.append("Item created with external identifier: " + handle);
			}
			else
			{
				verboseDescription.append("No external identifier available at this stage (item in workflow)");
			}

			DepositResult dr = new DepositResult();
			dr.setItem(installedItem);
			dr.setTreatment(this.getTreatment());

			return dr;
		}
		catch (RuntimeException re)
		{
			log.error("caught exception: ", re);
			throw re;
		}
		catch (Exception e)
		{
			log.error("caught exception: ", e);
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
		return "The package has been deposited into DSpace.  Each file has been unpacked " +
				"and provided with a unique identifier.  The metadata in the manifest has been " +
				"extracted and attached to the DSpace item, which has been provided with " +
				"an identifier leading to an HTML splash page.";
	}
}
