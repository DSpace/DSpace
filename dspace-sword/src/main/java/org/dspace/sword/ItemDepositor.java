/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.authorize.AuthorizeException;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.ErrorCodes;

import java.io.*;
import java.sql.SQLException;

public class ItemDepositor extends Depositor
{
	private Item item;

	public ItemDepositor(SWORDService swordService, DSpaceObject dso)
			throws DSpaceSWORDException
	{
		super(swordService, dso);

		if (!(dso instanceof Item))
		{
			throw new DSpaceSWORDException("You tried to initialise the item depositor with something" +
					"other than an item object");
		}

		this.item = (Item) dso;
	}

	public DepositResult doDeposit(Deposit deposit) throws SWORDErrorException, DSpaceSWORDException
	{
		// get the things out of the service that we need
		Context context = swordService.getContext();
		SWORDConfiguration swordConfig = swordService.getSwordConfig();
		SWORDUrlManager urlManager = swordService.getUrlManager();

		// FIXME: the spec is unclear what to do in this situation.  I'm going
		// the throw a 415 (ERROR_CONTENT) until further notice
		//
		// determine if this is an acceptable file format
		if (!swordConfig.isAcceptableContentType(context, deposit.getContentType(), item))
		{
			throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT,
					"Unacceptable content type in deposit request: " + deposit.getContentType());
		}

		// determine if this is an acceptable packaging type for the deposit
		// if not, we throw a 415 HTTP error (Unsupported Media Type, ERROR_CONTENT)
		if (!swordConfig.isSupportedMediaType(deposit.getPackaging(), this.item))
		{
			throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT,
					"Unacceptable packaging type in deposit request: " + deposit.getPackaging());
		}

		// Obtain the relevant ingester from the factory
		SWORDIngester si = SWORDIngesterFactory.getInstance(context, deposit, item);
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
                                swordService.message("DSpace will store an original copy of the deposit file, " +
						"as well as attaching it to the item");

				// in order to be allowed to add the file back to the item, we need to ignore authorisations
				// for a moment				
				context.turnOffAuthorisationSystem();

				String bundleName = ConfigurationManager.getProperty("sword-server", "bundle.name");
				if (bundleName == null || "".equals(bundleName))
				{
					bundleName = "SWORD";
				}

				Bundle[] bundles = item.getBundles(bundleName);
				Bundle swordBundle = null;
				if (bundles.length > 0)
				{
					swordBundle = bundles[0];
				}
				if (swordBundle == null)
				{
					swordBundle = item.createBundle(bundleName);
				}

				String fn = swordService.getFilename(context, deposit, true);

                Bitstream bitstream;
				FileInputStream fis = null;
                try
                {
                    fis = new FileInputStream(deposit.getFile());
                    bitstream = swordBundle.createBitstream(fis);
                }
                finally
                {
                    if (fis != null)
                    {
                        fis.close();
                    }
                }
				bitstream.setName(fn);
				bitstream.setDescription("Original file deposited via SWORD");

				BitstreamFormat bf = BitstreamFormat.findByMIMEType(context, deposit.getContentType());
				if (bf != null)
				{
					bitstream.setFormat(bf);
				}

				bitstream.update();
				swordBundle.update();
				item.update();

				swordService.message("Original package stored as " + fn + ", in item bundle " + swordBundle);

				// now reset the context ignore authorisation
				context.restoreAuthSystemState();

				// set the media link for the created item
				result.setMediaLink(urlManager.getMediaLink(bitstream));
			}
			else
			{
				// set the media link for the created item using the archived version (since it's just a file)
				result.setMediaLink(urlManager.getMediaLink(result.getBitstream()));
			}
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
		catch (AuthorizeException e)
		{
			throw new DSpaceSWORDException(e);
		}
		catch (FileNotFoundException e)
		{
			throw new DSpaceSWORDException(e);
		}
		catch (IOException e)
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

			// obtain the bitstream's owning bundles and remove the bitstream
			// from them.  This will ensure that the bitstream is physically
			// removed from the disk.
			Bitstream bs = result.getBitstream();
			Bundle[] bundles = bs.getBundles();
			for (int i = 0; i < bundles.length; i++)
			{
				bundles[i].removeBitstream(bs);
				bundles[i].update();
			}

			swordService.message("Removing temporary files from disk");

			// abort the context, so no database changes are written
			sc.abort();
			swordService.message("Database changes aborted");
		}
		catch (IOException e)
		{
			//log.error("caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
		catch (AuthorizeException e)
		{
			//log.error("authentication problem; caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
		catch (SQLException e)
		{
			//log.error("caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}
}
