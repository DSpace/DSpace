/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics.clarin;

import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.matomo.java.tracking.CustomVariable;
import org.matomo.java.tracking.MatomoException;
import org.matomo.java.tracking.MatomoRequest;

/**
 * Customized implementation of the ClarinMatomoTracker for the tracking the OAI harvesting events
 *
 * The class is copied from UFAL/CLARIN-DSPACE (https://github.com/ufal/clarin-dspace) and modified by
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinMatomoOAITracker extends ClarinMatomoTracker {
    /** log4j category */
    private static Logger log = Logger.getLogger(ClarinMatomoOAITracker.class);

    private final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Site ID for the OAI harvesting statistics
     */
    private int siteId;

    public ClarinMatomoOAITracker() {
        super();
        siteId = configurationService.getIntProperty("matomo.tracker.oai.site_id");
    }

    /**
     * Customize the matomo request parameters
     *
     * @param matomoRequest with the default parameters
     * @param request current request
     */
    @Override
    protected void preTrack(Context context, MatomoRequest matomoRequest, Item item, HttpServletRequest request) {
        super.preTrack(context, matomoRequest, item, request);

        matomoRequest.setSiteId(siteId);
        log.debug("Logging to site " + matomoRequest.getSiteId());
        try {
            matomoRequest.setPageCustomVariable(new CustomVariable("source", "oai"), 1);
        } catch (MatomoException e) {
            log.error(e);
        }
    }

    /**
     * Create the Matomo Request with updated actionURL
     *
     * @param context DSpace context object  - can be null
     * @param request current request
     * @param item downloading item - can be null
     * @param pageName - action name
     */
    @Override
    public void trackPage(Context context, HttpServletRequest request, Item item, String pageName) {
        pageName = expandPageName(request, pageName);
        log.debug("Matomo tracks " + pageName);
        String pageURL = getFullURL(request);

        MatomoRequest matomoRequest = createMatomoRequest(request, pageName, pageURL);
        if (Objects.isNull(matomoRequest)) {
            return;
        }

        // Add some headers and parameters to the request
        preTrack(context, matomoRequest, item, request);
        sendTrackingRequest(matomoRequest);
    }

    /**
     * Add the metadata prefix to the end of the action name e.g., add `/cmdi` metadata prefix to the name end
     *
     * @param request current request
     * @param pageName action name
     * @return
     */
    private String expandPageName(HttpServletRequest request, String pageName) {
        String[] metadataPrefix = request.getParameterValues("metadataPrefix");
        if (metadataPrefix != null && metadataPrefix.length > 0) {
            pageName = pageName + "/" + metadataPrefix[0];
        }
        return pageName;
    }

    /**
     * Track the harvesting event to the Matomo statistics
     *
     * @param request current request
     */
    public void trackOAIStatistics(HttpServletRequest request) {
        trackPage(null, request, null, "Repository OAI-PMH Data Provider Endpoint");
    }
}
