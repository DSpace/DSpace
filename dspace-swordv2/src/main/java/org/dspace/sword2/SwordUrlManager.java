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
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.handle.HandleManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.swordapp.server.SwordError;

import java.sql.SQLException;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * @author Richard Jones
 *
 * Class responsible for constructing and de-constructing sword url space
 * urls
 */
public class SwordUrlManager
{
	/** the sword configuration */
	private SwordConfigurationDSpace config;

	/** the active dspace context */
	private Context context;

	public SwordUrlManager(SwordConfigurationDSpace config, Context context)
	{
		this.config = config;
		this.context = context;
	}

	/**
	 * Obtain the deposit URL for the given collection.  These URLs
	 * should not be considered persistent, but will remain consistent
	 * unless configuration changes are made to DSpace
	 *
	 * @param collection
	 * @return	The Deposit URL
	 * @throws DSpaceSwordException
	 */
	public String getDepositLocation(Collection collection)
		throws DSpaceSwordException
	{
		return this.getBaseCollectionUrl() + "/" + collection.getHandle();
	}

	/**
	 * Obtain the deposit URL for the given community.  These URLs
	 * should not be considered persistent, but will remain consistent
	 * unless configuration changes are made to DSpace
	 *
	 * @param community
	 * @return	The Deposit URL
	 * @throws DSpaceSwordException
	 */
	public String getDepositLocation(Community community)
		throws DSpaceSwordException
	{
		if (this.config.allowCommunityDeposit())
		{
			return this.getBaseCollectionUrl() + "/" + community.getHandle();
		}
		return null;
	}

	public String getSwordBaseUrl()
			throws DSpaceSwordException
	{
		String sUrl = ConfigurationManager.getProperty("swordv2-server", "url");
		if (sUrl == null || "".equals(sUrl))
		{
			String dspaceUrl = ConfigurationManager.getProperty("dspace.baseUrl");
			if (dspaceUrl == null || "".equals(dspaceUrl))
			{
				throw new DSpaceSwordException("Unable to construct service document urls, due to missing/invalid " +
						"config in sword2.url and/or dspace.baseUrl");
			}

            try
            {
                URL url = new URL(dspaceUrl);
                sUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/swordv2").toString();
            }
            catch (MalformedURLException e)
            {
                throw new DSpaceSwordException("Unable to construct service document urls, due to invalid dspace.baseUrl " +
						e.getMessage(),e);
            }
		}
		return sUrl;
	}

    public Item getItem(Context context, String location)
            throws DSpaceSwordException, SwordError
    {
        try
		{
			String baseUrl = this.getSwordBaseUrl();
            String emBaseUrl = baseUrl + "/edit-media/";
			String eBaseUrl = baseUrl + "/edit/";
			String sBaseUrl = baseUrl + "/statement/";
			String cBaseUrl = null;
			if (location.startsWith(emBaseUrl))
			{
				cBaseUrl = emBaseUrl;
			}
			else if (location.startsWith(eBaseUrl))
			{
				cBaseUrl = eBaseUrl;
			}
			else if (location.startsWith(sBaseUrl))
			{
				cBaseUrl = sBaseUrl;
			}
            else
            {
				throw new SwordError(DSpaceUriRegistry.BAD_URL, "The item URL is invalid");
			}

            String iid = location.substring(cBaseUrl.length());
            if (iid.endsWith(".atom"))
            {
                // this is the atom url, so we need to strip that to ge tthe item id
                iid = iid.substring(0, iid.length() - ".atom".length());
            }
			else if (iid.endsWith(".rdf"))
			{
				// this is the rdf url so we need to strip that to get the item id
				iid = iid.substring(0, iid.length() - ".rdf".length());
			}
			
            int itemId = Integer.parseInt(iid);
            Item item = Item.find(context, itemId);
			return item;
		}
		catch (SQLException e)
		{
			// log.error("Caught exception:", e);
			throw new DSpaceSwordException("There was a problem resolving the item", e);
		}
    }

	public String getTypeSuffix(Context context, String location)
	{
		String tail = location.substring(location.lastIndexOf("/"));
		int typeSeparator = tail.lastIndexOf(".");
		if (typeSeparator == -1)
		{
			return null;
		}
		return tail.substring(typeSeparator + 1);
	}

    public boolean isFeedRequest(Context context, String url)
    {
        return url.endsWith(".atom");
    }

	/**
	 * Obtain the collection which is represented by the given
	 * URL
	 *
	 * @param context	the DSpace context
	 * @param location	the URL to resolve to a collection
	 * @return		The collection to which the url resolves
	 * @throws DSpaceSwordException
	 */
	// FIXME: we need to generalise this to DSpaceObjects, so that we can support
	// Communities, Collections and Items separately
	public Collection getCollection(Context context, String location)
		throws DSpaceSwordException, SwordError
	{
		try
		{
			String baseUrl = this.getBaseCollectionUrl();
			if (baseUrl.length() == location.length())
			{
				throw new SwordError(DSpaceUriRegistry.BAD_URL, "The deposit URL is incomplete");
			}
			String handle = location.substring(baseUrl.length());
			if (handle.startsWith("/"))
			{
				handle = handle.substring(1);
			}
			if ("".equals(handle))
			{
				throw new SwordError(DSpaceUriRegistry.BAD_URL, "The deposit URL is incomplete");
			}

			DSpaceObject dso = HandleManager.resolveToObject(context, handle);
            if (dso == null)
            {
                return null;
            }

			if (!(dso instanceof Collection))
			{
				throw new SwordError(DSpaceUriRegistry.BAD_URL, "The deposit URL does not resolve to a valid collection");
			}

			return (Collection) dso;
		}
		catch (SQLException e)
		{
			// log.error("Caught exception:", e);
			throw new DSpaceSwordException("There was a problem resolving the collection", e);
		}
	}

	/**
	 * Construct the service document URL for the given object, which will
	 * be supplied in the sword:service element of other service document
	 * entries.
	 *
	 * @param community
	 * @throws DSpaceSwordException
	 */
	public String constructSubServiceUrl(Community community)
			throws DSpaceSwordException
	{
		String base = this.getBaseServiceDocumentUrl();
		String handle = community.getHandle();
		return base + "/" + handle;
	}

	/**
	 * Construct the service document URL for the given object, which will
	 * be supplied in the sword:service element of other service document
	 * entries.
	 *
	 * @param collection
	 * @throws DSpaceSwordException
	 */
	public String constructSubServiceUrl(Collection collection)
			throws DSpaceSwordException
	{
		String base = this.getBaseServiceDocumentUrl();
		String handle = collection.getHandle();
		return base + "/" + handle;
	}

	/**
	 * Extract a DSpaceObject from the given URL.  If this method is unable to
	 * locate a meaningful and appropriate DSpace object it will throw the
	 * appropriate SWORD error.
	 * @param url
	 * @throws DSpaceSwordException
	 * @throws SwordError
	 */
	public DSpaceObject extractDSpaceObject(String url)
			throws DSpaceSwordException, SwordError
	{
		try
		{
			String sdBase = this.getBaseServiceDocumentUrl();
			// String mlBase = this.getBaseMediaLinkUrl();

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
                if (dso == null)
                {
                    return null;
                }
				else if (dso instanceof Collection || dso instanceof Community)
				{
					return dso;
				}
				else
				{
					throw new SwordError(DSpaceUriRegistry.BAD_URL,
							"Service Document request does not refer to a DSpace Collection or Community");
				}
			}
			else
			{
				throw new SwordError(DSpaceUriRegistry.BAD_URL,
						"Unable to recognise URL as a valid service document: " + url);
			}
		}
		catch (SQLException e)
		{
			throw new DSpaceSwordException(e);
		}
	}

	/**
	 * Get the base URL for service document requests.
	 *
	 * @throws DSpaceSwordException
	 */
	public String getBaseServiceDocumentUrl()
			throws DSpaceSwordException
	{
		String sdUrl = ConfigurationManager.getProperty("swordv2-server", "servicedocument.url");
		if (sdUrl == null || "".equals(sdUrl))
		{
			String dspaceUrl = ConfigurationManager.getProperty("dspace.baseUrl");
			if (dspaceUrl == null || "".equals(dspaceUrl))
			{
				throw new DSpaceSwordException("Unable to construct service document urls, due to missing/invalid " +
						"config in swordv2-server.cfg servicedocument.url and/or dspace.baseUrl");
			}

            try
            {
                URL url = new URL(dspaceUrl);
                sdUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/swordv2/servicedocument").toString();
            }
            catch (MalformedURLException e)
            {
                throw new DSpaceSwordException("Unable to construct service document urls, due to invalid dspace.baseUrl " +
						e.getMessage(),e);
            }
		}
		return sdUrl;
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
	 * [dspace.baseUrl]/sword/deposit
	 *
	 * where dspace.baseUrl is also in the configuration file.
	 *
	 * @return	the base URL for sword deposit
	 * @throws DSpaceSwordException
	 */
	public String getBaseCollectionUrl()
		throws DSpaceSwordException
	{
		String depositUrl = ConfigurationManager.getProperty("swordv2-server", "collection.url");
		if (depositUrl == null || "".equals(depositUrl))
		{
			String dspaceUrl = ConfigurationManager.getProperty("dspace.baseUrl");
			if (dspaceUrl == null || "".equals(dspaceUrl))
			{
				throw new DSpaceSwordException("Unable to construct deposit urls, due to missing/invalid config in " +
						"swordv2-server.cfg deposit.url and/or dspace.baseUrl");
			}

            try
            {
                URL url = new URL(dspaceUrl);
                depositUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/swordv2/collection").toString();
            }
            catch (MalformedURLException e)
            {
                throw new DSpaceSwordException("Unable to construct deposit urls, due to invalid dspace.baseUrl " +
						e.getMessage(),e);
            }


		}
		return depositUrl;
	}

	/**
	 * Is the given URL the base service document URL?
	 *
	 * @param url
	 * @throws DSpaceSwordException
	 */
	public boolean isBaseServiceDocumentUrl(String url)
			throws DSpaceSwordException
	{
		return this.getBaseServiceDocumentUrl().equals(url);
	}

	/**
	 * Central location for constructing usable URLs for DSpace bitstreams.
	 * There is no place in the main DSpace code base for doing this.
	 *
	 * @param bitstream
	 * @throws DSpaceSwordException
	 */
	public String getBitstreamUrl(Bitstream bitstream)
			throws DSpaceSwordException
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
				throw new DSpaceSwordException("Encountered orphaned bitstream");
			}

			Item[] items = parent.getItems();
			Item item;
			if (items.length > 0)
			{
				item = items[0];
			}
			else
			{
				throw new DSpaceSwordException("Encountered orphaned bundle");
			}

			String handle = item.getHandle();
			String bsLink = ConfigurationManager.getProperty("dspace.url");

			if (handle != null && !"".equals(handle))
			{
				bsLink = bsLink + "/bitstream/" + handle + "/" + bitstream.getSequenceID() + "/" + bitstream.getName();
			}
			else
			{
				bsLink = bsLink + "/retrieve/" + bitstream.getID() + "/" + bitstream.getName();
			}

			return bsLink;
		}
		catch (SQLException e)
		{
			throw new DSpaceSwordException(e);
		}
	}

	public String getActionableBitstreamUrl(Bitstream bitstream)
			throws DSpaceSwordException
	{
		return this.getSwordBaseUrl() + "/edit-media/bitstream/" + bitstream.getID() + "/" + bitstream.getName();
	}

	public boolean isActionableBitstreamUrl(Context context, String url)
	{
		return url.contains("/edit-media/bitstream/");
	}

	public Bitstream getBitstream(Context context, String location)
			throws DSpaceSwordException, SwordError
	{
		try
		{
			String baseUrl = this.getSwordBaseUrl();
            String emBaseUrl = baseUrl + "/edit-media/bitstream/";
			if (!location.startsWith(emBaseUrl))
			{
				throw new SwordError(DSpaceUriRegistry.BAD_URL, "The bitstream URL is invalid");
			}

            String bitstreamParts = location.substring(emBaseUrl.length());

			// the bitstream id is the part up to the first "/"
			int firstSlash = bitstreamParts.indexOf("/");
			int bid = Integer.parseInt(bitstreamParts.substring(0, firstSlash));
			String fn = bitstreamParts.substring(firstSlash + 1);

            Bitstream bitstream = Bitstream.find(context, bid);
			return bitstream;
		}
		catch (SQLException e)
		{
			// log.error("Caught exception:", e);
			throw new DSpaceSwordException("There was a problem resolving the collection", e);
		}
	}

	// FIXME: we need a totally new kind of URL scheme; perhaps we write the identifier into the item
	public String getAtomStatementUri(Item item)
			throws DSpaceSwordException
	{
		return this.getSwordBaseUrl() + "/statement/" + item.getID() + ".atom";
	}

	public String getOreStatementUri(Item item)
			throws DSpaceSwordException
	{
		return this.getSwordBaseUrl() + "/statement/" + item.getID() + ".rdf";
	}

	public String getAggregationUrl(Item item)
			throws DSpaceSwordException
	{
		return this.getOreStatementUri(item) + "#aggregation";
	}

	public IRI getEditIRI(Item item)
			throws DSpaceSwordException
	{
		return new IRI(this.getSwordBaseUrl() + "/edit/" + item.getID());
	}

	public String getSplashUrl(Item item)
			throws DSpaceSwordException
	{
		WorkflowTools wft = new WorkflowTools();

        // if the item is in the workspace, we need to give it it's own
        // special identifier
        if (wft.isItemInWorkspace(context, item))
        {
            String urlTemplate = ConfigurationManager.getProperty("swordv2-server", "workspace.url-template");
            if (urlTemplate != null)
            {
                return urlTemplate.replace("#wsid#", Integer.toString(wft.getWorkspaceItem(context, item).getID()));
            }
        }
        // otherwise, it may be in the workflow, in which case there is
        // no identifier
        else if (wft.isItemInWorkflow(context, item))
        {
            // do nothing
            return null;
        }
        // finally, otherwise we need to just return the handle of the
        // item
        else
        {
		    return HandleManager.getCanonicalForm(item.getHandle());
        }
        return null;
	}

	public IRI getContentUrl(Item item)
			throws DSpaceSwordException
	{
		return new IRI(this.getSwordBaseUrl() + "/edit-media/" + item.getID());
	}

    public IRI getMediaFeedUrl(Item item)
			throws DSpaceSwordException
	{
		return new IRI(this.getSwordBaseUrl() + "/edit-media/" + item.getID() + ".atom");
	}
}
