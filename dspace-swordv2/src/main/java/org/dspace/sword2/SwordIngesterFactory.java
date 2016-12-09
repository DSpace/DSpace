/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

/**
 * Factory class which will mint objects conforming to the
 * SWORDIngester interface.
 *
 * @author Richard Jones
 *
 */
public class SwordIngesterFactory
{
    /**
     * Generate an object which conforms to the SWORDIngester interface.
     * This Factory method may use the given DSpace context and the given
     * SWORD Deposit request to decide on the most appropriate implementation
     * of the interface to return.
     *
     * To configure how this method will respond, configure the package ingester
     * for the appropriate media types and defaults.  See the SWORD configuration
     * documentation for more details.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param deposit
     *     The original deposit request
     * @param dso
     *     target DSpace object
     * @return SWORDIngester object
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     * @throws SwordError
     *     if no suitable ingester is configured.
     */
    public static SwordContentIngester getContentInstance(Context context,
            Deposit deposit, DSpaceObject dso)
            throws DSpaceSwordException, SwordError
    {
        SwordContentIngester ingester = null;

        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

        // first look to see if there's an intester for the content type
        ingester = (SwordContentIngester) pluginService
            .getNamedPlugin(SwordContentIngester.class, deposit.getMimeType());
        if (ingester != null)
        {
            return ingester;
        }

        // if no ingester, then 
        // look to see if there's an ingester for the package format
        ingester = (SwordContentIngester) pluginService
            .getNamedPlugin(SwordContentIngester.class, deposit.getPackaging());
        if (ingester == null)
        {
            throw new SwordError(UriRegistry.ERROR_CONTENT,
                "No ingester configured for this package type");
        }
        return ingester;
    }

    public static SwordEntryIngester getEntryInstance(Context context,
            Deposit deposit, DSpaceObject dso)
            throws DSpaceSwordException, SwordError
    {
        SwordEntryIngester ingester = (SwordEntryIngester) CoreServiceFactory.getInstance().getPluginService()
            .getSinglePlugin(SwordEntryIngester.class);
        if (ingester == null)
        {
            throw new SwordError(UriRegistry.ERROR_CONTENT,
                "No ingester configured for handling SWORD entry documents");
        }
        return ingester;
    }
}
