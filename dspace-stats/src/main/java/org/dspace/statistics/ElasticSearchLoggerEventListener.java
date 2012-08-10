package org.dspace.statistics;

import org.apache.log4j.Logger;
import org.dspace.eperson.EPerson;
import org.dspace.services.model.Event;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;

public class ElasticSearchLoggerEventListener extends AbstractUsageEventListener {

    private static Logger log = Logger.getLogger(ElasticSearchLoggerEventListener.class);


    public void receiveEvent(Event event) {

        if(event instanceof UsageEvent)
        {
            try{

                UsageEvent ue = (UsageEvent) event;

                EPerson currentUser = ue.getContext() == null ? null : ue.getContext().getCurrentUser();

                ElasticSearchLogger.getInstance().post(ue.getObject(), ue.getRequest(), currentUser);
                log.info("Successfully logged " + ue.getObject().getTypeText() + "_" + ue.getObject().getID() + " " + ue.getObject().getName());
            }
            catch(Exception e)
            {
                log.error("General Exception: " + e.getMessage());
            }
        }
    }
}
