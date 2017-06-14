/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dspace.checker.factory.CheckerServiceFactory;
import org.dspace.checker.service.ChecksumHistoryService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * Manages the deletion of results from the checksum history. It uses the
 * dspace.cfg file as the default configuration file for the deletion settings
 * and can use a different configuration file if it is passed in.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 * 
 */
public final class ResultsPruner
{

    /**
     * Default logger.
     */
    private static final Logger LOG = Logger.getLogger(ResultsPruner.class);

    /**
     * Factory method for the default results pruner configuration using
     * dspace.cfg
     * 
     * @param context Context
     * @return a ResultsPruner that represent the default retention policy
     */
    public static ResultsPruner getDefaultPruner(Context context)
    {
        try
        {
            return getPruner(context, ConfigurationManager.getProperties());
        }
        catch (FileNotFoundException e)
        {
            throw new IllegalStateException(
                    "VeryExceptionalException - config file not there! ", e);
        }

    }

    
    /**
     * Factory method for ResultsPruners
     * 
     * @param context Context
     * @param propsFile
     *            to configure the results pruner.
     * @return the configured results pruner.
     * @throws FileNotFoundException if file doesn't exist
     *             it the configuration file cannot be found.
     */
    public static ResultsPruner getPruner(Context context, String propsFile)
            throws FileNotFoundException
    {
        Properties props = new Properties();
        FileInputStream fin = null;
        try
        {
            fin = new FileInputStream(propsFile);
            props.load(fin);
            
            return getPruner(context, props);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Problem loading properties file: "
                    + e.getMessage(), e);
        }
        finally
        {
            if (fin != null)
            {
                try
                {
                    fin.close();
                }
                catch (IOException e)
                {
                    LOG.warn(e);
                }
            }
        }
    }
    
    /**
     * Factory method for ResultsPruners (used to load ConfigurationManager
     * properties.
     * 
     * @param context Context
     * @param props Properties
     * @return pruner
     * @throws FileNotFoundException if file doesn't exist
     */
    public static ResultsPruner getPruner(Context context, Properties props)
    throws FileNotFoundException
    {
     
        ResultsPruner rp = new ResultsPruner(context);
        Pattern retentionPattern = Pattern
                .compile("checker\\.retention\\.(.*)");
        for (Enumeration<String> en = (Enumeration<String>)props.propertyNames(); en.hasMoreElements();)
        {
            String name = en.nextElement();
            Matcher matcher = retentionPattern.matcher(name);
            if (!matcher.matches())
            {
                continue;
            }
            String resultCode = matcher.group(1);
            long duration;
            try
            {
                duration = Utils.parseDuration(props.getProperty(name));
            }
            catch (ParseException e)
            {
                throw new IllegalStateException("Problem parsing duration: "
                        + e.getMessage(), e);
            }
            if ("default".equals(resultCode)) {
                rp.setDefaultDuration(duration);
            } else {
                ChecksumResultCode code = ChecksumResultCode.valueOf(resultCode);
                if (code == null) {
                    throw new IllegalStateException("Checksum result code not found: " + resultCode);
                }

                rp.addInterested(code, duration);
            }
        }
        return rp;
        
    }

    /** Ten years */
    private long defaultDuration = 31536000000L;

    /**
     * Map of retention durations, keyed by result code name
     */
    Map<ChecksumResultCode, Long> interests = new HashMap<>();


    /**
     * Checksum history database data access.
     */
    private ChecksumHistoryService checksumHistoryService = null;

    private Context context = null;

    /**
     * Default Constructor
     * @param context Context
     */
    public ResultsPruner(Context context)
    {
        this.checksumHistoryService = CheckerServiceFactory.getInstance().getChecksumHistoryService();
        this.context = context;
    }

    /**
     * Add a result and the length of time before the history with this type of
     * result is removed from the database.
     * 
     * @param result
     *            code in the database.
     * 
     * @param duration
     *            before bitstreams with the specified result type in the
     *            checksum history is removed.
     */
    public void addInterested(ChecksumResultCode result, long duration)
    {
        interests.put(result, duration);
    }

    /**
     * Add a result and the length of time before it is removed from the
     * checksum history table.
     * 
     * @param result
     *            code in the database.
     * 
     * @param duration
     *            before bitstreams with the specified result type in the
     *            checksum history is removed.
     * 
     * @throws ParseException if the duration cannot be parsed into a long value.
     */
    public void addInterested(ChecksumResultCode result, String duration)
            throws ParseException
    {
        addInterested(result, Utils.parseDuration(duration));
    }

    /**
     * The default amount of time before a result is removed.
     * 
     * @return the default duration.
     */
    public long getDefaultDuration()
    {
        return defaultDuration;
    }

    /**
     * Prunes the results retaining results as configured by the interests
     * registered with this object.
     * 
     * @return number of results removed.
     * @throws SQLException if database error
     */
    public int prune() throws SQLException {
        ChecksumResultCode[] codes = ChecksumResultCode.values();
        for (ChecksumResultCode code : codes)
    {
            if (!interests.containsKey(code))
            {
                interests.put(code, defaultDuration);
            }

        }
        int result = checksumHistoryService.prune(context, interests);
        return result;
    }

    /**
     * The default duration before records are removed from the checksum history
     * table.
     * 
     * @param defaultDuration
     *            used before records are removed from the checksum history.
     */
    public void setDefaultDuration(long defaultDuration)
    {
        this.defaultDuration = defaultDuration;
    }
}
