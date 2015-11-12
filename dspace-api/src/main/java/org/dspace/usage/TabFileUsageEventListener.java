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

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.services.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serialize AbstractUsageEvent data to a file as Tab delimited. Requires
 * configuration:  in dspace.cfg specify the path to the file as the value of
 * {@code usageEvent.tabFileLogger.file}.
 * 
 * @author Mark H. Wood
 * @author Mark Diggory
 */
public class TabFileUsageEventListener
        extends AbstractUsageEventListener
{
    /** log category. */
    private static final Logger errorLog = LoggerFactory
            .getLogger(TabFileUsageEventListener.class);

    /** File on which to write event records. */
    private static PrintWriter log = null;

    /** Usual string format for dates. */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyyMMdd'T'HHmmssSSS");

    public TabFileUsageEventListener()
    {
        if (null == log)
        {
            boolean appending;

            String logPath = ConfigurationManager
                    .getProperty("usageEvent.tabFileLogger.file");
            if (null == logPath)
            {
                errorLog.error("{} unconfigured, will not log events",
                        TabFileUsageEventListener.class.getName());
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
                errorLog.error("{} cannot open file, will not log events:  {}",
                                TabFileUsageEventListener.class.getName(),
                                e.getMessage());
                return;
            }

            if (!appending)
            {
                log.println("date"
                        + '\t' + "event"
                        + '\t' + "objectType"
                        + '\t' + "objectId"
                        + '\t' + "sessionId"
                        + '\t' + "sourceAddress"
                        + '\t' + "eperson");
            }
        }
    }
    
    @Override
    public void receiveEvent(Event event) {
		System.out.println("got: " + event.toString());
		if(event instanceof UsageEvent)
		{
			UsageEvent ue = (UsageEvent)event;

			if (null == log)
            {
                return;
            }
	
            log.append(dateFormat.format(new Date()));
	        log.append('\t').append(ue.getName()); // event type
	        log.append('\t').append(Constants.typeText[ue.getObject().getType()]);
	        log.append('\t').append(ue.getObject().getID().toString());
	        log.append('\t').append(ue.getRequest().getSession().getId());
	        log.append('\t').append(ue.getRequest().getRequestURI());
	
	        String epersonName = (null == ue.getContext().getCurrentUser()
                    ? "anonymous"
                    : ue.getContext().getCurrentUser().getEmail());
	        log.append('\t').append(epersonName);
	
	        log.println();
	        log.flush();
		}
	}
}
