/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;

import org.purl.sword.base.Deposit;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.ErrorCodes;

/**
 * Factory class which will mint objects conforming to the
 * SWORDIngester interface.
 *
 * @author Richard Jones
 *
 */
public class SWORDIngesterFactory
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
     *     deposit request
     * @param dso
     *     target DSpace object
     * @return SWORD ingester implementation
     * @throws DSpaceSWORDException
     *     can be thrown by the internals of the DSpace SWORD implementation
     * @throws SWORDErrorException on generic SWORD exception
     */
    public static SWORDIngester getInstance(Context context, Deposit deposit,
            DSpaceObject dso)
            throws DSpaceSWORDException, SWORDErrorException
    {
        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

        if (dso instanceof Collection)
        {
            SWORDIngester ingester = (SWORDIngester) pluginService
                .getNamedPlugin(SWORDIngester.class, deposit.getPackaging());
            if (ingester == null)
            {
                throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT,
                    "No ingester configured for this package type");
            }
            return ingester;
        }
        else if (dso instanceof Item)
        {
            SWORDIngester ingester = (SWORDIngester) pluginService
                .getNamedPlugin(SWORDIngester.class, "SimpleFileIngester");
            if (ingester == null)
            {
                throw new DSpaceSWORDException(
                    "SimpleFileIngester is not configured in plugin manager");
            }
            return ingester;
        }

        throw new DSpaceSWORDException(
            "No ingester could be found which works for this DSpace Object");
    }
}
