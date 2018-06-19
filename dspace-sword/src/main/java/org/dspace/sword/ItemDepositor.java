/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.ErrorCodes;
import org.purl.sword.base.SWORDErrorException;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

public class ItemDepositor extends Depositor
{
    protected ItemService itemService = ContentServiceFactory.getInstance()
            .getItemService();

    protected BundleService bundleService = ContentServiceFactory.getInstance()
            .getBundleService();

    protected BitstreamService bitstreamService = ContentServiceFactory
            .getInstance().getBitstreamService();

    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory
            .getInstance().getBitstreamFormatService();

    private Item item;

    public ItemDepositor(SWORDService swordService, DSpaceObject dso)
            throws DSpaceSWORDException
    {
        super(swordService, dso);

        if (!(dso instanceof Item))
        {
            throw new DSpaceSWORDException(
                    "You tried to initialise the item depositor with something" +
                            "other than an item object");
        }

        this.item = (Item) dso;
    }

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
                        item))
        {
            throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT,
                    "Unacceptable content type in deposit request: " +
                            deposit.getContentType());
        }

        // determine if this is an acceptable packaging type for the deposit
        // if not, we throw a 415 HTTP error (Unsupported Media Type, ERROR_CONTENT)
        if (!swordConfig
                .isSupportedMediaType(deposit.getPackaging(), this.item))
        {
            throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT,
                    "Unacceptable packaging type in deposit request: " +
                            deposit.getPackaging());
        }

        // Obtain the relevant ingester from the factory
        SWORDIngester si = SWORDIngesterFactory
                .getInstance(context, deposit, item);
        swordService.message("Loaded ingester: " + si.getClass().getName());

        // do the deposit
        DepositResult result = si.ingest(swordService, deposit, item);
        swordService.message("Archive ingest completed successfully");

        // if there's an item availalble, and we want to keep the original
        // then do that
        try
        {
            if (swordConfig.isKeepOriginal())
            {
                swordService.message(
                        "DSpace will store an original copy of the deposit file, " +
                                "as well as attaching it to the item");

                // in order to be allowed to add the file back to the item, we need to ignore authorisations
                // for a moment
                context.turnOffAuthorisationSystem();

                String bundleName = ConfigurationManager
                        .getProperty("sword-server", "bundle.name");
                if (StringUtils.isBlank(bundleName))
                {
                    bundleName = "SWORD";
                }

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
                bitstream.setDescription(context,
                        "Original file deposited via SWORD");

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
                // set the media link for the created item using the archived version (since it's just a file)
                result.setMediaLink(
                        urlManager.getMediaLink(result.getBitstream()));
            }
        }
        catch (SQLException | AuthorizeException | IOException e)
        {
            throw new DSpaceSWORDException(e);
        }

        return result;
    }

    public void undoDeposit(DepositResult result) throws DSpaceSWORDException
    {
        try
        {
            SWORDContext sc = swordService.getSwordContext();
            BundleService bundleService = ContentServiceFactory.getInstance()
                    .getBundleService();

            // obtain the bitstream's owning bundles and remove the bitstream
            // from them.  This will ensure that the bitstream is physically
            // removed from the disk.
            Bitstream bs = result.getBitstream();
            Iterator<Bundle> bundles = bs.getBundles().iterator();
            while (bundles.hasNext())
            {
                Bundle bundle = bundles.next();
                bundles.remove();
                bundleService.removeBitstream(sc.getContext(), bundle, bs);
                bundleService.update(sc.getContext(), bundle);
            }

            swordService.message("Removing temporary files from disk");

            // abort the context, so no database changes are written
            sc.abort();
            swordService.message("Database changes aborted");
        }
        catch (IOException | AuthorizeException | SQLException e)
        {
            throw new DSpaceSWORDException(e);
        }
    }
}
