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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import org.dspace.core.ConfigurationManager;
import org.dspace.services.model.Event;

/**
 * Serialize AbstractUsageEvent data to a file as Tab deliminated. Requires
 * configuration:  in dspace.cfg specify the path to the file as the value of
 * {@code usageEvent.tabFileLogger.file}.
 * 
 * @author Mark H. Wood
 * @author Mark Diggory
 * @version $Revision: 3734 $
 */
public class TabFileUsageEventListener extends AbstractUsageEventListener
{
	
    /** log4j category */
    private static Logger errorLog = Logger
            .getLogger(TabFileUsageEventListener.class);

    /** File on which to write event records */
    static PrintWriter log = null;

    public TabFileUsageEventListener()
    {

        if (null == log)
        {
            boolean appending;

            String logPath = ConfigurationManager
                    .getProperty("usageEvent.tabFileLogger.file");
            if (null == logPath)
            {
                errorLog
                        .error("UsageEventTabFileLogger unconfigured, will not log events");
                return;
            }

            String logDir = null;
            if (!new File(logPath).isAbsolute())
                logDir = ConfigurationManager.getProperty("log.dir");

            File logFile = new File(logDir, logPath);
            appending = logFile.length() > 0;
            try
            {
                log = new PrintWriter(new OutputStreamWriter(
                        new FileOutputStream(logFile, true)));
            }
            catch (FileNotFoundException e)
            {
                errorLog
                        .error(
                                "UsageEventTabFileLogger cannot open file, will not log events",
                                e);
                return;
            }

            if (!appending)
            {
                log.println("date event objectType objectId sessionId sourceAddress eperson");
            }
            
        }
    }
    
    public void receiveEvent(Event event) {
		System.out.println("got: " + event.toString());
		if(event instanceof UsageEvent)
		{
			UsageEvent ue = (UsageEvent)event;

			if (null == log)
	            return;
	
	        SimpleDateFormat dateFormat = new SimpleDateFormat(
	                "yyyyMMdd'T'HHmmssSSS");
	
	        String string = dateFormat.format(new Date());
	        string += "\t" + ue.getName(); // event type
	        string += "\t" + ue.getObject().getType();
	        string += "\t" + ue.getObject().getID();
	        string += "\t" + ue.getRequest().getSession().getId();
	        string += "\t" + ue.getRequest().getRequestURI();
	
	        String epersonName = (null == ue.getContext().getCurrentUser() ? "anonymous" : ue.getContext().getCurrentUser().getEmail());
	        string += "\t" + epersonName;
	
	        log.println(string);
	        log.flush();
		}
	}
}
