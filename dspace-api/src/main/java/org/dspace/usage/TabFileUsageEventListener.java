/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
            {
                logDir = ConfigurationManager.getProperty("log.dir");
            }

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
            {
                return;
            }
	
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
