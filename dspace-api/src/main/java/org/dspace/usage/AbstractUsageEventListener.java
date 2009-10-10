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

import org.dspace.services.EventService;
import org.dspace.services.model.EventListener;

/**
 * AbstractUsageEventListener is used as the base class for listening events running
 * in the EventService.
 * 
 * @author Mark Diggory (mdiggory at atmire.com)
 * @version $Revision: $
 */
public abstract class AbstractUsageEventListener implements EventListener {

	public AbstractUsageEventListener() {
		super();
	}

	/**
	 * Empty String[] flags to have Listener 
	 * consume any event name prefixes.
	 */
	public String[] getEventNamePrefixes() {
		return new String[0];
	}

	/**
	 * Currently consumes events generated for
	 * all resources.
	 */
	public String getResourcePrefix() {
		return null;
	}

	public void setEventService(EventService service) throws Exception {
		if(service != null)
			service.registerEventListener(this);
		else
			throw new RuntimeException("EventService handed to Listener cannot be null");
	
	}
	
}