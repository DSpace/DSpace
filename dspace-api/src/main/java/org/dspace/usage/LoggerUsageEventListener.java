/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.LogManager;
import org.dspace.services.model.Event;
import org.dspace.usage.UsageEvent.Action;

/**
 * 
 * @author Mark Diggory (mdiggory at atmire.com)
 *
 */
public class LoggerUsageEventListener extends AbstractUsageEventListener{

    /** log4j category */
    private static Logger log = Logger
            .getLogger(LoggerUsageEventListener.class);
    
	@Override
	public void receiveEvent(Event event) {
		
        //Search events are already logged
		//UsageSearchEvent is already logged in the search classes, no need to repeat this logging
		if(event instanceof UsageEvent && !(event instanceof UsageSearchEvent))
		{
			UsageEvent ue = (UsageEvent)event;

			log.info(LogManager.getHeader(
					ue.getContext(),
					formatAction(ue.getAction(), ue.getObject()),
					formatMessage(ue.getObject()))
					);
			
		}
	}

	private static String formatAction(Action action, DSpaceObject object)
	{
		try
		{
			String objText = Constants.typeText[object.getType()].toLowerCase();
			return action.text() + "_" + objText;
		}catch(Exception e)
		{
			
		}
		return "";
		
	}
	
	private static String formatMessage(DSpaceObject object)
	{
		try
		{
			String objText = Constants.typeText[object.getType()].toLowerCase();
			String handle = object.getHandle();
			
			/* Emulate Item logger */
			if(handle != null && object instanceof Item)
            {
                return "handle=" + object.getHandle();
            }
			else
            {
                return objText + "_id=" + object.getID();
            }

		}
        catch(Exception e)
		{
			
		}
		return "";
		
	}
}
