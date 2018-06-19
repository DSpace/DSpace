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

import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;
import org.dspace.services.model.Event;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serialize {@link UsageEvent} data to a file as Tab delimited. In dspace.cfg
 * specify the path to the file as the value of
 * {@code usageEvent.tabFileLogger.file}.  If that path is not absolute, it
 * will be interpreted as relative to the directory named in {@code log.dir}.
 * If no name is configured, it defaults to "usage-events.tsv".  If the file is
 * new or empty, a column heading record will be written when the file is opened.
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

    /** ISO 8601 Basic string format for record timestamps. */
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyyMMdd'T'HHmmssSSS");

    /** File on which to write event records. */
    private PrintWriter eventLog;

    /** Is this instance initialized? */
    private boolean initialized = false;

    /**
     * Set up a usage event listener for writing TSV records to a file.
     */
    private void init()
    {
        ConfigurationService configurationService
                = new DSpace().getConfigurationService();

        String logPath = configurationService.getPropertyAsType(
                "usageEvent.tabFileLogger.file",
                "usage-events.tsv");

        String logDir = null;
        if (!new File(logPath).isAbsolute())
        {
            logDir = configurationService.getProperty("log.report.dir");
        }

        File logFile = new File(logDir, logPath);
        try
        {
            eventLog = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile, true)));
            errorLog.debug("Writing to {}", logFile.getAbsolutePath());
        }
        catch (FileNotFoundException e)
        {
            errorLog.error("{} cannot open file, will not log events:  {}",
                            TabFileUsageEventListener.class.getName(),
                            e.getMessage());
            throw new IllegalArgumentException("Cannot open event log file", e);
        }

        if (logFile.length() <= 0)
        {
            eventLog.println("date"
                    + '\t' + "event"
                    + '\t' + "objectType"
                    + '\t' + "objectId"
                    + '\t' + "sessionId"
                    + '\t' + "sourceAddress"
                    + '\t' + "eperson");
        }

        initialized = true;
    }
    
    @Override
    public synchronized void receiveEvent(Event event)
    {
        if (!initialized)
            init();

        if (errorLog.isDebugEnabled())
            errorLog.debug("got: {}", event.toString());

        if(!(event instanceof UsageEvent))
            return;

        if (null == eventLog)
            return;

        UsageEvent ue = (UsageEvent)event;

        eventLog.append(dateFormat.format(new Date()))
            .append('\t').append(ue.getName()) // event type
            .append('\t').append(Constants.typeText[ue.getObject().getType()])
            .append('\t').append(ue.getObject().getID().toString())
            .append('\t').append(ue.getRequest().getSession().getId())
            .append('\t').append(ue.getRequest().getRemoteAddr());

        String epersonName = (null == ue.getContext().getCurrentUser()
                ? "anonymous"
                : ue.getContext().getCurrentUser().getEmail());
        eventLog.append('\t').append(epersonName);

        eventLog.println();
        eventLog.flush();
	}
}
