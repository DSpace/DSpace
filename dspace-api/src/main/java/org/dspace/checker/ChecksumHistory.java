/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.Date;

/**
 * <p>
 * Represents a history record for the bitstream.
 * </p>
 *
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 */
public class ChecksumHistory {

    private static final Logger log = Logger.getLogger(ChecksumHistory.class);

    /**
     * Unique bitstream id.
     */
    private int bitstreamId;

    /**
     * Date the process started.
     */
    private Date processStartDate;

    /**
     * Date the process ended.
     */
    private Date processEndDate;

    /**
     * The expected checksum.
     */
    private String checksumExpected;

    /**
     * The checksum calculated.
     */
    private String checksumCalculated;

    /**
     * The string resultLong.
     */
    private String resultLong;

    /**
     * The string resultCode matching resultLong.
     */
    private String resultCode;

    /**
     * filled in on demand only
     */
    private Item item;

    /**
     * filled in on demand only
     */
    private Bitstream bitstream;

    /**
     * filled in on demand only
     */
    private Collection collection;

    /**
     * filled in on demand only
     */
    private Community community;

    /**
     * have filled data
     */
    Boolean filledIn = false;

    public ChecksumHistory() {
    }

    /**
     * Minimal Constructor.
     *
     * @param bitstreamId bitstream id in the database
     */
    public ChecksumHistory(int bitstreamId) {
        this.bitstreamId = bitstreamId;
    }

    /**
     * * Full history info Constructor.
     *
     * @param bitstrmId      bitstream Id.
     * @param startDate      process start date
     * @param endDate        process end date
     * @param checksumExpted expected checksum
     * @param checksumCalc   calculated checksum
     * @param resultLong     result information
     */
    public ChecksumHistory(int bitstrmId, Date startDate, Date endDate,
                           String checksumExpted, String checksumCalc, String resultLong, String resultCode) {
        this.bitstreamId = bitstrmId;
        this.processStartDate = (startDate == null ? null : new Date(startDate.getTime()));
        this.processEndDate = (endDate == null ? null : new Date(endDate.getTime()));
        this.checksumExpected = checksumExpted;
        this.checksumCalculated = checksumCalc;
        this.resultLong = resultLong;
        this.resultCode = resultCode;
    }

    /**
     * @return Returns the bitstreamId.
     */
    public int getBitstreamId() {
        return bitstreamId;
    }

    /**
     * @return Returns the checksumCalculated.
     */
    public String getChecksumCalculated() {
        return checksumCalculated == null ? "" : checksumCalculated;
    }

    /**
     * Get the extpected checksum.
     *
     * @return Returns the checksumExpected.
     */
    public String getChecksumExpected() {
        return checksumExpected == null ? "" : checksumExpected;
    }

    /**
     * Get the process end date. This is the date and time the processing ended.
     *
     * @return Returns the processEndDate.
     */
    public Date getProcessEndDate() {
        return processEndDate == null ? null : new Date(processEndDate.getTime());
    }

    /**
     * Get the process start date. This is the date and time the processing
     * started.
     *
     * @return Returns the processStartDate.
     */
    public Date getProcessStartDate() {
        return processStartDate == null ? null : new Date(processStartDate.getTime());
    }


    /**
     * @return result code
     */
    public String getResultCode() {
        return resultCode == null ? "" : resultCode;
    }

    /**
     * Return the processing result.
     *
     * @return the result (long format)
     */
    public String getResultLong() {
        return resultLong == null ? "" : resultLong;
    }

    private void filleme(Context ctxt) {
        if (!filledIn) {
            try {
                bitstream = Bitstream.find(ctxt, bitstreamId);

                DSpaceObject parent;
                if (bitstream != null) {
                    parent = bitstream.getParentObject();
                    while (parent != null) {
                        if (parent instanceof Item) {
                            item = (Item) parent;
                        } else if (parent instanceof Collection) {
                            collection = (Collection) parent;
                        } else if (parent instanceof Community) {
                            community = (Community) parent;
                        }
                        parent = parent.getParentObject();
                    }
                }
            } catch (SQLException e) {
                log.error(e.getMessage());
            } finally {
                filledIn = true;
            }
        }
    }

    public Item getItem(Context ctxt) {
        filleme(ctxt);
        return item;
    }

    public Collection getCollection(Context ctxt) {
        filleme(ctxt);
        return collection;
    }

    public Community getCommunity(Context ctxt) {
        filleme(ctxt);
        return community;
    }

}
