/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.piwik.PiwikException;

public class PiwikOAITracker extends PiwikTracker
{
    /** log4j category */
    private static Logger log = Logger.getLogger(TrackerFactory.class);

    @Override
    protected void preTrack(HttpServletRequest request)
    {
        super.preTrack(request);
        tracker.setIdSite(getIdSite());
        try
        {
            tracker.setPageCustomVariable("source", "oai");
        }
        catch (PiwikException e)
        {
            log.error(e);
        }
    }

    @Override
    public void trackPage(HttpServletRequest request, String pageName)
    {
        pageName = expandPageName(request, pageName);
        String pageURL = getFullURL(request);
        tracker.setPageUrl(pageURL);

        preTrack(request);
        URL url = tracker.getPageTrackURL(pageName);
        sendTrackingRequest(url);
    }

    private String expandPageName(HttpServletRequest request, String pageName)
    {
        String[] metadataPrefix = request.getParameterValues("metadataPrefix");
        if(metadataPrefix != null && metadataPrefix.length > 0) {
            pageName = pageName + "/" + metadataPrefix[0];
        }
        return pageName;
    }

    private int getIdSite()
    {
        return ConfigurationManager.getIntProperty("lr",
                "lr.tracker.oai.site_id");
    }
}
