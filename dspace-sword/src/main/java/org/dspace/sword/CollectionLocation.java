/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This class provides a single point of contact for
 * resolving Collections from SWORD Deposit URLs and for
 * generating SWORD Deposit URLs from Collections
 *
 * @author Richard Jones
 */
public class CollectionLocation {
    /**
     * Log4j logger
     */
    public static final Logger log = LogManager.getLogger(CollectionLocation.class);

    protected HandleService handleService = HandleServiceFactory.getInstance()
                                                                .getHandleService();
    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Obtain the deposit URL for the given collection.  These URLs
     * should not be considered persistent, but will remain consistent
     * unless configuration changes are made to DSpace
     *
     * @param collection collection to query
     * @return The Deposit URL
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public String getLocation(Collection collection)
        throws DSpaceSWORDException {
        return this.getBaseUrl() + "/" + collection.getHandle();
    }

    /**
     * Obtain the collection which is represented by the given
     * URL
     *
     * @param context  the DSpace context
     * @param location the URL to resolve to a collection
     * @return The collection to which the url resolves
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    public Collection getCollection(Context context, String location)
        throws DSpaceSWORDException {
        try {
            String baseUrl = this.getBaseUrl();
            if (baseUrl.length() == location.length()) {
                throw new DSpaceSWORDException("The deposit URL is incomplete");
            }
            String handle = location.substring(baseUrl.length());
            if (handle.startsWith("/")) {
                handle = handle.substring(1);
            }
            if ("".equals(handle)) {
                throw new DSpaceSWORDException("The deposit URL is incomplete");
            }

            DSpaceObject dso = handleService.resolveToObject(context, handle);

            if (!(dso instanceof Collection)) {
                throw new DSpaceSWORDException(
                    "The deposit URL does not resolve to a valid collection");
            }

            return (Collection) dso;
        } catch (SQLException e) {
            log.error("Caught exception:", e);
            throw new DSpaceSWORDException(
                "There was a problem resolving the collection", e);
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
     * [dspace.server.url]/sword/deposit
     *
     * where dspace.server.url is also in the configuration file.
     *
     * @return the base URL for sword deposit
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    private String getBaseUrl()
        throws DSpaceSWORDException {
        String depositUrl = configurationService.getProperty(
            "sword-server.deposit.url");
        if (depositUrl == null || "".equals(depositUrl)) {
            String dspaceUrl = configurationService
                .getProperty("dspace.server.url");
            if (dspaceUrl == null || "".equals(dspaceUrl)) {
                throw new DSpaceSWORDException(
                    "Unable to construct deposit urls, due to missing/invalid config in sword.deposit.url and/or " +
                        "dspace.server.url");
            }

            try {
                URL url = new URL(dspaceUrl);
                depositUrl = new URL(url.getProtocol(), url.getHost(),
                                     url.getPort(), "/sword/deposit").toString();
            } catch (MalformedURLException e) {
                throw new DSpaceSWORDException(
                    "Unable to construct deposit urls, due to invalid dspace.server.url " +
                        e.getMessage(), e);
            }

        }
        return depositUrl;
    }
}
