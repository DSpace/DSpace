/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.VersionUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.plugin.ItemHomeProcessor;
import org.dspace.plugin.PluginException;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;

public class VersioningItemHome implements ItemHomeProcessor {

	/** log4j category */
	private static Logger log = Logger.getLogger(VersioningItemHome.class);

	@Override
	public void process(Context context, HttpServletRequest request,
			HttpServletResponse response, Item item) throws PluginException,
			AuthorizeException {
		boolean versioningEnabled = ConfigurationManager.getBooleanProperty(
				"versioning", "enabled");
		boolean newVersionAvailable = false;
		boolean showVersionWorkflowAvailable = false;
		boolean hasVersionButton = false;
		boolean hasVersionHistory = false;
		
		VersionHistory history = null;
		List<Version> historyVersions = new ArrayList<Version>();
		String latestVersionHandle = null;
		String latestVersionURL = null;
		if (versioningEnabled) {
			try {
				if(item.canEdit()) {
					if (VersionUtil.isLatest(context, item) && item.isArchived()) {
						hasVersionButton = true;
					}
				}
			} catch (SQLException e) {
				throw new PluginException(e.getMessage());
			}

			if (VersionUtil.hasVersionHistory(context, item)) {
				hasVersionHistory = true;
				history = VersionUtil.retrieveVersionHistory(context, item);
				for(Version versRow : history.getVersions()) {  
					
		            //Skip items currently in submission
		            try {
						if(VersionUtil.isItemInSubmission(context, versRow.getItem()))
						{
						    continue;
						}
						else {
							historyVersions.add(versRow);
						}
					} catch (SQLException e) {
						throw new PluginException(e.getMessage());
					}
				}
			}

			// Check if we have a history for the item
			Version latestVersion;
			try {
				latestVersion = VersionUtil.checkLatestVersion(context, item);
			} catch (SQLException e) {
				throw new PluginException(e.getMessage());
			}

			if (latestVersion != null) {
				if (latestVersion != null
						&& latestVersion.getItemID() != item.getID()) {
					// We have a newer version
					Item latestVersionItem = latestVersion.getItem();
					if (latestVersionItem.isArchived()) {
						// Available, add a link for the user alerting him that
						// a new version is available
						newVersionAvailable = true;
						try {
							latestVersionURL = HandleManager.resolveToURL(
									context, latestVersionItem.getHandle());
						} catch (SQLException e) {
							throw new PluginException(e.getMessage());
						}
						latestVersionHandle = latestVersionItem.getHandle();
					} else {
						// We might be dealing with a workflow/workspace item
						showVersionWorkflowAvailable = true;
					}
				}
			}
		}

		request.setAttribute("versioning.enabled", versioningEnabled);
		request.setAttribute("versioning.hasversionbutton", hasVersionButton);
		request.setAttribute("versioning.hasversionhistory", hasVersionHistory);
		request.setAttribute("versioning.history", history);
		request.setAttribute("versioning.historyversions", historyVersions);
		request.setAttribute("versioning.newversionavailable",
				newVersionAvailable);
		request.setAttribute("versioning.showversionwfavailable",
				showVersionWorkflowAvailable);
		request.setAttribute("versioning.latestversionhandle",
				latestVersionHandle);
		request.setAttribute("versioning.latestversionurl", latestVersionURL);

	}

}
