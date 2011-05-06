/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
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
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
            "MM/dd/yyyy hh:mm:ss");

    /**
     * Date the current checking run started.
     */
    Date startDate = null;

    /**
     * ChecksumResultDAO dependency variable.
     */
    private ChecksumResultDAO resultDAO;

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
        this.resultDAO = new ChecksumResultDAO();

        LOG.info(msg("run-start-time") + ": " + DATE_FORMAT.format(startDt));
    }

    /**
     * Get the i18N string.
     * 
     * @param key
     *            to get the message.
     * @return the message found.
     */
    private String msg(String key)
    {
        return I18nUtil.getMessage("org.dspace.checker.ResultsLogger." + key);
    }

    /**
     * Collect a result for logging.
     * 
     * @param info
     *            the BitstreamInfo representing the result.
     * @see org.dspace.checker.ChecksumResultsCollector#collect(org.dspace.checker.BitstreamInfo)
     */
    public void collect(BitstreamInfo info)
    {
        LOG.info("******************************************************");
        LOG.info(msg("bitstream-id") + ": " + info.getBitstreamId());
        LOG.info(msg("bitstream-info-found") + ": " + info.getInfoFound());
        LOG.info(msg("bitstream-marked-deleted") + ": " + info.getDeleted());
        LOG.info(msg("bitstream-found") + ": " + info.getBitstreamFound());
        LOG.info(msg("to-be-processed") + ": " + info.getToBeProcessed());
        LOG.info(msg("internal-id") + ": " + info.getInternalId());
        LOG.info(msg("name") + ": " + info.getName());
        LOG.info(msg("store-number") + ": " + info.getStoreNumber());
        LOG.info(msg("size") + ": " + info.getSize());
        LOG.info(msg("bitstream-format") + ": " + info.getBitstreamFormatId());
        LOG.info(msg("user-format-description") + ": "
                + info.getUserFormatDescription());
        LOG.info(msg("source") + ": " + info.getSource());
        LOG
                .info(msg("checksum-algorithm") + ": "
                        + info.getChecksumAlgorithm());
        LOG.info(msg("previous-checksum") + ": " + info.getStoredChecksum());
        LOG.info(msg("previous-checksum-date")
                + ": "
                + ((info.getProcessEndDate() != null) ? DATE_FORMAT.format(info
                        .getProcessEndDate()) : "unknown"));
        LOG.info(msg("new-checksum") + ": " + info.getCalculatedChecksum());
        LOG.info(msg("checksum-comparison-result") + ": "
                + resultDAO.getChecksumCheckStr(info.getChecksumCheckResult()));
        LOG.info("\n\n");
    }
}
