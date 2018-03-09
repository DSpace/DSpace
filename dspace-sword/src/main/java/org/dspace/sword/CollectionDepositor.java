/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.authorize.AuthorizeException;
import org.purl.sword.base.Deposit;

import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.ErrorCodes;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Richard Jones
 *
 * A depositor which can deposit content into a DSpace Collection
 *
 */
public class CollectionDepositor extends Depositor
{
    /** logger */
    private static Logger log = Logger.getLogger(CollectionDepositor.class);

    protected ItemService itemService = ContentServiceFactory.getInstance()
            .getItemService();

    protected BundleService bundleService = ContentServiceFactory.getInstance()
            .getBundleService();

    protected BitstreamService bitstreamService = ContentServiceFactory
            .getInstance().getBitstreamService();

    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory
            .getInstance().getBitstreamFormatService();

    /**
     * The DSpace Collection we are depositing into
     */
    private Collection collection;

    /**
     * Construct a depositor for the given service instance on the
     * given DSpaceObject.  If the DSpaceObject is not an instance of Collection
     * this constructor will throw an Exception
     *
     * @param swordService
     * @param dso
     * @throws DSpaceSWORDException
     */
    public CollectionDepositor(SWORDService swordService, DSpaceObject dso)
            throws DSpaceSWORDException
    {
        super(swordService, dso);

        if (!(dso instanceof Collection))
        {
            throw new DSpaceSWORDException(
                    "You tried to initialise the collection depositor with something" +
                            "other than a collection object");
        }

        this.collection = (Collection) dso;

        log.debug("Created instance of CollectionDepositor");
    }

    /**
     * Perform a deposit, using the supplied SWORD Deposit object.
     *
     * @param deposit
     * @throws SWORDErrorException
     * @throws DSpaceSWORDException
     */
    public DepositResult doDeposit(Deposit deposit)
            throws SWORDErrorException, DSpaceSWORDException
    {
        // get the things out of the service that we need
        Context context = swordService.getContext();
        SWORDConfiguration swordConfig = swordService.getSwordConfig();
        SWORDUrlManager urlManager = swordService.getUrlManager();

        // FIXME: the spec is unclear what to do in this situation.  I'm going
        // the throw a 415 (ERROR_CONTENT) until further notice
        //
        // determine if this is an acceptable file format
        if (!swordConfig
                .isAcceptableContentType(context, deposit.getContentType(),
                        collection))
        {
            log.error("Unacceptable content type detected: " +
                    deposit.getContentType() + " for collection " +
                    collection.getID());
            throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT,
                    "Unacceptable content type in deposit request: " +
                            deposit.getContentType());
        }

        // determine if this is an acceptable packaging type for the deposit
        // if not, we throw a 415 HTTP error (Unsupported Media Type, ERROR_CONTENT)
        if (!swordConfig
                .isSupportedMediaType(deposit.getPackaging(), this.collection))
        {
            log.error("Unacceptable packaging type detected: " +
                    deposit.getPackaging() + "for collection" +
                    collection.getID());
            throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT,
                    "Unacceptable packaging type in deposit request: " +
                            deposit.getPackaging());
        }

        // Obtain the relevant ingester from the factory
        SWORDIngester si = SWORDIngesterFactory
                .getInstance(context, deposit, collection);
        swordService.message("Loaded ingester: " + si.getClass().getName());

        // do the deposit
        DepositResult result = si.ingest(swordService, deposit, collection);
        swordService.message("Archive ingest completed successfully");

        // if there's an item availalble, and we want to keep the original
        // then do that
        try
        {
            if (swordConfig.isKeepOriginal())
            {
                swordService.message(
                        "DSpace will store an original copy of the deposit, " +
                                "as well as ingesting the item into the archive");

                // in order to be allowed to add the file back to the item, we need to ignore authorisations
                // for a moment
                context.turnOffAuthorisationSystem();

                String bundleName = ConfigurationManager
                        .getProperty("sword-server", "bundle.name");
                if (bundleName == null || "".equals(bundleName))
                {
                    bundleName = "SWORD";
                }
                Item item = result.getItem();
                List<Bundle> bundles = item.getBundles();
                Bundle swordBundle = null;
                for (Bundle bundle : bundles)
                {
                    if (bundleName.equals(bundle.getName()))
                    {
                        // we found one
                        swordBundle = bundle;
                        break;
                    }
                }
                if (swordBundle == null)
                {
                    swordBundle = bundleService
                            .create(context, item, bundleName);
                }

                String fn = swordService.getFilename(context, deposit, true);

                Bitstream bitstream;
                FileInputStream fis = null;
                try
                {
                    fis = new FileInputStream(deposit.getFile());
                    bitstream = bitstreamService
                            .create(context, swordBundle, fis);
                }
                finally
                {
                    if (fis != null)
                    {
                        fis.close();
                    }
                }

                bitstream.setName(context, fn);
                bitstream.setDescription(context, "SWORD deposit package");

                BitstreamFormat bf = bitstreamFormatService
                        .findByMIMEType(context, deposit.getContentType());
                if (bf != null)
                {
                    bitstreamService.setFormat(context, bitstream, bf);
                }

                bitstreamService.update(context, bitstream);
                bundleService.update(context, swordBundle);
                itemService.update(context, item);

                swordService.message("Original package stored as " + fn +
                        ", in item bundle " + swordBundle);

                // now reset the context ignore authorisation
                context.restoreAuthSystemState();

                // set the media link for the created item
                result.setMediaLink(urlManager.getMediaLink(bitstream));
            }
            else
            {
                // set the vanilla media link, which doesn't resolve to anything
                result.setMediaLink(urlManager.getBaseMediaLinkUrl());
            }
        }
        catch (SQLException | AuthorizeException | IOException e)
        {
            log.error("caught exception: ", e);
            throw new DSpaceSWORDException(e);
        }

        return result;
    }

    /**
     * Reverse any changes which may have resulted as the consequence of a deposit.
     *
     * This is inteded for use during no-op deposits, and should be called at the
     * end of such a deposit process in order to remove any temporary files and
     * to abort the database connection, so no changes are written.
     *
     * @param result
     * @throws DSpaceSWORDException
     */
    public void undoDeposit(DepositResult result) throws DSpaceSWORDException
    {
        SWORDContext sc = swordService.getSwordContext();

        // abort the context, so no database changes are written
        // uploaded files will be deleted by the cleanup script
        sc.abort();
        swordService.message("Database changes aborted");
    }
}
