/* CollectionLocation.java
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

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * This class provides a single point of contact for
 * resolving Collections from SWORD Deposit URLs and for
 * generating SWORD Deposit URLs from Collections
 * 
 * @author Richard Jones
 *
 */
public class CollectionLocation
{
	/** Log4j logger */
	public static Logger log = Logger.getLogger(CollectionLocation.class);
	
	/**
	 * Obtain the deposit URL for the given collection.  These URLs
	 * should not be considered persistent, but will remain consistent
	 * unless configuration changes are made to DSpace
	 * 
	 * @param collection
	 * @return	The Deposit URL
	 * @throws DSpaceSWORDException
	 */
	public String getLocation(Collection collection)
		throws DSpaceSWORDException
	{
		return this.getBaseUrl() + "/" + collection.getHandle();
	}
	
	/**
	 * Obtain the collection which is represented by the given 
	 * URL
	 * 
	 * @param context	the DSpace context
	 * @param location	the URL to resolve to a collection
	 * @return		The collection to which the url resolves
	 * @throws DSpaceSWORDException
	 */
	public Collection getCollection(Context context, String location)
		throws DSpaceSWORDException
	{
		try
		{
			String baseUrl = this.getBaseUrl();
			if (baseUrl.length() == location.length())
			{
				throw new DSpaceSWORDException("The deposit URL is incomplete");
			}
			String handle = location.substring(baseUrl.length());
			if (handle.startsWith("/"))
			{
				handle = handle.substring(1);
			}
			if ("".equals(handle))
			{
				throw new DSpaceSWORDException("The deposit URL is incomplete");
			}
			
			DSpaceObject dso = HandleManager.resolveToObject(context, handle);
			
			if (!(dso instanceof Collection))
			{
				throw new DSpaceSWORDException("The deposit URL does not resolve to a valid collection");
			}
			
			return (Collection) dso;
		}
		catch (SQLException e)
		{
			log.error("Caught exception:", e);
			throw new DSpaceSWORDException("There was a problem resolving the collection", e);
		}
	}
	
	/**
	 * Get the base deposit URL for the DSpace SWORD implementation.  This
	 * is effectively the URL of the servlet which deals with deposit
	 * requests, and is used as the basis for the individual Collection
	 * URLs
	 * 
	 * If the configuration sword.deposit.url is set, this will be returned,
	 * but if not, it will construct the url as follows:
	 * 
	 * [dspace.url]/dspace-sword/deposit
	 * 
	 * where dspace.url is also in the configuration file.
	 * 
	 * @return	the base URL for sword deposit
	 * @throws DSpaceSWORDException
	 */
	private String getBaseUrl()
		throws DSpaceSWORDException
	{
		String depositUrl = ConfigurationManager.getProperty("sword.deposit.url");
		if (depositUrl == null || "".equals(depositUrl))
		{
			String dspaceUrl = ConfigurationManager.getProperty("dspace.url");
			if (dspaceUrl == null || "".equals(dspaceUrl))
			{
				throw new DSpaceSWORDException("Unable to construct deposit urls, due to missing/invalid config in sword.deposit.url and/or dspace.url");
			}

            try
            {
                URL url = new URL(dspaceUrl);
                depositUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/sword/deposit").toString();
            }
            catch (MalformedURLException e)
            {
                throw new DSpaceSWORDException("Unable to construct deposit urls, due to invalid dspace.url " + e.getMessage(),e);
            }
			
			
		}
		return depositUrl;
	}
}
