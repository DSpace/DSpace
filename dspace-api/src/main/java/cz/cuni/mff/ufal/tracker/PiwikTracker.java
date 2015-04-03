/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.piwik.PiwikException;
import org.piwik.SimplePiwikTracker;

public class PiwikTracker implements Tracker
{
    /** log4j category */
    private static Logger log = Logger.getLogger(PiwikTracker.class);

    protected SimplePiwikTracker tracker;

    PiwikTracker()
    {
        init();
    }

    protected void init()
    {
        String apiURL = ConfigurationManager.getProperty("lr",
                "lr.tracker.api.url");
        String authToken = ConfigurationManager.getProperty("lr",
                "lr.tracker.api.auth.token");

        try
        {
            tracker = new SimplePiwikTracker(apiURL);
            if (authToken != null && !authToken.isEmpty())
            {
                tracker.setTokenAuth(authToken);
            }
        }
        catch (PiwikException e)
        {
            log.error(e);
        }
    }

    public void trackPage(HttpServletRequest request, String pageName)
    {

        String pageURL = getFullURL(request);
        tracker.setPageUrl(pageURL);

        preTrack(request);
        URL url = tracker.getPageTrackURL(pageName);
        sendTrackingRequest(url);
    }

    public void trackDownload(HttpServletRequest request)
    {
        String downloadURL = getFullURL(request);

        preTrack(request);
        URL url = tracker.getDownloadTrackURL(downloadURL);
        sendTrackingRequest(url);
    }

    public void sendTrackingRequest(URL url)
    {
        try
        {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            int responseCode = conn.getResponseCode();

            if (responseCode != 200)
            {
                log.error("Invalid response code from Piwik tracker API: "
                        + responseCode);
            }

        }
        catch (IOException e)
        {
            log.error(e);
        }
    }

    protected String getFullURL(HttpServletRequest request)
    {
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme());
        url.append("://");
        url.append(request.getServerName());
        url.append("http".equals(request.getScheme())
                && request.getServerPort() == 80
                || "https".equals(request.getScheme())
                && request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
        url.append(request.getRequestURI());
        url.append(request.getQueryString() != null ? "?"
                + request.getQueryString() : "");
        return url.toString();
    }

    protected void preTrack(HttpServletRequest request)
    {
        tracker.setIp(getIpAddress(request));
    }

    protected String getIpAddress(HttpServletRequest request)
    {
        String ip = "";
        String header = request.getHeader("X-Forwarded-For");
        if(header == null) {
            header = request.getRemoteAddr();
        }
        if(header != null) {
            String[] ips = header.split(", ");
            ip = ips.length > 0 ? ips[0] : "";
        }
        return ip;
    }

}
