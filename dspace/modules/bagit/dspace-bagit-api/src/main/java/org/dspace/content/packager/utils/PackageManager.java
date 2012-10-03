package org.dspace.content.packager.utils;

import java.sql.SQLException;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.packager.utils.PackageManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME: This bit of code is reused in different modules; we need to move it
 * into a base custom-api module or something.
 * 
 * @author Kevin S. Clarke <ksclarke@gmail.com>
 * 
 */
@SuppressWarnings("deprecation")
public class PackageManager {

	private static final Logger log = LoggerFactory
			.getLogger(PackageManager.class);

	private PackageManager() {
	}

	/**
	 * Pass in a handle of a data package or a data file and we return the
	 * DSpace Item for the data package; otherwise, returns null. This is
	 * specific to Dryad's concept of data packages and data files.
	 * 
	 * @param aContext Current DSpace context
	 * @param aHandle A DSpace handle in the form <code>10255/dryad.230</code>
	 * @return The DSpaceObject for the data package
	 * @throws SQLException If there is a problem interacting with the database
	 */
	public static Item resolveToDataPackage(Context aContext,
			String aHandle) throws SQLException {
		Item item = (Item) HandleManager.resolveToObject(aContext, aHandle);
		String prefix = ConfigurationManager.getProperty("handle.prefix");
		String resolver = ConfigurationManager
				.getProperty("handle.canonical.prefix");

		if (resolver.endsWith("/")) {
			resolver += (prefix + "/");
		}
		else {
			resolver += ("/" + prefix + "/");
		}

		// If we have a dc.relation.haspart we are a data package; so return
		if (!(item.getMetadata("dc.relation.haspart").length > 0)) {
			DCValue[] dcValues = item.getMetadata("dc.relation.ispartof");

			// Otherwise, check to see if we have a data package relationship
			if (dcValues.length > 0) {
				String handle = dcValues[0].value;
				int start;

				if (handle.startsWith(resolver)) {
					if (log.isDebugEnabled()) {
						log.debug("Processing canonical handle: " + handle);
					}

					handle = prefix + handle.substring(resolver.length() - 1);
				}
				else if ((start = handle.indexOf("/handle/")) != -1) {
					if (log.isDebugEnabled()) {
						log.debug("Processing local handle: " + handle);
					}

					handle = handle.substring(start + 8); // length of /handle/
				}

				item = (Item) HandleManager.resolveToObject(aContext, handle);
			}
			else {
				if (log.isDebugEnabled()) {
					log.debug("Didn't find a data package for " + aHandle);
				}

				item = null;
			}
		}

		return item;
	}
}
