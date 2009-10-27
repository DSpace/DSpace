/* ItemDepositor.java
 *
 * Copyright (c) 2007, Aberystwyth University
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 *  - Neither the name of the Centre for Advanced Software and
 *    Intelligent Systems (CASIS) nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.dspace.sword;

import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.dspace.authorize.AuthorizeException;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.ErrorCodes;
import org.apache.log4j.Logger;

import java.util.Date;
import java.io.*;
import java.text.SimpleDateFormat;
import java.sql.SQLException;

public class ItemDepositor extends Depositor
{
	private static Logger log = Logger.getLogger(ItemDepositor.class);

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

		String tempDir = swordConfig.getTempDir();
		String tempFile = tempDir + "/" + swordService.getTempFilename();

		if (swordConfig.isKeepOriginal())
		{
			try
			{
				swordService.message("DSpace will store an original copy of the deposit file, " +
						"as well as attaching it to the item");

				// first, store the temp file
				InputStream is = deposit.getFile();
				FileOutputStream fos = new FileOutputStream(tempFile);
				Utils.copy(is, fos);
				fos.close();
				is.close();

				// now create an input stream from that temp file to ingest
				InputStream fis = new FileInputStream(tempFile);
				deposit.setFile(fis);
			}
			catch (FileNotFoundException e)
			{
				log.error("caught exception: ", e);
				throw new DSpaceSWORDException(e);
			}
			catch (IOException e)
			{
				log.error("caught exception: ", e);
				throw new DSpaceSWORDException(e);
			}
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
				// in order to be allowed to add the file back to the item, we need to ignore authorisations
				// for a moment
				boolean ignoreAuth = context.ignoreAuthorization();
				context.setIgnoreAuthorization(true);

				String bundleName = ConfigurationManager.getProperty("sword.bundle.name");
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

				FileInputStream fis = new FileInputStream(tempFile);
				Bitstream bitstream = swordBundle.createBitstream(fis);
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
				context.setIgnoreAuthorization(ignoreAuth);

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
