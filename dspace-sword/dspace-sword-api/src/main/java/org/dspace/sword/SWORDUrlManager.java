/* SWORDUrlManager.java
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
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.handle.HandleManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.purl.sword.base.SWORDErrorException;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * @author Richard Jones
 *
 * Class responsible for constructing and de-constructing sword url space
 * urls
 */
public class SWORDUrlManager
{
	/** logger */
	private static Logger log = Logger.getLogger(SWORDUrlManager.class);

	/** the sword configuration */
	private SWORDConfiguration config;

	/** the active dspace context */
	private Context context;

	public SWORDUrlManager(SWORDConfiguration config, Context context)
	{
		this.config = config;
		this.context = context;
	}

	/**
	 * Get the generator url for atom entry documents.  This can be
	 * overridden from the default in configuration
	 *
	 * @return
	 */
	public String getGeneratorUrl()
	{
		String cfg = ConfigurationManager.getProperty("sword.generator.url");
		if (cfg == null || "".equals(cfg))
		{
			return SWORDProperties.SOFTWARE_URI;
		}
		return cfg;
	}

	/**
	 * Obtain the deposit URL for the given collection.  These URLs
	 * should not be considered persistent, but will remain consistent
	 * unless configuration changes are made to DSpace
	 *
	 * @param collection
	 * @return	The Deposit URL
	 * @throws DSpaceSWORDException
	 */
	public String getDepositLocation(Collection collection)
		throws DSpaceSWORDException
	{
		return this.getBaseDepositUrl() + "/" + collection.getHandle();
	}

	/**
	 * Obtain the deposit URL for the given item.  These URLs
	 * should not be considered persistent, but will remain consistent
	 * unless configuration changes are made to DSpace
	 *
	 * @param item
	 * @return	The Deposit URL
	 * @throws DSpaceSWORDException
	 */
	public String getDepositLocation(Item item)
		throws DSpaceSWORDException
	{
		return this.getBaseDepositUrl() + "/" + item.getHandle();
	}

	/**
	 * Obtain the deposit URL for the given community.  These URLs
	 * should not be considered persistent, but will remain consistent
	 * unless configuration changes are made to DSpace
	 *
	 * @param community
	 * @return	The Deposit URL
	 * @throws DSpaceSWORDException
	 */
	public String getDepositLocation(Community community)
		throws DSpaceSWORDException
	{
		// FIXME: there is no deposit url for communities yet, so this could
		// be misleading
		return this.getBaseDepositUrl() + "/" + community.getHandle();
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
	// FIXME: we need to generalise this to DSpaceObjects, so that we can support
	// Communities, Collections and Items separately
	public Collection getCollection(Context context, String location)
		throws DSpaceSWORDException, SWORDErrorException
	{
		try
		{
			String baseUrl = this.getBaseDepositUrl();
			if (baseUrl.length() == location.length())
			{
				throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL, "The deposit URL is incomplete");
			}
			String handle = location.substring(baseUrl.length());
			if (handle.startsWith("/"))
			{
				handle = handle.substring(1);
			}
			if ("".equals(handle))
			{
				throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL, "The deposit URL is incomplete");
			}

			DSpaceObject dso = HandleManager.resolveToObject(context, handle);

			if (!(dso instanceof Collection))
			{
				throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL, "The deposit URL does not resolve to a valid collection");
			}

			return (Collection) dso;
		}
		catch (SQLException e)
		{
			// log.error("Caught exception:", e);
			throw new DSpaceSWORDException("There was a problem resolving the collection", e);
		}
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
	public DSpaceObject getDSpaceObject(Context context, String location)
		throws DSpaceSWORDException, SWORDErrorException
	{
		try
		{
			String baseUrl = this.getBaseDepositUrl();
			if (baseUrl.length() == location.length())
			{
				throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL, "The deposit URL is incomplete");
			}
			String handle = location.substring(baseUrl.length());
			if (handle.startsWith("/"))
			{
				handle = handle.substring(1);
			}
			if ("".equals(handle))
			{
				throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL, "The deposit URL is incomplete");
			}

			DSpaceObject dso = HandleManager.resolveToObject(context, handle);

			if (!(dso instanceof Collection) && !(dso instanceof Item))
			{
				throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL, "The deposit URL does not resolve to a valid deposit target");
			}

			return dso;
		}
		catch (SQLException e)
		{
			// log.error("Caught exception:", e);
			throw new DSpaceSWORDException("There was a problem resolving the collection", e);
		}
	}

	/**
	 * Construct the service document url for the given object, which will
	 * be supplied in the sword:service element of other service document entries
	 *
	 * @param community
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public String constructSubServiceUrl(Community community)
			throws DSpaceSWORDException
	{
		String base = this.getBaseServiceDocumentUrl();
		String handle = community.getHandle();
		return base + "/" + handle;
	}

	/**
	 * Construct the service document url for the given object, which will
	 * be supplied in the sword:service element of other service document entries
	 *
	 * @param collection
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public String constructSubServiceUrl(Collection collection)
			throws DSpaceSWORDException
	{
		String base = this.getBaseServiceDocumentUrl();
		String handle = collection.getHandle();
		return base + "/" + handle;
	}

	/**
	 * Extract a DSpaceObject from the given url.  If this method is unable to
	 * locate a meaningful and appropriate dspace object it will throw the
	 * appropriate sword error
	 * @param url
	 * @return
	 * @throws DSpaceSWORDException
	 * @throws SWORDErrorException
	 */
	public DSpaceObject extractDSpaceObject(String url)
			throws DSpaceSWORDException, SWORDErrorException
	{
		try
		{
			String sdBase = this.getBaseServiceDocumentUrl();
			String mlBase = this.getBaseMediaLinkUrl();

			if (url.startsWith(sdBase))
			{
				// we are dealing with a service document request
				
				// first, let's find the beginning of the handle
				url = url.substring(sdBase.length());
				if (url.startsWith("/"))
				{
					url = url.substring(1);
				}
				if (url.endsWith("/"))
				{
					url = url.substring(0, url.length() - 1);
				}

				DSpaceObject dso = HandleManager.resolveToObject(context, url);
				if (dso instanceof Collection || dso instanceof Community)
				{
					return dso;
				}
				else
				{
					throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL,
							"Service Document request does not refer to a DSpace Collection or Community");
				}
			}
			else if (url.startsWith(mlBase))
			{
				// we are dealing with a bitstream media link

				// find the index of the "/bitstream/" segment of the url
				int bsi = url.indexOf("/bitstream/");

				// subtsring the url from the end of this "/bitstream/" string, to get the bitstream id
				String bsid = url.substring(bsi + 11);

				// strip off extraneous slashes
				if (bsid.endsWith("/"))
				{
					bsid = bsid.substring(0, url.length() - 1);
				}

				Bitstream bitstream = Bitstream.find(context, Integer.parseInt(bsid));
				return bitstream;
			}
			else
			{
				throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL,
						"Unable to recognise URL as a valid service document: " + url);
			}
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * get the base url for service document requests
	 *
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public String getBaseServiceDocumentUrl()
			throws DSpaceSWORDException
	{
		String depositUrl = ConfigurationManager.getProperty("sword.servicedocument.url");
		if (depositUrl == null || "".equals(depositUrl))
		{
			String dspaceUrl = ConfigurationManager.getProperty("dspace.url");
			if (dspaceUrl == null || "".equals(dspaceUrl))
			{
				throw new DSpaceSWORDException("Unable to construct service document urls, due to missing/invalid " +
						"config in sword.servicedocument.url and/or dspace.url");
			}

            try
            {
                URL url = new URL(dspaceUrl);
                depositUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/sword/servicedocument").toString();
            }
            catch (MalformedURLException e)
            {
                throw new DSpaceSWORDException("Unable to construct service document urls, due to invalid dspace.url " +
						e.getMessage(),e);
            }


		}
		return depositUrl;
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
	public String getBaseDepositUrl()
		throws DSpaceSWORDException
	{
		String depositUrl = ConfigurationManager.getProperty("sword.deposit.url");
		if (depositUrl == null || "".equals(depositUrl))
		{
			String dspaceUrl = ConfigurationManager.getProperty("dspace.url");
			if (dspaceUrl == null || "".equals(dspaceUrl))
			{
				throw new DSpaceSWORDException("Unable to construct deposit urls, due to missing/invalid config in " +
						"sword.deposit.url and/or dspace.url");
			}

            try
            {
                URL url = new URL(dspaceUrl);
                depositUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/sword/deposit").toString();
            }
            catch (MalformedURLException e)
            {
                throw new DSpaceSWORDException("Unable to construct deposit urls, due to invalid dspace.url " +
						e.getMessage(),e);
            }


		}
		return depositUrl;
	}

	/**
	 * is the given url the base service document url
	 *
	 * @param url
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public boolean isBaseServiceDocumentUrl(String url)
			throws DSpaceSWORDException
	{
		return this.getBaseServiceDocumentUrl().equals(url);
	}

	/**
	 * is the given url the base media link url
	 *
	 * @param url
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public boolean isBaseMediaLinkUrl(String url)
			throws DSpaceSWORDException
	{
		return this.getBaseMediaLinkUrl().equals(url);
	}

	/**
	 * Central location for constructing usable urls for dspace bitstreams.  There
	 * is no place in the main DSpace code base for doing this.
	 *
	 * @param bitstream
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public String getBitstreamUrl(Bitstream bitstream)
			throws DSpaceSWORDException
	{
		try
		{
			Bundle[] bundles = bitstream.getBundles();
			Bundle parent = null;
			if (bundles.length > 0)
			{
				parent = bundles[0];
			}
			else
			{
				throw new DSpaceSWORDException("Encountered orphaned bitstream");
			}

			Item[] items = parent.getItems();
			Item item;
			if (items.length > 0)
			{
				item = items[0];
			}
			else
			{
				throw new DSpaceSWORDException("Encountered orphaned bundle");
			}

			String handle = item.getHandle();
			String bsLink = ConfigurationManager.getProperty("dspace.url");

			if (handle != null && !"".equals(handle))
			{
				bsLink = bsLink + "/bitstream/" + handle + "/" + bitstream.getSequenceID() + "/" + bitstream.getName();
			}
			else
			{
				bsLink = bsLink + "/retrieve/" + bitstream.getID();
			}

			return bsLink;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * get the base media link url
	 *
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public String getBaseMediaLinkUrl()
			throws DSpaceSWORDException
	{
		String mlUrl = ConfigurationManager.getProperty("sword.media-link.url");
		if (mlUrl == null || "".equals(mlUrl))
		{
			String dspaceUrl = ConfigurationManager.getProperty("dspace.url");
			if (dspaceUrl == null || "".equals(dspaceUrl))
			{
				throw new DSpaceSWORDException("Unable to construct media-link urls, due to missing/invalid config in " +
						"sword.media-link.url and/or dspace.url");
			}

            try
            {
                URL url = new URL(dspaceUrl);
                mlUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/sword/media-link").toString();
            }
            catch (MalformedURLException e)
            {
                throw new DSpaceSWORDException("Unable to construct media-link urls, due to invalid dspace.url " +
						e.getMessage(),e);
            }


		}
		return mlUrl;
	}

	/**
	 * get the media link url for the given item
	 *
	 * @param dso
	 * @return
	 * @throws DSpaceSWORDException
	 */
	private String getMediaLink(Item dso)
			throws DSpaceSWORDException
	{
		String ml = this.getBaseMediaLinkUrl();
		String handle = dso.getHandle();
		if (handle != null)
		{
			ml = ml + "/" + dso.getHandle();
		}
		return ml;
	}

	/**
	 * get the media link url for the given bitstream
	 * 
	 * @param bitstream
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public String getMediaLink(Bitstream bitstream)
			throws DSpaceSWORDException
	{
		try
		{
			Bundle[] bundles = bitstream.getBundles();
			Bundle parent = null;
			if (bundles.length > 0)
			{
				parent = bundles[0];
			}
			else
			{
				throw new DSpaceSWORDException("Encountered orphaned bitstream");
			}

			Item[] items = parent.getItems();
			Item item;
			if (items.length > 0)
			{
				item = items[0];
			}
			else
			{
				throw new DSpaceSWORDException("Encountered orphaned bundle");
			}

			String itemUrl = this.getMediaLink(item);
			if (itemUrl.equals(this.getBaseMediaLinkUrl()))
			{
				return itemUrl;
			}

			String bsUrl = itemUrl + "/bitstream/" + bitstream.getID();

			return bsUrl;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}
}
