/* SWORDService.java
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

import java.sql.SQLException;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.content.BitstreamFormat;

import org.purl.sword.base.Deposit;

/**
 * @author Richard Jones
 *
 * This class represents the actual sword service provided by dspace.  It
 * is the central location for the authentcated contexts, the installation
 * specific configuration, and the url management.
 *
 * It is ubiquotous in the sword implementation, and all related
 * information and services should be retrived via this api
 *
 */
public class SWORDService
{
	/** Log4j logging instance */
	public static Logger log = Logger.getLogger(SWORDService.class);

	/** The SWORD context of the request */
	private SWORDContext swordContext;

	/** The configuration of the sword server instance */
	private SWORDConfiguration swordConfig;

	/** The URL Generator */
	private SWORDUrlManager urlManager;

	/** a holder for the messages coming through from the implementation */
	private StringBuilder verboseDescription = new StringBuilder();

	/** is this a verbose operation */
	private boolean verbose = false;

	/** date formatter */
	private SimpleDateFormat dateFormat;

	/**
	 * Construct a new service instance around the given authenticated
	 * sword context
	 *
	 * @param sc
	 */
	public SWORDService(SWORDContext sc)
	{
		this.swordContext = sc;
		this.swordConfig = new SWORDConfiguration();
		this.urlManager = new SWORDUrlManager(this.swordConfig, this.swordContext.getContext());
		dateFormat = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.S]");
	}

	public SWORDUrlManager getUrlManager()
	{
		return urlManager;
	}

	public void setUrlManager(SWORDUrlManager urlManager)
	{
		this.urlManager = urlManager;
	}

	public SWORDContext getSwordContext()
	{
		return swordContext;
	}

	public void setSwordContext(SWORDContext swordContext)
	{
		this.swordContext = swordContext;
	}

	public SWORDConfiguration getSwordConfig()
	{
		return swordConfig;
	}

	public void setSwordConfig(SWORDConfiguration swordConfig)
	{
		this.swordConfig = swordConfig;
	}

	public Context getContext()
	{
		return this.swordContext.getContext();
	}

	public boolean isVerbose()
	{
		return verbose;
	}

	public void setVerbose(boolean verbose)
	{
		this.verbose = verbose;
	}

	public StringBuilder getVerboseDescription()
	{
		return verboseDescription;
	}

	/**
	 * shortcut to registering a message with the verboseDescription
	 * member variable.  This checks to see if the request is
	 * verbose, meaning we don't have to do it inline and break nice
	 * looking code up
	 *
	 * @param message
	 */
	public void message(String message)
	{
		// build the processing message
		String msg = dateFormat.format(new Date()) + " " + message + "; \n\n";

		// if this is a verbose deposit, then log it
		if (this.verbose)
		{
			verboseDescription.append(msg);
		}

		// add to server logs anyway
		log.info(msg);
	}

	/**
	 * Construct the most appropriate filename for the incoming deposit
	 *
	 * @param context
	 * @param deposit
	 * @param original
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public String getFilename(Context context, Deposit deposit, boolean original)
			throws DSpaceSWORDException
	{
		try
		{
			BitstreamFormat bf = BitstreamFormat.findByMIMEType(context, deposit.getContentType());
			String[] exts = null;
			if (bf != null)
			{
				exts = bf.getExtensions();
			}

			String fn = deposit.getFilename();
			if (fn == null || "".equals(fn))
			{
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				fn = "sword-" + sdf.format(new Date());
				if (original)
				{
					fn = fn + ".original";
				}
				if (exts != null)
				{
					fn = fn + "." + exts[0];
				}
			}

			return fn;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Get the name of the temp files that should be used
	 * 
	 * @return
	 */
	public String getTempFilename()
	{
		return "sword-" + (new Date()).getTime();
	}
}
