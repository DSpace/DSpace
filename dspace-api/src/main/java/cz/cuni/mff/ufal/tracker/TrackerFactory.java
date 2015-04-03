/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

public class TrackerFactory
{
    /** log4j category */
    private static Logger log = Logger.getLogger(TrackerFactory.class);

    public static Tracker createInstance(TrackingSite site)
    {
        String trackerType = ConfigurationManager
                .getProperty("lr", "lr.tracker.type");
        if (trackerType.equalsIgnoreCase("piwik"))
        {
            return PiwikTrackerFactory.createInstance(site);
        }
        throw new IllegalArgumentException("Invalid tracker type:" + trackerType);
    }
}
