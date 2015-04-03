/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import org.apache.log4j.Logger;

public class PiwikTrackerFactory
{
    /** log4j category */
    private static Logger log = Logger.getLogger(PiwikTrackerFactory.class);

    public static Tracker createInstance(TrackingSite site)
    {                
        switch(site) {
            case OAI:
                return new PiwikOAITracker();                
            case BITSTREAM:
                return new PiwikBitstreamTracker();                                          
        }       
        throw new IllegalArgumentException("Unknown site: " + site);
    }
}
