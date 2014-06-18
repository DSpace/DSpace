/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.harvest.OAIHarvester;

/**
 * Update the alert system based upon the form submitted from the control panel.
 * 
 * @author Scott Phillips
 */

public class ControlPanelAction extends AbstractAction
{

    /**
     * Either activate or deactivate the alert system.
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        // In any case update the system-wide alert system
        String message = request.getParameter("message");
        String countdownString = request.getParameter("countdown");
        String restrictsessions = request.getParameter("restrictsessions");
        
        int countdown = -1;
        if (countdownString != null)
        {
        	try {
        		countdown = Integer.valueOf(countdownString);
        	} 
        	catch (NumberFormatException nfe)
        	{
        		// just ignore it.
        	}
        }
        
        
        // Update the message
        if (message != null)
        {
            SystemwideAlerts.setMessage(message);
        }
        
        if (countdown >= 0)
        {
        	// Convert from minutes to milliseconds;
        	countdown = countdown * 60 * 1000;
        	
        	// Figure out when the count down is.
        	long countDownTo = System.currentTimeMillis() + countdown;
        	
        	// set it.
        	SystemwideAlerts.setCountDownToo(countDownTo);
        }
        
        if (restrictsessions != null && restrictsessions.length() > 0)
        {
        	try {
        		int newState = Integer.valueOf(restrictsessions);
        		SystemwideAlerts.setRestrictSessions(newState);
        	} 
        	catch (NumberFormatException nfe)
        	{
        		// ignore it
        	}
        }
        
        
        
        if (request.getParameter("submit_activate") != null)
        {
        	SystemwideAlerts.activateAlert();
        	
        	// Ensure the alert is active for this request, return 
        	// a success so the sitemap can add the alert in.
        	return new HashMap();
        	
        }
        else if (request.getParameter("submit_deactivate") != null)
        {
        	SystemwideAlerts.deactivateAlert();
        }
        else if (request.getParameter("submit_harvest_start") != null) {
        	OAIHarvester.startNewScheduler();
        }
        else if (request.getParameter("submit_harvest_resume") != null) {
        	OAIHarvester.resumeScheduler();
        }
        else if (request.getParameter("submit_harvest_pause") != null) {
        	OAIHarvester.pauseScheduler();
        }
        else if (request.getParameter("submit_harvest_stop") != null) {
        	OAIHarvester.stopScheduler();
        }
        else if (request.getParameter("submit_harvest_reset") != null) {
        	OAIHarvester.resetScheduler();
        }

        return null;
    }

}
