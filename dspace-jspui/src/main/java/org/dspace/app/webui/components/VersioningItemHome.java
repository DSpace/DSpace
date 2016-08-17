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
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.plugin.ItemHomeProcessor;
import org.dspace.plugin.PluginException;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;

public class VersioningItemHome implements ItemHomeProcessor {

    /** log4j category */
    private static Logger log = Logger.getLogger(VersioningItemHome.class);

    private DOIService doiService;
    private HandleService handleService;
    private IdentifierService identifierService;
    private VersionHistoryService versionHistoryService;
    private VersioningService versioningService;
    private ItemService itemService;    
    private ConfigurationService configurationService;
	
    public VersioningItemHome() {
        doiService = IdentifierServiceFactory.getInstance().getDOIService();
        handleService = HandleServiceFactory.getInstance().getHandleService();
        identifierService = IdentifierServiceFactory.getInstance().getIdentifierService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        versioningService = VersionServiceFactory.getInstance().getVersionService();
        versionHistoryService = VersionServiceFactory.getInstance().getVersionHistoryService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }
	

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item) throws PluginException,
            AuthorizeException {
        boolean versioningEnabled = configurationService
                .getPropertyAsType("versioning.enabled", false);
        boolean submitterCanCreateNewVersion = configurationService
            .getPropertyAsType("versioning.submitterCanCreateNewVersion", false);
        boolean newVersionAvailable = false;
        boolean showVersionWorkflowAvailable = false;
        boolean hasVersionButton = false;
        boolean hasVersionHistory = false;

        VersionHistory history = null;
        List<Version> historyVersions = new ArrayList<Version>();
        String latestVersionIdentifier = null;

        if (versioningEnabled) {
            try {
                if (itemService.canEdit(context, item)) {
                    if (versionHistoryService.isLastVersion(context, item) 
                            && item.isArchived()) {
                        hasVersionButton = true;
                    } 
                }
                else if (submitterCanCreateNewVersion) 
                {
                    if (versionHistoryService.isLastVersion(context, item) 
                            && item.isArchived() 
                            && itemService.canCreateNewVersion(context, item)) 
                        {
                            hasVersionButton = true;
                        }
                    
                }
                if (versionHistoryService.hasVersionHistory(context, item)) {
                    hasVersionHistory = true;
                    history = versionHistoryService.findByItem(context, item);
                    for (Version versRow : versioningService.getVersionsByHistory(context, history)) {
                        //Skip items currently in submission
                        if (VersionUtil.isItemInSubmission(context, versRow.getItem())) {
                            continue;
                        }
                        historyVersions.add(versRow);
                    }
                }
            } catch (SQLException e) {
                throw new PluginException(e.getMessage());
            }

            // Check if we have a history for the item
            Version latestVersion;
            try {
                latestVersion = VersionUtil.checkLatestVersion(context, item);
            } catch (SQLException e) {
                throw new PluginException(e.getMessage());
            }
            
            if (latestVersion != null
                    && latestVersion.getItem() != null
                    && !latestVersion.getItem().getID().equals(item.getID()))
            {
                // We have a newer version
                Item latestVersionItem = latestVersion.getItem();
                if (latestVersionItem.isArchived()) {
                    // Available, add a link for the user alerting him that
                    // a new version is available
                    newVersionAvailable = true;

                    // look up the the latest version handle
                    String latestVersionHandle = latestVersionItem.getHandle();
                    if (latestVersionHandle != null) {
                            latestVersionIdentifier = handleService.getCanonicalForm(latestVersionHandle);
                    }

                    // lookup the latest version doi
                    String latestVersionDOI = identifierService.lookup(
                            context, latestVersionItem, DOI.class);
                    if (latestVersionDOI != null)
                    {
                        try {
                            latestVersionDOI = doiService.DOIToExternalForm(latestVersionDOI);
                        } catch (IdentifierException ex)
                        {
                            log.error("Unable to convert DOI '" + latestVersionDOI 
                                    + "' into external form: " + ex.toString(), ex);
                            throw new PluginException(ex);
                        }
                    }

                    // do we prefer to use handle or DOIs?
                    if ("doi".equalsIgnoreCase(configurationService.getProperty("webui.preferred.identifier")))
                    {
                        if (latestVersionDOI != null)
                        {
                            latestVersionIdentifier = latestVersionDOI;
                        }
                    }
                } else {
                    // We might be dealing with a workflow/workspace item
                    showVersionWorkflowAvailable = true;
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
        request.setAttribute("versioning.latest_version_identifier",
                latestVersionIdentifier);

    }

}
