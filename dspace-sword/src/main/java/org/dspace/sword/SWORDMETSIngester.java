/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import java.io.File;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.SWORDErrorException;

public class SWORDMETSIngester implements SWORDIngester {
    private SWORDService swordService;

    protected ItemService itemService = ContentServiceFactory.getInstance()
                                                             .getItemService();
    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Log4j logger
     */
    public static final Logger log = LogManager.getLogger(SWORDMETSIngester.class);

    /* (non-Javadoc)
     * @see org.dspace.sword.SWORDIngester#ingest(org.dspace.core.Context, org.purl.sword.base.Deposit)
     */
    @Override
    public DepositResult ingest(SWORDService service, Deposit deposit,
                                DSpaceObject dso)
        throws DSpaceSWORDException, SWORDErrorException {
        try {
            // first, make sure this is the right kind of ingester, and set the collection
            if (!(dso instanceof Collection)) {
                throw new DSpaceSWORDException(
                    "Tried to run an ingester on wrong target type");
            }
            Collection collection = (Collection) dso;

            // now set the sword service
            swordService = service;

            // get the things out of the service that we need
            Context context = swordService.getContext();

            // get deposited file as InputStream
            File depositFile = deposit.getFile();

            // load the plugin manager for the required configuration
            String cfg = configurationService.getProperty("sword-server.mets-ingester.package-ingester");
            if (cfg == null || "".equals(cfg)) {
                cfg = "METS";  // default to METS
            }
            swordService.message("Using package manifest format: " + cfg);

            PackageIngester pi = (PackageIngester) CoreServiceFactory.getInstance().getPluginService()
                                                                     .getNamedPlugin(PackageIngester.class, cfg);
            swordService.message(
                "Loaded package ingester: " + pi.getClass().getName());

            // the licence is either in the zip or the mets manifest.  Either way
            // it's none of our business here
            String licence = null;

            // Initialize parameters to packager
            PackageParameters params = new PackageParameters();

            // Force package ingester to respect Collection workflows
            params.setWorkflowEnabled(true);

            // Should restore mode be enabled, i.e. keep existing handle?
            if (configurationService
                .getBooleanProperty("sword-server.restore-mode.enable",
                                    false)) {
                params.setRestoreModeEnabled(true);
            }

            // Whether or not to use the collection template
            params.setUseCollectionTemplate(configurationService
                                                .getBooleanProperty(
                                                    "mets.default.ingest.useCollectionTemplate",
                                                    false));

            // ingest the item from the temp file
            DSpaceObject ingestedObject = pi
                .ingest(context, collection, depositFile, params, licence);
            if (ingestedObject == null) {
                swordService.message(
                    "Failed to ingest the package; throwing exception");
                throw new SWORDErrorException(
                    DSpaceSWORDErrorCodes.UNPACKAGE_FAIL,
                    "METS package ingester failed to unpack package");
            }

            //Verify we have an Item as a result -- SWORD can only ingest Items
            if (!(ingestedObject instanceof Item)) {
                throw new DSpaceSWORDException(
                    "DSpace Ingester returned wrong object type -- not an Item result.");
            } else {
                //otherwise, we have an item, and a workflow should have already been started for it.
                swordService.message("Workflow process started");
            }

            // get reference to item so that we can report on it
            Item installedItem = (Item) ingestedObject;

            // update the item metadata to include the current time as
            // the updated date
            this.setUpdatedDate(context, installedItem);

            // DSpace ignores the slug value as suggested identifier, but
            // it does store it in the metadata
            this.setSlug(installedItem, deposit.getSlug());

            // in order to write these changes, we need to bypass the
            // authorisation briefly, because although the user may be
            // able to add stuff to the repository, they may not have
            // WRITE permissions on the archive.
            context.turnOffAuthorisationSystem();
            itemService.update(context, installedItem);
            context.restoreAuthSystemState();

            // for some reason, DSpace will not give you the handle automatically,
            // so we have to look it up
            HandleService handleService = HandleServiceFactory.getInstance()
                                                              .getHandleService();
            String handle = handleService.findHandle(context, installedItem);

            swordService.message("Ingest successful");
            swordService.message("Item created with internal identifier: " +
                                     installedItem.getID());
            if (handle != null) {
                swordService.message(
                    "Item created with external identifier: " + handle);
            } else {
                swordService.message(
                    "No external identifier available at this stage (item in workflow)");
            }

            DepositResult dr = new DepositResult();
            dr.setItem(installedItem);
            dr.setHandle(handle);
            dr.setTreatment(this.getTreatment());

            return dr;
        } catch (RuntimeException re) {
            log.error("caught exception: ", re);
            throw re;
        } catch (Exception e) {
            log.error("caught exception: ", e);
            throw new DSpaceSWORDException(e);
        }
    }

    /**
     * Add the current date to the item metadata.  This looks up
     * the field in which to store this metadata in the configuration
     * sword.updated.field
     *
     * @param context
     * @param item
     * @throws DSpaceSWORDException
     */
    private void setUpdatedDate(Context context, Item item)
        throws DSpaceSWORDException {
        String field = configurationService
            .getProperty("sword-server.updated.field");
        if (field == null || "".equals(field)) {
            throw new DSpaceSWORDException(
                "No configuration, or configuration is invalid for: sword.updated.field");
        }

        MetadataFieldInfo dc = this.configToDC(field, null);
        try {
            itemService.clearMetadata(context, item, dc.schema, dc.element,
                                      dc.qualifier, Item.ANY);
            DCDate date = new DCDate(ZonedDateTime.now(ZoneOffset.UTC));
            itemService.addMetadata(context, item, dc.schema, dc.element,
                                    dc.qualifier, null, date.toString());
        } catch (SQLException e) {
            log.error("Caught exception: ", e);
            throw new DSpaceSWORDException(e);
        }

        swordService.message(
            "Updated date added to response from item metadata where available");
    }

    /**
     * Store the given slug value (which is used for suggested identifiers,
     * and which DSpace ignores) in the item metadata.  This looks up the
     * field in which to store this metadata in the configuration
     * sword.slug.field
     *
     * @param item
     * @param slugVal
     * @throws DSpaceSWORDException
     */
    private void setSlug(Item item, String slugVal)
        throws DSpaceSWORDException {
        // if there isn't a slug value, don't set it
        if (slugVal == null) {
            return;
        }

        String field = configurationService
            .getProperty("sword-server.slug.field");
        if (field == null || "".equals(field)) {
            throw new DSpaceSWORDException(
                "No configuration, or configuration is invalid for: sword.slug.field");
        }

        MetadataFieldInfo mfi = this.configToDC(field, null);
        try {
            itemService
                .clearMetadata(swordService.getContext(), item, mfi.schema,
                               mfi.element, mfi.qualifier, Item.ANY);
            itemService.addMetadata(swordService.getContext(), item, mfi.schema,
                                    mfi.element, mfi.qualifier, null, slugVal);
        } catch (SQLException e) {
            log.error("Caught exception: ", e);
            throw new DSpaceSWORDException(e);
        }

        swordService.message("Slug value set in response where available");
    }

    /**
     * utility method to turn given metadata fields of the form
     * schema.element.qualifier into Metadatum objects which can be
     * used to access metadata in items.
     *
     * The def parameter should be null, * or "" depending on how
     * you intend to use the Metadatum object
     *
     * @param config
     * @param def
     * @return
     */
    private MetadataFieldInfo configToDC(String config, String def) {
        MetadataFieldInfo mfi = new MetadataFieldInfo();
        mfi.schema = def;
        mfi.element = def;
        mfi.qualifier = def;

        StringTokenizer stz = new StringTokenizer(config, ".");
        mfi.schema = stz.nextToken();
        mfi.element = stz.nextToken();
        if (stz.hasMoreTokens()) {
            mfi.qualifier = stz.nextToken();
        }

        return mfi;
    }

    /**
     * The human readable description of the treatment this ingester has
     * put the deposit through
     *
     * @return
     * @throws DSpaceSWORDException
     */
    private String getTreatment() throws DSpaceSWORDException {
        return "The package has been deposited into DSpace.  Each file has been unpacked " +
            "and provided with a unique identifier.  The metadata in the manifest has been " +
            "extracted and attached to the DSpace item, which has been provided with " +
            "an identifier leading to an HTML splash page.";
    }

    private class MetadataFieldInfo {
        private String schema;

        private String element;

        private String qualifier;
    }
}
