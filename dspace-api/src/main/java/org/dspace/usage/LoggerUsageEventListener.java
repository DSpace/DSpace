/*
 * Version: $Revision: $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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
    
	public void receiveEvent(Event event) {
		
		if(event instanceof UsageEvent)
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
				return "handle=" + object.getHandle();
			else 
				return objText + "_id=" + object.getID();

		}catch(Exception e)
		{
			
		}
		return "";
		
	}
}
