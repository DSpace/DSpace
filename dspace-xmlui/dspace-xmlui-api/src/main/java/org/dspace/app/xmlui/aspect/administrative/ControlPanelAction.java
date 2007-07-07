/*
 * ControlPanelAction.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/05/01 22:33:39 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
        	SystemwideAlerts.setMessage(message);
        
        if (countdown >= 0)
        {
        	// Convert from minutes to milliseconds;
        	countdown = countdown * 60 * 1000;
        	
        	// Figure out when the count down is.
        	long countDownTo = System.currentTimeMillis() + countdown;
        	
        	// set it.
        	SystemwideAlerts.setCountDownToo(countDownTo);
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
        
        return null;
    }

}
