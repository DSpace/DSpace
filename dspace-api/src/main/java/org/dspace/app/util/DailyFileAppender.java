/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;


import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Special log appender for log4j. Adds the current date (ie. year-mon) to
 * the end of the file name, so that rolling on to the next log is simply
 * a case of starting a new one - no renaming of old logs.
 *
 * This is advisable if you are using Windows, and have multiple applications
 * (ie. dspace, dspace-oai, dspace-sword) that all want to write to the same log file,
 * as each would otherwise try to rename the old files during rollover.
 *
 * An example log4j.properties (one log per month, retains three months of logs)
 *
 * log4j.rootCategory=INFO, A1
 * log4j.appender.A1=org.dspace.app.util.DailyFileAppender
 * log4j.appender.A1.File=@@log.dir@@/dspace.log
 * log4j.appender.A1.DatePattern=yyyy-MM
 * log4j.appender.A1.MaxLogs=3
 * log4j.appender.A1.layout=org.apache.log4j.PatternLayout
 * log4j.appender.A1.layout.ConversionPattern=%d %-5p %c @ %m%n
 *
 */
public class DailyFileAppender extends FileAppender
{
    /**
     * The fixed date pattern to be used if one is not specified.
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * The folder under which daily folders are created. This can be a absolute path
     * or relative path also.
     * e.g. JavaLogs/CPRILog or F:/LogFiles/CPRILog
     */
    private String mstrFileName;

    /**
     * Used internally and contains the name of the date derived from current system date.
     */
    private Date   mstrDate = new Date(System.currentTimeMillis());

    /**
     * Holds the user specified DatePattern,
     */
    private String mstrDatePattern = DATE_PATTERN;

    private boolean mMonthOnly = false;

    /**
     * The date formatter object used for parsing the user specified DatePattern.
     */
    private SimpleDateFormat mobjSDF;

    private boolean mWithHostName = false;

    private int mMaxLogs = 0;

    /**
     * Default constructor. This is required as the appender class is dynamically
     * loaded.
     */
    public DailyFileAppender()
    {
        super();
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.FileAppender#activateOptions()
     */
    @Override
    public void activateOptions()
    {
        setFileName();
        cleanupOldFiles();
        super.activateOptions();
    }

/*------------------------------------------------------------------------------
 * Getters
 *----------------------------------------------------------------------------*/
    public String getDatePattern()
    {
        return this.mstrDatePattern;
    }

    @Override
    public String getFile()
    {
        return this.mstrFileName;
    }

    public boolean getWithHost()
    {
        return mWithHostName;
    }

    public int getMaxLogs()
    {
    	return mMaxLogs;
    }

/*------------------------------------------------------------------------------
 * Setters
 *----------------------------------------------------------------------------*/
    public void setDatePattern(String pstrPattern)
    {
        this.mstrDatePattern = checkPattern(pstrPattern);
        if (mstrDatePattern.contains("dd") || mstrDatePattern.contains("DD"))
        {
            mMonthOnly = false;
        }
        else
        {
            mMonthOnly = true;
        }
    }

    @Override
    public void setFile(String file)
    {
        // Trim spaces from both ends. The users probably does not want
        // trailing spaces in file names.
        String val = file.trim();
        mstrFileName = val;
     }

    public void setWithHost(boolean wh)
    {
        mWithHostName = wh;
    }

    public void setMaxLogs(int ml)
    {
    	mMaxLogs = ml;
    }

/*------------------------------------------------------------------------------
 * Methods
 *----------------------------------------------------------------------------*/
    /* (non-Javadoc)
     * @see org.apache.log4j.WriterAppender#subAppend(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void subAppend(LoggingEvent pobjEvent)
    {
        Date   dtNow = new Date(System.currentTimeMillis());

        boolean rollover = false;

        if (mMonthOnly)
        {
            Calendar now = Calendar.getInstance();
            Calendar cur = Calendar.getInstance();
            now.setTime(dtNow);
            cur.setTime(mstrDate);
            rollover = !(now.get(Calendar.YEAR) == cur.get(Calendar.YEAR) && now.get(Calendar.MONTH) == cur.get(Calendar.MONTH));
        }
        else
        {
            rollover = !(DateUtils.isSameDay(dtNow, mstrDate));
        }

        if (rollover)
        {
            try
            {
                rollOver(dtNow);
            }
            catch (IOException IOEx)
            {
                LogLog.error("rollOver() failed!", IOEx);
            }
        }

        super.subAppend(pobjEvent);
    }

/*------------------------------------------------------------------------------
 * Helpers
 *----------------------------------------------------------------------------*/
    /**
     * The helper function to validate the DatePattern.
     * @param pstrPattern The DatePattern to be validated.
     * @return The validated date pattern or defautlt DATE_PATTERN
     */
    private String checkPattern(String pstrPattern)
    {
        String strRet = null;
        SimpleDateFormat objFmt = new SimpleDateFormat(DATE_PATTERN);

        try
        {
            this.mobjSDF = new SimpleDateFormat(pstrPattern);
            strRet = pstrPattern;
        }
        catch (NullPointerException NPExIgnore)
        {
            LogLog.error("Invalid DatePattern " + pstrPattern, NPExIgnore);
            this.mobjSDF = objFmt;
            strRet = DATE_PATTERN;
        }
        catch (IllegalArgumentException IlArgExIgnore)
        {
            LogLog.error("Invalid DatePattern " + pstrPattern, IlArgExIgnore);
            this.mobjSDF = objFmt;
            strRet = DATE_PATTERN;
        }
        finally
        {
            objFmt = null;
        }
        return strRet;
    }

    /**
     * This function is responsible for performing the actual file rollover.
     * @param pstrName The name of the new folder based on current system date.
     * @throws IOException if IO error
     */
    private static boolean deletingFiles = false;
    private void cleanupOldFiles()
    {
        // If we need to delete log files
        if (mMaxLogs > 0 && !deletingFiles)
        {
			deletingFiles = true;

			// Determine the final file extension with the hostname
    		String hostFileExt = null;
            try
            {
                hostFileExt = "." + java.net.InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException e)
            {
                LogLog.error("Unable to retrieve host name");
            }

			try
			{
				// Array to hold the logs we are going to keep
				File[] logsToKeep = new File[mMaxLogs];

				// Get a 'master' file handle, and the parent directory from it
		        File logMaster = new File(mstrFileName);
		        File logDir = logMaster.getParentFile();
		        if (logDir.isDirectory())
		        {
		        	// Iterate all the files in that directory
		        	File[] logArr = logDir.listFiles();
		        	for (File curLog : logArr)
		        	{
        				LogLog.debug("Comparing '" + curLog.getAbsolutePath() + "' to '" + mstrFileName + "'");
		        		String name = curLog.getAbsolutePath();

		        		// First, see if we are not using hostname, or the log file ends with this host
		                if (!mWithHostName || (hostFileExt != null && name.endsWith(hostFileExt)))
		                {
		                	// Check that the file is indeed one we want (contains the master file name)
			        		if (name.contains(mstrFileName))
			        		{
			        			// Iterate through the array of logs we are keeping
			        			for (int i = 0; curLog != null && i < logsToKeep.length; i++)
			        			{
			        				// Have we exhausted the 'to keep' array?
			        				if (logsToKeep[i] == null)
			        				{
				        				// Empty space, retain this log file
			        					logsToKeep[i] = curLog;
			        					curLog = null;
			        				}
			        				// If the 'kept' file is older than the current one
			        				else if (logsToKeep[i].getName().compareTo(curLog.getName()) < 0)
			        				{
			        					// Replace tested entry with current file
			        					File temp = logsToKeep[i];
			        					logsToKeep[i] = curLog;
			        					curLog = temp;
			        				}
			        			}

			        			// If we have a 'current' entry at this point, it's a log we don't want
			        			if (curLog != null)
			        			{
			        				LogLog.debug("Deleting log " + curLog.getName());
		        					if (! curLog.delete())
                                    {
                                        LogLog.error("Unable to delete log file");
                                    }
			        			}
			        		}
		                }
		        	}
		        }
			}
			catch (Exception e)
			{
				// Don't worry about exceptions
			}
			finally
			{
				deletingFiles = false;
			}
        }
    }

    private void rollOver(Date dtNow) throws IOException
    {
        mstrDate = dtNow;
        setFileName();
        this.setFile(fileName, true, bufferedIO, bufferSize);

        cleanupOldFiles();
    }

    private void setFileName()
    {
        fileName = mstrFileName + "." + mobjSDF.format(mstrDate);

        if (mWithHostName)
        {
            try
            {
                fileName += "." + java.net.InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException e)
            {
                LogLog.error("Unable to retrieve host name");
            }
        }
    }
}