/* CollectionDepositor.java
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

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.dspace.authorize.AuthorizeException;
import org.purl.sword.base.Deposit;

import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.ErrorCodes;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

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
			throw new DSpaceSWORDException("You tried to initialise the collection depositor with something" +
					"other than a collection object");
		}

		this.collection = (Collection) dso;

		log.debug("Created instance of CollectionDepositor");
	}

	/**
	 * Perform a deposit, using the supplied SWORD Deposit object
	 *
	 * @param deposit
	 * @return
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
		if (!swordConfig.isAcceptableContentType(context, deposit.getContentType(), collection))
		{
			log.error("Unacceptable content type detected: " + deposit.getContentType() + " for collection " + collection.getID());
			throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT,
					"Unacceptable content type in deposit request: " + deposit.getContentType());
		}

		// determine if this is an acceptable packaging type for the deposit
		// if not, we throw a 415 HTTP error (Unsupported Media Type, ERROR_CONTENT)
		if (!swordConfig.isSupportedMediaType(deposit.getPackaging(), this.collection))
		{
			log.error("Unacceptable packaging type detected: " + deposit.getPackaging() + "for collection" + collection.getID());
			throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT,
					"Unacceptable packaging type in deposit request: " + deposit.getPackaging());
		}

		String tempDir = swordConfig.getTempDir();
		String tempFile = tempDir + "/" + swordService.getTempFilename();
		log.debug("Storing temporary file at " + tempFile);

		if (swordConfig.isKeepOriginal())
		{
			try
			{
				swordService.message("DSpace will store an original copy of the deposit, " +
						"as well as ingesting the item into the archive");

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
		SWORDIngester si = SWORDIngesterFactory.getInstance(context, deposit, collection);
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
				// in order to be allowed to add the file back to the item, we need to ignore authorisations
				// for a moment
				boolean ignoreAuth = context.ignoreAuthorization();
				context.setIgnoreAuthorization(true);

				String bundleName = ConfigurationManager.getProperty("sword.bundle.name");
				if (bundleName == null || "".equals(bundleName))
				{
					bundleName = "SWORD";
				}
				Item item = result.getItem();
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
				bitstream.setDescription("SWORD deposit package");

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
				// set the vanilla media link, which doesn't resolve to anything
				result.setMediaLink(urlManager.getBaseMediaLinkUrl());
			}
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
		catch (AuthorizeException e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException(e);
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
