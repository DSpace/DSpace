/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.dspace.core.Context;
import org.dspace.content.Bitstream;
import org.dspace.core.ReloadableEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * <p>
 * Represents a history record for the bitstream.
 * </p>
 *
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 *
 */
@Entity
@Table(name="checksum_history")
public class ChecksumHistory implements ReloadableEntity<Long>
{


    @Id
    @Column(name="check_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="checksum_history_check_id_seq")
    @SequenceGenerator(name="checksum_history_check_id_seq", sequenceName="checksum_history_check_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bitstream_id")
    private Bitstream bitstream;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "process_start_date", nullable = false)
    private Date processStartDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "process_end_date", nullable = false)
    private Date processEndDate;

    @Column(name= "checksum_expected", nullable = false)
    private String checksumExpected;

    @Column(name= "checksum_calculated", nullable = false)
    private String checksumCalculated;

    @ManyToOne
    @JoinColumn(name = "result")
    private ChecksumResult checksumResult;


    /**
     * Protected constructor, create object using:
     * {@link org.dspace.checker.service.ChecksumHistoryService#addHistory(Context, MostRecentChecksum)}
     */
    protected ChecksumHistory()
    {
    }

    public Long getID() {
        return id;
    }

    /**
     * @return Returns the bitstreamId.
     */
    public Bitstream getBitstream()
    {
        return bitstream;
    }

    public void setBitstream(Bitstream bitstream) {
        this.bitstream = bitstream;
    }

    /**
     * @return Returns the checksumCalculated.
     */
    public String getChecksumCalculated()
    {
        return checksumCalculated;
    }

    /**
     * Set the checksum calculated.
     *
     * @param checksumCalculated
     *            The checksumCalculated to set.
     */
    public void setChecksumCalculated(String checksumCalculated)
    {
        this.checksumCalculated = checksumCalculated;
    }

    /**
     * Get the extpected checksum.
     *
     * @return Returns the checksumExpected.
     */
    public String getChecksumExpected()
    {
        return checksumExpected;
    }

    /**
     * Set the expected checksum.
     *
     * @param checksumExpected
     *            The checksumExpected to set.
     */
    public void setChecksumExpected(String checksumExpected)
    {
        this.checksumExpected = checksumExpected;
    }

    /**
     * Get the process end date. This is the date and time the processing ended.
     *
     * @return Returns the processEndDate.
     */
    public Date getProcessEndDate()
    {
        return processEndDate == null ? null : new Date(processEndDate.getTime());
    }

    /**
     * Set the process end date. This is the date and time the processing ended.
     *
     * @param processEndDate
     *            The processEndDate to set.
     */
    public void setProcessEndDate(Date processEndDate)
    {
        this.processEndDate = (processEndDate == null ? null : new Date(processEndDate.getTime()));
    }

    /**
     * Get the process start date. This is the date and time the processing
     * started.
     *
     * @return Returns the processStartDate.
     */
    public Date getProcessStartDate()
    {
        return processStartDate == null ? null : new Date(processStartDate.getTime());
    }

    /**
     * Set the process start date. This is the date and time the processing
     * started.
     *
     * @param processStartDate
     *            The processStartDate to set.
     */
    public void setProcessStartDate(Date processStartDate)
    {
        this.processStartDate = (processStartDate == null ? null : new Date(processStartDate.getTime()));
    }

    /**
     * Return the processing result.
     * @return result
     */
    public ChecksumResult getResult()
    {
        return checksumResult;
    }

    /**
     * Set the checksum processing result.
     *
     * @param result
     *            The result to set.
     */
    public void setResult(ChecksumResult result)
    {
        this.checksumResult = result;
    }
}
