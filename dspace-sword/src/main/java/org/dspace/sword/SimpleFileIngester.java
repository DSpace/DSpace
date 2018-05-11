/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import java.io.FileInputStream;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.SWORDErrorException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.authorize.AuthorizeException;

import java.sql.SQLException;
import java.io.IOException;
import java.util.List;

/**
 * @author Richard Jones
 *
 * An implementation of the SWORDIngester interface for ingesting single
 * files into a DSpace Item
 *
 */
public class SimpleFileIngester implements SWORDIngester
{

    protected ItemService itemService = ContentServiceFactory.getInstance()
            .getItemService();

    protected BundleService bundleService = ContentServiceFactory.getInstance()
            .getBundleService();

    protected BitstreamService bitstreamService = ContentServiceFactory
            .getInstance().getBitstreamService();

    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory
            .getInstance().getBitstreamFormatService();

    /**
     * Perform the ingest using the given deposit object onto the specified
     * target DSpace object, using the SWORD service implementation.
     *
     * @param service
     * @param deposit
     * @param target
     * @throws DSpaceSWORDException
     * @throws SWORDErrorException
     */
    public DepositResult ingest(SWORDService service, Deposit deposit,
            DSpaceObject target)
            throws DSpaceSWORDException, SWORDErrorException
    {
        try
        {
            if (!(target instanceof Item))
            {
                throw new DSpaceSWORDException(
                        "SimpleFileIngester can only be loaded for deposit onto DSpace Items");
            }
            Item item = (Item) target;

            // now set the sword service
            SWORDService swordService = service;

            // get the things out of the service that we need
            Context context = swordService.getContext();
            SWORDUrlManager urlManager = swordService.getUrlManager();

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

            Bitstream bs;
            FileInputStream fis = null;

            try
            {
                fis = new FileInputStream(deposit.getFile());
                bs = bitstreamService.create(context, original, fis);
            }
            finally
            {
                if (fis != null)
                {
                    fis.close();
                }
            }

            String fn = swordService.getFilename(context, deposit, false);
            bs.setName(context, fn);

            swordService.message("File created in item with filename " + fn);

            BitstreamFormat bf = bitstreamFormatService
                    .findByMIMEType(context, deposit.getContentType());
            if (bf != null)
            {
                bs.setFormat(context, bf);
            }

            // to do the updates, we need to ignore authorisation in the context
            context.turnOffAuthorisationSystem();

            bitstreamService.update(context, bs);
            bundleService.update(context, original);
            itemService.update(context, item);

            // reset the ignore authorisation
            context.restoreAuthSystemState();

            DepositResult result = new DepositResult();
            result.setHandle(urlManager.getBitstreamUrl(bs));
            result.setTreatment(this.getTreatment());
            result.setBitstream(bs);

            return result;
        }
        catch (SQLException | AuthorizeException | IOException e)
        {
            throw new DSpaceSWORDException(e);
        }
    }

    /**
     * Get the description of the treatment this class provides to the deposit
     *
     * @return the description
     */
    private String getTreatment()
    {
        return "The file has been attached to the specified item";
    }
}
