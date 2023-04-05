/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.purl.sword.base.SWORDErrorException;

/**
 * @author Richard Jones
 *
 * Class responsible for constructing and de-constructing sword url space
 * urls
 */
public class SWORDUrlManager {
    /**
     * the SWORD configuration
     */
    private final SWORDConfiguration config;

    /**
     * the active DSpace context
     */
    private final Context context;

    protected HandleService handleService =
        HandleServiceFactory.getInstance().getHandleService();

    protected BitstreamService bitstreamService =
        ContentServiceFactory.getInstance().getBitstreamService();

    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    private String swordPath = configurationService.getProperty(
            "sword-server.path", "sword");

    private String dspaceUrl = configurationService.getProperty(
            "dspace.server.url");

    public SWORDUrlManager(SWORDConfiguration config, Context context) {
        this.config = config;
        this.context = context;
    }

    /**
     * Get the generator URL for ATOM entry documents.  This can be
     * overridden from the default in configuration.
     *
     * @return the generator URL for ATOM entry documents
     */
    public String getGeneratorUrl() {
        String cfg = configurationService.getProperty(
            "sword-server.generator.url");
        if (cfg == null || "".equals(cfg)) {
            return SWORDProperties.SOFTWARE_URI;
        }
        return cfg;
    }

    /**
     * Obtain the deposit URL for the given collection.  These URLs
     * should not be considered persistent, but will remain consistent
     * unless configuration changes are made to DSpace
     *
     * @param collection the collection to query
     * @return The Deposit URL
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getDepositLocation(Collection collection)
        throws DSpaceSWORDException {
        return this.getBaseDepositUrl() + "/" + collection.getHandle();
    }

    /**
     * Obtain the deposit URL for the given item.  These URLs
     * should not be considered persistent, but will remain consistent
     * unless configuration changes are made to DSpace
     *
     * @param item the item to query
     * @return The Deposit URL
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getDepositLocation(Item item)
        throws DSpaceSWORDException {
        return this.getBaseDepositUrl() + "/" + item.getHandle();
    }

    /**
     * Obtain the deposit URL for the given community.  These URLs
     * should not be considered persistent, but will remain consistent
     * unless configuration changes are made to DSpace
     *
     * @param community the community to query
     * @return The Deposit URL
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getDepositLocation(Community community)
        throws DSpaceSWORDException {
        // FIXME: there is no deposit url for communities yet, so this could
        // be misleading
        return this.getBaseDepositUrl() + "/" + community.getHandle();
    }

    /**
     * Obtain the collection which is represented by the given
     * URL
     *
     * @param context  the DSpace context
     * @param location the URL to resolve to a collection
     * @return The collection to which the url resolves
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     * @throws SWORDErrorException  on generic SWORD exception
     */
    // FIXME: we need to generalise this to DSpaceObjects, so that we can support
    // Communities, Collections and Items separately
    public Collection getCollection(Context context, String location)
        throws DSpaceSWORDException, SWORDErrorException {
        try {
            String baseUrl = this.getBaseDepositUrl();
            if (baseUrl.length() == location.length()) {
                throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL,
                                              "The deposit URL is incomplete");
            }
            String handle = location.substring(baseUrl.length());
            if (handle.startsWith("/")) {
                handle = handle.substring(1);
            }
            if ("".equals(handle)) {
                throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL,
                                              "The deposit URL is incomplete");
            }

            DSpaceObject dso = handleService.resolveToObject(context, handle);

            if (!(dso instanceof Collection)) {
                throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL,
                                              "The deposit URL does not resolve to a valid collection");
            }

            return (Collection) dso;
        } catch (SQLException e) {
            // log.error("Caught exception:", e);
            throw new DSpaceSWORDException(
                "There was a problem resolving the collection", e);
        }
    }

    /**
     * Obtain the collection which is represented by the given
     * URL
     *
     * @param context  the DSpace context
     * @param location the URL to resolve to a collection
     * @return The collection to which the url resolves
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     * @throws SWORDErrorException  on generic SWORD exception
     */
    public DSpaceObject getDSpaceObject(Context context, String location)
        throws DSpaceSWORDException, SWORDErrorException {
        try {
            String baseUrl = this.getBaseDepositUrl();
            if (baseUrl.length() == location.length()) {
                throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL,
                                              "The deposit URL is incomplete");
            }
            String handle = location.substring(baseUrl.length());
            if (handle.startsWith("/")) {
                handle = handle.substring(1);
            }
            if ("".equals(handle)) {
                throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL,
                                              "The deposit URL is incomplete");
            }

            DSpaceObject dso = handleService.resolveToObject(context, handle);

            if (!(dso instanceof Collection) && !(dso instanceof Item)) {
                throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL,
                                              "The deposit URL does not resolve to a valid deposit target");
            }

            return dso;
        } catch (SQLException e) {
            // log.error("Caught exception:", e);
            throw new DSpaceSWORDException(
                "There was a problem resolving the collection", e);
        }
    }

    /**
     * Construct the service document URL for the given object, which will
     * be supplied in the sword:service element of other service document
     * entries.
     *
     * @param community target community
     * @return service document URL for the given object
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public String constructSubServiceUrl(Community community)
        throws DSpaceSWORDException {
        String base = this.getBaseServiceDocumentUrl();
        String handle = community.getHandle();
        return base + "/" + handle;
    }

    /**
     * Construct the service document URL for the given object, which will
     * be supplied in the sword:service element of other service document
     * entries.
     *
     * @param collection target collection
     * @return service document URL for the given object
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public String constructSubServiceUrl(Collection collection)
        throws DSpaceSWORDException {
        String base = this.getBaseServiceDocumentUrl();
        String handle = collection.getHandle();
        return base + "/" + handle;
    }

    /**
     * Extract a DSpaceObject from the given URL.  If this method is unable to
     * locate a meaningful and appropriate DSpace object it will throw the
     * appropriate SWORD error.
     *
     * @param url URL to get DSpace object from
     * @return DSpace object corresponding to given URL
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     * @throws SWORDErrorException  on generic SWORD exception
     */
    public DSpaceObject extractDSpaceObject(String url)
        throws DSpaceSWORDException, SWORDErrorException {
        try {
            String sdBase = this.getBaseServiceDocumentUrl();
            String mlBase = this.getBaseMediaLinkUrl();

            if (url.startsWith(sdBase)) {
                // we are dealing with a service document request

                // first, let's find the beginning of the handle
                url = url.substring(sdBase.length());
                if (url.startsWith("/")) {
                    url = url.substring(1);
                }
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }

                DSpaceObject dso = handleService.resolveToObject(context, url);
                if (dso instanceof Collection || dso instanceof Community) {
                    return dso;
                } else {
                    throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL,
                                                  "Service Document request does not refer to a DSpace Collection or " +
                                                      "Community");
                }
            } else if (url.startsWith(mlBase)) {
                // we are dealing with a bitstream media link

                // find the index of the "/bitstream/" segment of the url
                int bsi = url.indexOf("/bitstream/");

                // subtsring the url from the end of this "/bitstream/" string, to get the bitstream id
                String bsid = url.substring(bsi + 11);

                // strip off extraneous slashes
                if (bsid.endsWith("/")) {
                    bsid = bsid.substring(0, url.length() - 1);
                }
                return bitstreamService.findByIdOrLegacyId(context, bsid);
            } else {
                throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL,
                                              "Unable to recognise URL as a valid service document: " +
                                                  url);
            }
        } catch (SQLException e) {
            throw new DSpaceSWORDException(e);
        }
    }

    /**
     * Get the base URL for service document requests.
     *
     * @return the base URL for service document requests
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getBaseServiceDocumentUrl()
        throws DSpaceSWORDException {
        String depositUrl = configurationService.getProperty(
            "sword-server.servicedocument.url");
        if (depositUrl == null || "".equals(depositUrl)) {
            if (dspaceUrl == null || "".equals(dspaceUrl)) {
                throw new DSpaceSWORDException(
                    "Unable to construct service document urls, due to missing/invalid " +
                        "config in sword.servicedocument.url and/or dspace.server.url");
            }
            depositUrl = buildSWORDUrl("servicedocument");
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
     * [dspace.server.url]/sword/deposit
     *
     * where dspace.server.url is also in the configuration file.
     *
     * @return the base URL for SWORD deposit
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getBaseDepositUrl()
        throws DSpaceSWORDException {
        String depositUrl = configurationService.getProperty(
            "sword-server.deposit.url");
        if (depositUrl == null || "".equals(depositUrl)) {
            if (dspaceUrl == null || "".equals(dspaceUrl)) {
                throw new DSpaceSWORDException(
                    "Unable to construct deposit urls, due to missing/invalid config in " +
                        "sword.deposit.url and/or dspace.server.url");
            }
            depositUrl =  buildSWORDUrl("deposit");
        }
        return depositUrl;
    }

    /**
     * Is the given URL the base service document URL?
     *
     * @param url URL to check
     * @return true if the given URL the base service document URL
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public boolean isBaseServiceDocumentUrl(String url)
        throws DSpaceSWORDException {
        return this.getBaseServiceDocumentUrl().equals(url);
    }

    /**
     * Is the given URL the base media link URL?
     *
     * @param url URL to check
     * @return true if the given URL the base media link URL
     * @throws DSpaceSWORDException passed through.
     */
    public boolean isBaseMediaLinkUrl(String url)
        throws DSpaceSWORDException {
        return this.getBaseMediaLinkUrl().equals(url);
    }

    /**
     * Central location for constructing usable URLs for DSpace bitstreams.
     * There is no place in the main DSpace code base for doing this.
     *
     * @param bitstream target bitstream
     * @return a URL to the given Bitstream.
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getBitstreamUrl(Bitstream bitstream)
        throws DSpaceSWORDException {
        try {
            List<Bundle> bundles = bitstream.getBundles();
            Bundle parent = null;
            if (!bundles.isEmpty()) {
                parent = bundles.get(0);
            } else {
                throw new DSpaceSWORDException(
                    "Encountered orphaned bitstream");
            }

            List<Item> items = parent.getItems();
            Item item;
            if (!items.isEmpty()) {
                item = items.get(0);
            } else {
                throw new DSpaceSWORDException("Encountered orphaned bundle");
            }

            String handle = item.getHandle();
            String bsLink = configurationService.getProperty("dspace.ui.url");

            if (handle != null && !"".equals(handle)) {
                bsLink = bsLink + "/bitstream/" + handle + "/" +
                    bitstream.getSequenceID() + "/" + bitstream.getName();
            } else {
                bsLink = bsLink + "/retrieve/" + bitstream.getID();
            }

            return bsLink;
        } catch (SQLException e) {
            throw new DSpaceSWORDException(e);
        }
    }

    /**
     * Get the base media link URL.  It can be configured using
     * {@code sword-server.media-link.url}.  If not configured, it will be
     * calculated using {@code dspace.server.url} and the constant path
     * {@code /sword/media-link}.
     *
     * @return that URL.
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getBaseMediaLinkUrl()
        throws DSpaceSWORDException {
        String mlUrl = configurationService.getProperty(
            "sword-server", "media-link.url");
        if (StringUtils.isBlank(mlUrl)) {
            if (dspaceUrl == null || "".equals(dspaceUrl)) {
                throw new DSpaceSWORDException(
                    "Unable to construct media-link urls, due to missing/invalid config in " +
                        "media-link.url and/or dspace.server.url");
            }
            mlUrl = buildSWORDUrl("media-link");
        }
        return mlUrl;
    }

    /**
     * get the media link URL for the given item
     *
     * @param dso target DSpace object
     * @return media link URL for the given item
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    private String getMediaLink(Item dso)
        throws DSpaceSWORDException {
        String ml = this.getBaseMediaLinkUrl();
        String handle = dso.getHandle();
        if (handle != null) {
            ml = ml + "/" + dso.getHandle();
        }
        return ml;
    }

    /**
     * Get the media link URL for the given bitstream.
     *
     * @param bitstream target bitstream
     * @return media link URL for the given bitstream
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getMediaLink(Bitstream bitstream)
        throws DSpaceSWORDException {
        try {
            List<Bundle> bundles = bitstream.getBundles();
            Bundle parent = null;
            if (!bundles.isEmpty()) {
                parent = bundles.get(0);
            } else {
                throw new DSpaceSWORDException(
                    "Encountered orphaned bitstream");
            }

            List<Item> items = parent.getItems();
            Item item;
            if (!items.isEmpty()) {
                item = items.get(0);
            } else {
                throw new DSpaceSWORDException("Encountered orphaned bundle");
            }

            String itemUrl = this.getMediaLink(item);
            if (itemUrl.equals(this.getBaseMediaLinkUrl())) {
                return itemUrl;
            }

            return itemUrl + "/bitstream/" + bitstream.getID();
        } catch (SQLException e) {
            throw new DSpaceSWORDException(e);
        }
    }

    /**
     * Return configured server path for SWORD url
     *
     * @param path the target SWORD endpoint
     * @return a sword URL
     */
    private String buildSWORDUrl(String path) {
        return dspaceUrl + "/" + swordPath + "/" + path;
    }
}
