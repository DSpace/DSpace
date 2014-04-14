/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Collection;
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
	 * @param deposit
	 * @throws DSpaceSwordException
	 */
	public static SwordContentIngester getContentInstance(Context context, Deposit deposit, DSpaceObject dso)
            throws DSpaceSwordException, SwordError
    {
        SwordContentIngester ingester = null;

        // first look to see if there's an intester for the content type
        ingester = (SwordContentIngester) PluginManager.getNamedPlugin("swordv2-server", SwordContentIngester.class, deposit.getMimeType());
        if (ingester != null)
        {
            return ingester;
        }

        // if no ingester, then 
        // look to see if there's an ingester for the package format
        ingester = (SwordContentIngester) PluginManager.getNamedPlugin("swordv2-server", SwordContentIngester.class, deposit.getPackaging());
        if (ingester == null)
        {
            throw new SwordError(UriRegistry.ERROR_CONTENT, "No ingester configured for this package type");
        }
        return ingester;
	}

    public static SwordEntryIngester getEntryInstance(Context context, Deposit deposit, DSpaceObject dso)
            throws DSpaceSwordException, SwordError
    {
		SwordEntryIngester ingester = (SwordEntryIngester) PluginManager.getSinglePlugin("swordv2-server", SwordEntryIngester.class);
		if (ingester == null)
		{
			throw new SwordError(UriRegistry.ERROR_CONTENT, "No ingester configured for handling sword entry documents");
		}
		return ingester;
	}
}
