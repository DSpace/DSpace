/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.piwik.PiwikException;

public class PiwikBitstreamTracker extends PiwikTracker
{
    /** log4j category */
    private static Logger log = Logger.getLogger(TrackerFactory.class);

    @Override
    protected void preTrack(HttpServletRequest request) {
        super.preTrack(request);
        tracker.setIdSite(getIdSite());
        try
        {
            tracker.setPageCustomVariable("source", "bitstream");
        }
        catch (PiwikException e)
        {
            log.error(e);
        }
    }

    private int getIdSite() {
        return ConfigurationManager.getIntProperty("lr", "lr.tracker.bitstream.site_id");
    }
}
