/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;

/**
 * <p>
 * Collects results from a Checksum process and outputs them to a Log4j Logger.
 * </p>
 * 
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 * 
 */
public class ResultsLogger implements ChecksumResultsCollector
{
    /**
     * Usual Log4J logger.
     */
    private static final Logger LOG = Logger.getLogger(ResultsLogger.class);

    /**
     * Utility date format.
     */
    private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        }
    };
    /**
     * Date the current checking run started.
     */
    Date startDate = null;

    /**
     * Blanked off, no-op constructor. Do not use.
     */
    private ResultsLogger()
    {
    }

    /**
     * Main constructor.
     * 
     * @param startDt
     *            Date the checking run started.
     */
    public ResultsLogger(Date startDt)
    {
        LOG.info(msg("run-start-time") + ": " + DATE_FORMAT.get().format(startDt));
    }

    /**
     * Get the i18N string.
     * 
     * @param key
     *            to get the message.
     * @return the message found.
     */
    protected String msg(String key)
    {
        return I18nUtil.getMessage("org.dspace.checker.ResultsLogger." + key);
    }

    /**
     * Collect a result for logging.
     * 
     * @param context Context
     * @param info
     *            the BitstreamInfo representing the result.
     * @throws SQLException if database error
     * @see org.dspace.checker.ChecksumResultsCollector#collect(org.dspace.core.Context, org.dspace.checker.MostRecentChecksum)
     */
    @Override
    public void collect(Context context, MostRecentChecksum info) throws SQLException {
        Bitstream bitstream = info.getBitstream();
        LOG.info("******************************************************");
        LOG.info(msg("bitstream-id") + ": " + bitstream.getID());
        LOG.info(msg("bitstream-info-found") + ": " + info.isInfoFound());
        LOG.info(msg("bitstream-marked-deleted") + ": " + bitstream.isDeleted());
        LOG.info(msg("bitstream-found") + ": " + info.isBitstreamFound());
        LOG.info(msg("to-be-processed") + ": " + info.isToBeProcessed());
        LOG.info(msg("internal-id") + ": " + bitstream.getInternalId());
        LOG.info(msg("name") + ": " + bitstream.getName());
        LOG.info(msg("store-number") + ": " + bitstream.getStoreNumber());
        LOG.info(msg("size") + ": " + bitstream.getSizeBytes());
        LOG.info(msg("bitstream-format") + ": " + (bitstream.getFormat(context) != null ? bitstream.getFormat(context).getID() : "-1"));
        LOG.info(msg("user-format-description") + ": "
                + bitstream.getUserFormatDescription());
        LOG.info(msg("source") + ": " + bitstream.getSource());
        LOG
                .info(msg("checksum-algorithm") + ": "
                        + info.getChecksumAlgorithm());
        LOG.info(msg("previous-checksum") + ": " + info.getExpectedChecksum());
        LOG.info(msg("previous-checksum-date")
                + ": "
                + ((info.getProcessEndDate() != null) ? DATE_FORMAT.get().format(info
                        .getProcessEndDate()) : "unknown"));
        LOG.info(msg("new-checksum") + ": " + info.getCurrentChecksum());
        LOG.info(msg("checksum-comparison-result") + ": "
                + (info.getChecksumResult().getResultCode()));
        LOG.info("\n\n");
    }
}
