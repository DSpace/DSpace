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
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageUtils;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;

import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public class SwordMETSContentIngester extends AbstractSwordContentIngester
{
    /** Log4j logger */
    public static final Logger log = Logger
            .getLogger(SwordMETSContentIngester.class);

    protected WorkspaceItemService workspaceItemService = ContentServiceFactory
            .getInstance().getWorkspaceItemService();

    protected CollectionService collectionService = ContentServiceFactory
            .getInstance().getCollectionService();

    protected HandleService handleService = HandleServiceFactory.getInstance()
            .getHandleService();

    public DepositResult ingest(Context context, Deposit deposit,
            DSpaceObject dso, VerboseDescription verboseDescription)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        return this.ingest(context, deposit, dso, verboseDescription, null);
    }

    @Override
    public DepositResult ingestToCollection(Context context, Deposit deposit,
            Collection collection, VerboseDescription verboseDescription,
            DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        try
        {
            // if we are actuall given an item in the deposit result of a previous operation
            // then we do an ingestToItem
            if (result != null)
            {
                Item item = result.getItem();
                return this.ingestToItem(context, deposit, item,
                        verboseDescription, result);
            }

            // otherwise, go on and do a create ...

            // create the an item in the workspace.  This is necessary, because later
            // we are going to ask a package ingester to /replace/ this item, which gives
            // us finer control over the workflow state of the item, whereas asking
            // the ingester to /create/ this item causes it to be injected into the workflow,
            // irrespective of the In-Progress header provided by the depositor
            WorkspaceItem wsi = workspaceItemService
                    .create(context, collection, true);
            Item item = wsi.getItem();

            // need to add a licence file, otherwise the METS replace function raises a NullPointerException
            String licence = collectionService.getLicense(collection);
            if (PackageUtils.findDepositLicense(context, item) == null)
            {
                PackageUtils
                        .addDepositLicense(context, licence, item, collection);
            }

            // get deposited file as InputStream
            File depositFile = deposit.getFile();

            // load the plugin manager for the required configuration
            String cfg = ConfigurationManager.getProperty("swordv2-server",
                    "mets-ingester.package-ingester");
            if (cfg == null || "".equals(cfg))
            {
                cfg = "METS";  // default to METS
            }
            verboseDescription.append("Using package manifest format: " + cfg);

            PackageIngester pi = (PackageIngester) CoreServiceFactory.getInstance().getPluginService()
                    .getNamedPlugin(PackageIngester.class, cfg);
            verboseDescription.append("Loaded package ingester: " +
                    pi.getClass().getName());

            // Initialize parameters to packager
            PackageParameters params = new PackageParameters();

            // Force package ingester to respect Collection workflows
            params.setWorkflowEnabled(true);

            // Should restore mode be enabled, i.e. keep existing handle?
            if (ConfigurationManager.getBooleanProperty(
                "swordv2-server", "restore-mode.enable", false))
            {
                params.setRestoreModeEnabled(true);
            }

            // Whether or not to use the collection template
            params.setUseCollectionTemplate(ConfigurationManager
                    .getBooleanProperty(
                            "mets.default.ingest.useCollectionTemplate",
                            false));

            // ingest the item from the temp file
            DSpaceObject ingestedObject = pi
                    .replace(context, item, depositFile, params);
            if (ingestedObject == null)
            {
                verboseDescription
                        .append("Failed to ingest the package; throwing exception");
                throw new SwordError(DSpaceUriRegistry.UNPACKAGE_FAIL,
                        "METS package ingester failed to unpack package");
            }

            // Verify we have an Item as a result
            if (!(ingestedObject instanceof Item))
            {
                throw new DSpaceSwordException(
                        "DSpace Ingester returned wrong object type -- not an Item result.");
            }
            else
            {
                //otherwise, we have an item, and a workflow should have already been started for it.
                verboseDescription.append("Workflow process started");
            }

            // get reference to item so that we can report on it
            Item installedItem = (Item) ingestedObject;

            // update the item metadata to inclue the current time as
            // the updated date
            this.setUpdatedDate(context, installedItem, verboseDescription);

            // DSpace ignores the slug value as suggested identifier, but
            // it does store it in the metadata
            this.setSlug(context, installedItem, deposit.getSlug(),
                    verboseDescription);

            // in order to write these changes, we need to bypass the
            // authorisation briefly, because although the user may be
            // able to add stuff to the repository, they may not have
            // WRITE permissions on the archive.
            context.turnOffAuthorisationSystem();
            itemService.update(context, installedItem);
            context.restoreAuthSystemState();

            // for some reason, DSpace will not give you the handle automatically,
            // so we have to look it up
            String handle = handleService.findHandle(context, installedItem);

            verboseDescription.append("Ingest successful");
            verboseDescription
                    .append("Item created with internal identifier: " +
                            installedItem.getID());
            if (handle != null)
            {
                verboseDescription
                        .append("Item created with external identifier: " +
                                handle);
            }
            else
            {
                verboseDescription
                        .append("No external identifier available at this stage (item in workflow)");
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

    @Override
    public DepositResult ingestToItem(Context context, Deposit deposit,
            Item item, VerboseDescription verboseDescription,
            DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        if (result == null)
        {
            result = new DepositResult();
        }

        try
        {
            // get deposited file as InputStream
            File depositFile = deposit.getFile();

            // load the plugin manager for the required configuration
            String cfg = ConfigurationManager.getProperty(
                "swordv2-server", "mets-ingester.package-ingester");

            if (cfg == null || "".equals(cfg))
            {
                cfg = "METS";  // default to METS
            }
            verboseDescription.append("Using package manifest format: " + cfg);

            PackageIngester pi = (PackageIngester) CoreServiceFactory.getInstance().getPluginService()
                    .getNamedPlugin(PackageIngester.class, cfg);
            verboseDescription.append("Loaded package ingester: " +
                    pi.getClass().getName());

            // Initialize parameters to packager
            PackageParameters params = new PackageParameters();

            // Force package ingester to respect Collection workflows
            params.setWorkflowEnabled(true);

            // Should restore mode be enabled, i.e. keep existing handle?
            if (ConfigurationManager.getBooleanProperty(
                "swordv2-server", "restore-mode.enable", false))
            {
                params.setRestoreModeEnabled(true);
            }

            // Whether or not to use the collection template
            params.setUseCollectionTemplate(ConfigurationManager
                    .getBooleanProperty(
                            "mets.default.ingest.useCollectionTemplate",
                            false));

            // ingest the item from the temp file
            DSpaceObject ingestedObject = pi
                    .replace(context, item, depositFile, params);
            if (ingestedObject == null)
            {
                verboseDescription
                        .append("Failed to replace the package; throwing exception");
                throw new SwordError(DSpaceUriRegistry.UNPACKAGE_FAIL,
                        "METS package ingester failed to unpack package");
            }

            // Verify we have an Item as a result
            if (!(ingestedObject instanceof Item))
            {
                throw new DSpaceSwordException(
                        "DSpace Ingester returned wrong object type -- not an Item result.");
            }

            // get reference to item so that we can report on it
            Item installedItem = (Item) ingestedObject;

            // update the item metadata to inclue the current time as
            // the updated date
            this.setUpdatedDate(context, installedItem, verboseDescription);

            // in order to write these changes, we need to bypass the
            // authorisation briefly, because although the user may be
            // able to add stuff to the repository, they may not have
            // WRITE permissions on the archive.
            context.turnOffAuthorisationSystem();
            itemService.update(context, installedItem);
            context.restoreAuthSystemState();

            // for some reason, DSpace will not give you the handle automatically,
            // so we have to look it up
            verboseDescription.append("Replace successful");

            result.setItem(installedItem);
            result.setTreatment(this.getTreatment());

            return result;
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
