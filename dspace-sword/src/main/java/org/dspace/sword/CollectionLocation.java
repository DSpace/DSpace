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

import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

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
    public static final Logger log = Logger.getLogger(CollectionLocation.class);

    protected HandleService handleService = HandleServiceFactory.getInstance()
            .getHandleService();

    /**
     * Obtain the deposit URL for the given collection.  These URLs
     * should not be considered persistent, but will remain consistent
     * unless configuration changes are made to DSpace
     *
     * @param collection
     * @return The Deposit URL
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
     * @param context    the DSpace context
     * @param location    the URL to resolve to a collection
     * @return The collection to which the url resolves
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

            DSpaceObject dso = handleService.resolveToObject(context, handle);

            if (!(dso instanceof Collection))
            {
                throw new DSpaceSWORDException(
                        "The deposit URL does not resolve to a valid collection");
            }

            return (Collection) dso;
        }
        catch (SQLException e)
        {
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
     * [dspace.baseUrl]/sword/deposit
     *
     * where dspace.baseUrl is also in the configuration file.
     *
     * @return the base URL for sword deposit
     * @throws DSpaceSWORDException
     */
    private String getBaseUrl()
            throws DSpaceSWORDException
    {
        String depositUrl = ConfigurationManager
                .getProperty("sword-server", "deposit.url");
        if (depositUrl == null || "".equals(depositUrl))
        {
            String dspaceUrl = ConfigurationManager
                    .getProperty("dspace.baseUrl");
            if (dspaceUrl == null || "".equals(dspaceUrl))
            {
                throw new DSpaceSWORDException(
                        "Unable to construct deposit urls, due to missing/invalid config in sword.deposit.url and/or dspace.baseUrl");
            }

            try
            {
                URL url = new URL(dspaceUrl);
                depositUrl = new URL(url.getProtocol(), url.getHost(),
                        url.getPort(), "/sword/deposit").toString();
            }
            catch (MalformedURLException e)
            {
                throw new DSpaceSWORDException(
                        "Unable to construct deposit urls, due to invalid dspace.baseUrl " +
                                e.getMessage(), e);
            }

        }
        return depositUrl;
    }
}
