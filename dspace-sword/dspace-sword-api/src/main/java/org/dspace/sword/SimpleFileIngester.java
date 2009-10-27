/* SimpleFileIngester.java
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

import org.purl.sword.base.Deposit;
import org.purl.sword.base.SWORDErrorException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.authorize.AuthorizeException;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.io.InputStream;
import java.io.IOException;

/**
 * @author Richard Jones
 *
 * An implementation of the SWORDIngester interface for ingesting single
 * files into a DSpace Item
 *
 */
public class SimpleFileIngester implements SWORDIngester
{
	/** logger */
	private static Logger log = Logger.getLogger(SimpleFileIngester.class);

	/** sword service implementation */
	private SWORDService swordService;

	/**
	 * perform the ingest using the given deposit object onto the specified
	 * target dspace object, using the sword service implementation
	 *
	 * @param service
	 * @param deposit
	 * @param target
	 * @return
	 * @throws DSpaceSWORDException
	 * @throws SWORDErrorException
	 */
	public DepositResult ingest(SWORDService service, Deposit deposit, DSpaceObject target)
			throws DSpaceSWORDException, SWORDErrorException
	{
		try
		{
			if (!(target instanceof Item))
			{
				throw new DSpaceSWORDException("SimpleFileIngester can only be loaded for deposit onto DSpace Items");
			}
			Item item = (Item) target;

			// now set the sword service
			swordService = service;

			// get the things out of the service that we need
			Context context = swordService.getContext();
			SWORDUrlManager urlManager = swordService.getUrlManager();

			Bundle[] bundles = item.getBundles("ORIGINAL");
			Bundle original;
			if (bundles.length > 0)
			{
				original = bundles[0];
			}
			else
			{
				original = item.createBundle("ORIGINAL");
			}

			InputStream is = deposit.getFile();
			Bitstream bs = original.createBitstream(is);

			String fn = swordService.getFilename(context, deposit, false);
			bs.setName(fn);

			swordService.message("File created in item with filename " + fn);

			BitstreamFormat bf = BitstreamFormat.findByMIMEType(context, deposit.getContentType());
			if (bf != null)
			{
				bs.setFormat(bf);
			}

			// to do the updates, we need to ignore authorisation in the context
			boolean ignoreAuth = context.ignoreAuthorization();
			context.setIgnoreAuthorization(true);

			bs.update();
			original.update();
			item.update();

			// reset the ignore authorisation
			context.setIgnoreAuthorization(ignoreAuth);

			DepositResult result = new DepositResult();
			result.setHandle(urlManager.getBitstreamUrl(bs));
			result.setTreatment(this.getTreatment());
			result.setBitstream(bs);

			return result;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
		catch (AuthorizeException e)
		{
			throw new DSpaceSWORDException(e);
		}
		catch (IOException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Get the description of the treatment this class provides to the deposit
	 * 
	 * @return
	 */
	private String getTreatment()
	{
		return "The file has been attached to the specified item";
	}
}
