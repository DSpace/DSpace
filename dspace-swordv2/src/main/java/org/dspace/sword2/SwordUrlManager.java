/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.abdera.i18n.iri.IRI;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.handle.HandleServiceImpl;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.swordapp.server.SwordError;

import java.sql.SQLException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;

/**
 * @author Richard Jones
 *
 * Class responsible for constructing and de-constructing SWORD URL space
 * URLs
 */
public class SwordUrlManager
{
    protected ItemService itemService =
        ContentServiceFactory.getInstance().getItemService();

    protected BitstreamService bitstreamService =
        ContentServiceFactory.getInstance().getBitstreamService();

    protected HandleService handleService =
        HandleServiceFactory.getInstance().getHandleService();

    /** the SWORD configuration */
    private SwordConfigurationDSpace config;

    /** the active DSpace context */
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
     *     target collection
     * @return The Deposit URL
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
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
     *     target community
     * @return The Deposit URL
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
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
            String dspaceUrl = ConfigurationManager
                .getProperty("dspace.baseUrl");
            if (dspaceUrl == null || "".equals(dspaceUrl))
            {
                throw new DSpaceSwordException(
                    "Unable to construct service document urls, due to missing/invalid " +
                    "config in sword2.url and/or dspace.baseUrl");
            }

            try
            {
                URL url = new URL(dspaceUrl);
                sUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(),
                    "/swordv2").toString();
            }
            catch (MalformedURLException e)
            {
                throw new DSpaceSwordException(
                    "Unable to construct service document urls, due to invalid dspace.baseUrl " +
                    e.getMessage(), e);
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
                throw new SwordError(DSpaceUriRegistry.BAD_URL,
                    "The item URL is invalid");
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

            Item item = itemService.findByIdOrLegacyId(context, iid);
            return item;
        }
        catch (SQLException e)
        {
            // log.error("Caught exception:", e);
            throw new DSpaceSwordException(
                "There was a problem resolving the item", e);
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
     * @param context    the DSpace context
     * @param location    the URL to resolve to a collection
     * @return The collection to which the url resolves
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     * @throws SwordError
     *     if a proper URL cannot be calculated.
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
                throw new SwordError(DSpaceUriRegistry.BAD_URL,
                    "The deposit URL is incomplete");
            }
            String handle = location.substring(baseUrl.length());
            if (handle.startsWith("/"))
            {
                handle = handle.substring(1);
            }
            if ("".equals(handle))
            {
                throw new SwordError(DSpaceUriRegistry.BAD_URL,
                    "The deposit URL is incomplete");
            }

            DSpaceObject dso = handleService.resolveToObject(context, handle);
            if (dso == null)
            {
                return null;
            }

            if (!(dso instanceof Collection))
            {
                throw new SwordError(DSpaceUriRegistry.BAD_URL,
                    "The deposit URL does not resolve to a valid collection");
            }

            return (Collection) dso;
        }
        catch (SQLException e)
        {
            // log.error("Caught exception:", e);
            throw new DSpaceSwordException(
                "There was a problem resolving the collection", e);
        }
    }

    /**
     * Construct the service document URL for the given object, which will
     * be supplied in the sword:service element of other service document
     * entries.
     *
     * @param community
     *     target community
     * @return service document URL
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
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
     *     target collection
     * @return service document URL
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
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
     *
     * @param url
     *     URL to get DSpace object from
     * @return DSpace object from URL
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     * @throws SwordError
     *     SWORD error per SWORD spec
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

                DSpaceObject dso = handleService.resolveToObject(context, url);
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
                    "Unable to recognise URL as a valid service document: " +
                    url);
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
     * @return service document base URL
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getBaseServiceDocumentUrl()
            throws DSpaceSwordException
    {
        String sdUrl = ConfigurationManager
            .getProperty("swordv2-server", "servicedocument.url");
        if (sdUrl == null || "".equals(sdUrl))
        {
            String dspaceUrl = ConfigurationManager
                .getProperty("dspace.baseUrl");
            if (dspaceUrl == null || "".equals(dspaceUrl))
            {
                throw new DSpaceSwordException(
                    "Unable to construct service document urls, due to missing/invalid " +
                    "config in swordv2-server.cfg servicedocument.url and/or dspace.baseUrl");
            }

            try
            {
                URL url = new URL(dspaceUrl);
                sdUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(),
                    "/swordv2/servicedocument").toString();
            }
            catch (MalformedURLException e)
            {
                throw new DSpaceSwordException(
                    "Unable to construct service document urls, due to invalid dspace.baseUrl " +
                    e.getMessage(), e);
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
     * @return the base URL for SWORD deposit
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getBaseCollectionUrl()
            throws DSpaceSwordException
    {
        String depositUrl = ConfigurationManager
            .getProperty("swordv2-server", "collection.url");
        if (depositUrl == null || "".equals(depositUrl))
        {
            String dspaceUrl = ConfigurationManager
                .getProperty("dspace.baseUrl");
            if (dspaceUrl == null || "".equals(dspaceUrl))
            {
                throw new DSpaceSwordException(
                    "Unable to construct deposit urls, due to missing/invalid config in " +
                    "swordv2-server.cfg deposit.url and/or dspace.baseUrl");
            }

            try
            {
                URL url = new URL(dspaceUrl);
                depositUrl = new URL(url.getProtocol(), url.getHost(),
                    url.getPort(), "/swordv2/collection").toString();
            }
            catch (MalformedURLException e)
            {
                throw new DSpaceSwordException(
                    "Unable to construct deposit urls, due to invalid dspace.baseUrl " +
                    e.getMessage(), e);
            }

        }
        return depositUrl;
    }

    /**
     * Is the given URL the base service document URL?
     *
     * @param url
     *     URL to check
     * @return true if URL is service document base URL
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public boolean isBaseServiceDocumentUrl(String url)
            throws DSpaceSwordException
    {
        return this.getBaseServiceDocumentUrl().equals(url);
    }

    /**
     * Central location for constructing usable URLs for DSpace bitstreams.
     * There is no place in the main DSpace codebase for doing this.
     *
     * @param bitstream
     *     target bitstream
     * @return URL of given bitstream
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getBitstreamUrl(Bitstream bitstream)
            throws DSpaceSwordException
    {
        try
        {
            List<Bundle> bundles = bitstream.getBundles();
            Bundle parent = null;
            if (!bundles.isEmpty())
            {
                parent = bundles.get(0);
            }
            else
            {
                throw new DSpaceSwordException(
                    "Encountered orphaned bitstream");
            }

            List<Item> items = parent.getItems();
            Item item;
            if (!items.isEmpty())
            {
                item = items.get(0);
            }
            else
            {
                throw new DSpaceSwordException("Encountered orphaned bundle");
            }

            String handle = item.getHandle();
            String bsLink = ConfigurationManager.getProperty("dspace.url");

            if (handle != null && !"".equals(handle))
            {
                bsLink = bsLink + "/bitstream/" + handle + "/" +
                    bitstream.getSequenceID() + "/" + bitstream.getName();
            }
            else
            {
                bsLink = bsLink + "/retrieve/" + bitstream.getID() + "/" +
                        bitstream.getName();
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
        return this.getSwordBaseUrl() + "/edit-media/bitstream/" +
            bitstream.getID() + "/" + bitstream.getName();
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
                throw new SwordError(DSpaceUriRegistry.BAD_URL,
                    "The bitstream URL is invalid");
            }

            String bitstreamParts = location.substring(emBaseUrl.length());

            // the bitstream id is the part up to the first "/"
            int firstSlash = bitstreamParts.indexOf("/");
            String bid = bitstreamParts.substring(0, firstSlash);
            Bitstream bitstream =
                bitstreamService.findByIdOrLegacyId(context, bid);
            return bitstream;
        }
        catch (SQLException e)
        {
            // log.error("Caught exception:", e);
            throw new DSpaceSwordException(
                "There was a problem resolving the collection", e);
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
            String urlTemplate = ConfigurationManager
                    .getProperty("swordv2-server", "workspace.url-template");
            if (urlTemplate != null)
            {
                return urlTemplate.replace("#wsid#", Integer.toString(
                    wft.getWorkspaceItem(context, item).getID()));
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
            return handleService.getCanonicalForm(item.getHandle());
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
        return new IRI(this.getSwordBaseUrl() + "/edit-media/" + item.getID() +
            ".atom");
    }
}
