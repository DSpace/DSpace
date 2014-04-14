/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.abdera.i18n.iri.IRI;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.apache.log4j.Logger;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Richard Jones
 *
 * Class to generate an ATOM Entry document for a DSpace Item
 */
public class ReceiptGenerator
{
	/** logger */
	private static Logger log = Logger.getLogger(ReceiptGenerator.class);

	protected DepositReceipt createFileReceipt(Context context, DepositResult result, SwordConfigurationDSpace config)
			throws DSpaceSwordException, SwordError, SwordServerException
	{
		SwordUrlManager urlManager = config.getUrlManager(context, config);
		DepositReceipt receipt = new DepositReceipt();

		receipt.setLocation(new IRI(urlManager.getActionableBitstreamUrl(result.getOriginalDeposit())));
		receipt.setEmpty(true);

		return receipt;
	}

    protected DepositReceipt createMediaResourceReceipt(Context context, Item item, SwordConfigurationDSpace config)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        SwordUrlManager urlManager = config.getUrlManager(context, config);
        DepositReceipt receipt = new DepositReceipt();
        receipt.setLocation(urlManager.getContentUrl(item));
        return receipt;
    }

	protected DepositReceipt createReceipt(Context context, DepositResult result, SwordConfigurationDSpace config)
			throws DSpaceSwordException, SwordError, SwordServerException
	{
		return this.createReceipt(context, result, config, false);
	}

	/**
	 * Construct the entry
	 *
	 * @throws DSpaceSwordException
	 */
	protected DepositReceipt createReceipt(Context context, DepositResult result, SwordConfigurationDSpace config, boolean mediaResourceLocation)
			throws DSpaceSwordException, SwordError, SwordServerException
	{
		SwordUrlManager urlManager = config.getUrlManager(context, config);
		DepositReceipt receipt = new DepositReceipt();

		receipt.setAtomStatementURI(urlManager.getAtomStatementUri(result.getItem()));
		receipt.setOREStatementURI(urlManager.getOreStatementUri(result.getItem()));
		receipt.setEditIRI(urlManager.getEditIRI(result.getItem()));
		receipt.setSplashUri(urlManager.getSplashUrl(result.getItem()));
		receipt.setSwordEditIRI(urlManager.getEditIRI(result.getItem()));
		receipt.setTreatment(result.getTreatment());
		receipt.setContent(urlManager.getContentUrl(result.getItem()), "application/zip");
        receipt.addEditMediaIRI(urlManager.getContentUrl(result.getItem()), "application/zip");
        receipt.setMediaFeedIRI(urlManager.getMediaFeedUrl(result.getItem()));
        receipt.setLastModified(result.getItem().getLastModified());

		if (mediaResourceLocation)
		{
			receipt.setLocation(urlManager.getContentUrl(result.getItem()));
		}
		else
		{
			receipt.setLocation(urlManager.getEditIRI(result.getItem()));
		}

		Bitstream od = result.getOriginalDeposit();
		if (od != null)
		{
			// note here that we don't provide an actionable url
			receipt.setOriginalDeposit(urlManager.getActionableBitstreamUrl(od), od.getFormat().getMIMEType());
		}

		Map<String, String> derived = new HashMap<String, String>();
		List<Bitstream> drs = result.getDerivedResources();
		if (drs != null)
		{
			for (Bitstream bs : result.getDerivedResources())
			{
				// here we provide actionable urls for the parts of the resource
				derived.put(urlManager.getActionableBitstreamUrl(bs), bs.getFormat().getMIMEType());
			}
		}
        receipt.setDerivedResources(derived);

		// add the category information to the sword entry
		this.addCategories(result, receipt);

		// add the publish date
		this.addPublishDate(result, receipt);

        // add the item's metadata
        SwordEntryDisseminator disseminator = SwordDisseminatorFactory.getEntryInstance();
        disseminator.disseminate(context, result.getItem(), receipt);
        
		try
		{
			StringBuilder rightsString = new StringBuilder();
			Bundle[] bundles = result.getItem().getBundles("LICENSE");
			for (int i = 0; i < bundles.length; i++)
			{
				Bitstream[] bss = bundles[i].getBitstreams();
				for (int j = 0; j < bss.length; j++)
				{
					String url = urlManager.getBitstreamUrl(bss[j]);
					rightsString.append(url + " ");
				}
			}
			receipt.getWrappedEntry().setRights(rightsString.toString());
		}
		catch (SQLException e)
		{
			throw new DSpaceSwordException(e);
		}

		// add the date on which the entry was last updated
		this.addLastUpdatedDate(result, receipt);

		// do this from configuration
		receipt.setPackaging(config.getDisseminatePackaging());

		return receipt;
	}

	/**
	 * Construct the entry
	 *
	 * @throws DSpaceSwordException
	 */
	protected DepositReceipt createReceipt(Context context, Item item, SwordConfigurationDSpace config)
			throws DSpaceSwordException, SwordError, SwordServerException
	{
		SwordUrlManager urlManager = config.getUrlManager(context, config);
		DepositReceipt receipt = new DepositReceipt();

		receipt.setAtomStatementURI(urlManager.getAtomStatementUri(item));
		receipt.setOREStatementURI(urlManager.getOreStatementUri(item));
		receipt.setEditIRI(urlManager.getEditIRI(item));
		receipt.setLocation(urlManager.getEditIRI(item));
		receipt.setSplashUri(urlManager.getSplashUrl(item));
		receipt.setSwordEditIRI(urlManager.getEditIRI(item));
		receipt.setContent(urlManager.getContentUrl(item), "application/zip");
        receipt.addEditMediaIRI(urlManager.getContentUrl(item), "application/zip");
        receipt.setMediaFeedIRI(urlManager.getMediaFeedUrl(item));
        receipt.setLastModified(item.getLastModified());

		// add the category information to the sword entry
		this.addCategories(item, receipt);

		// add the publish date
		this.addPublishDate(item, receipt);

        // add the item's metadata
        SwordEntryDisseminator disseminator = SwordDisseminatorFactory.getEntryInstance();
        disseminator.disseminate(context, item, receipt);

		try
		{
			StringBuilder rightsString = new StringBuilder();
			Bundle[] bundles = item.getBundles("LICENSE");
			for (int i = 0; i < bundles.length; i++)
			{
				Bitstream[] bss = bundles[i].getBitstreams();
				for (int j = 0; j < bss.length; j++)
				{
					String url = urlManager.getBitstreamUrl(bss[j]);
					rightsString.append(url + " ");
				}
			}
			receipt.getWrappedEntry().setRights(rightsString.toString());
		}
		catch (SQLException e)
		{
			throw new DSpaceSwordException(e);
		}

		// add the date on which the entry was last updated
		this.addLastUpdatedDate(item, receipt);

		// do this from configuration
		receipt.setPackaging(config.getDisseminatePackaging());

		return receipt;
	}

	/**
	 * Add all the subject classifications from the bibliographic
	 * metadata.
	 *
	 */
	protected void addCategories(DepositResult result, DepositReceipt receipt)
	{
		DCValue[] dcv = result.getItem().getMetadata("dc.subject.*");
		if (dcv != null)
		{
			for (int i = 0; i < dcv.length; i++)
			{
				receipt.getWrappedEntry().addCategory(UriRegistry.DC_NAMESPACE, dcv[i].value, dcv[i].value);
			}
		}
	}

	protected void addCategories(Item item, DepositReceipt receipt)
	{
		DCValue[] dcv = item.getMetadata("dc.subject.*");
		if (dcv != null)
		{
			for (int i = 0; i < dcv.length; i++)
			{
				receipt.getWrappedEntry().addCategory(UriRegistry.DC_NAMESPACE, dcv[i].value, dcv[i].value);
			}
		}
	}

	/**
	 * Add the date of publication from the bibliographic metadata
	 *
	 */
	protected void addPublishDate(DepositResult result, DepositReceipt receipt)
	{
		DCValue[] dcv = result.getItem().getMetadata("dc.date.issued");
		if (dcv != null && dcv.length == 1)
        {
            try
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date published = sdf.parse(dcv[0].value);
                receipt.getWrappedEntry().setPublished(published);
            }
            catch (ParseException e)
            {
                // we tried, but never mind
                return;
            }
        }
	}

	protected void addPublishDate(Item item, DepositReceipt receipt)
	{
		DCValue[] dcv = item.getMetadata("dc.date.issued");
		if (dcv != null && dcv.length == 1)
        {
            try
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date published = sdf.parse(dcv[0].value);
                receipt.getWrappedEntry().setPublished(published);
            }
            catch (ParseException e)
            {
                // we tried, but never mind
                return;
            }
        }
	}

	/**
	 * Add the date that this item was last updated
	 *
	 */
	protected void addLastUpdatedDate(DepositResult result, DepositReceipt receipt)
	{
		String config = ConfigurationManager.getProperty("swordv2-server", "updated.field");
		DCValue[] dcv = result.getItem().getMetadata(config);
		if (dcv != null && dcv.length == 1)
        {
            try
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date updated = sdf.parse(dcv[0].value);
                receipt.getWrappedEntry().setUpdated(updated);
            }
            catch (ParseException e)
            {
                // we tried, but never mind
                return;
            }
        }
	}

	protected void addLastUpdatedDate(Item item, DepositReceipt receipt)
	{
		String config = ConfigurationManager.getProperty("swordv2-server", "updated.field");
		DCValue[] dcv = item.getMetadata(config);
		if (dcv != null && dcv.length == 1)
        {
            try
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date updated = sdf.parse(dcv[0].value);
                receipt.getWrappedEntry().setUpdated(updated);
            }
            catch (ParseException e)
            {
                // we tried, but never mind
                return;
            }
        }
	}
}
