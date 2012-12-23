/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.dspace.core.Constants;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */


@Entity
@Table(name = "checksum_history")
public class CheckSumHistory{
    private long checkId;
    private Bitstream bitstream;
    private Date processStartDate;
    private Date processendDate;
    private String checksumExpected;
    private String checksumCalculated;
    private String result;
    
    @Id
    @Column(name = "check_id")
    @GeneratedValue
    public long getCheckId() {
        return checkId;
    }
    
    public long setCheckId(long checkId) {
        return this.checkId = checkId;
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bitstream_id", nullable = true)
	public Bitstream getBitstream() {
		return bitstream;
	}

	public void setBitstream(Bitstream bitstream) {
		this.bitstream = bitstream;
	}

    @Column(name = "process_start_date", nullable = true)
	public Date getProcessStartDate() {
		return processStartDate;
	}

	public void setProcessStartDate(Date processStartDate) {
		this.processStartDate = processStartDate;
	}

    @Column(name = "process_end_date", nullable = true)
	public Date getProcessendDate() {
		return processendDate;
	}

	public void setProcessendDate(Date processendDate) {
		this.processendDate = processendDate;
	}

    @Column(name = "checksum_expected", nullable = true)
	public String getChecksumExpected() {
		return checksumExpected;
	}

	public void setChecksumExpected(String checksumExpected) {
		this.checksumExpected = checksumExpected;
	}

    @Column(name = "checksum_calculated", nullable = true)
	public String getChecksumCalculated() {
		return checksumCalculated;
	}

	public void setChecksumCalculated(String checksumCalculated) {
		this.checksumCalculated = checksumCalculated;
	}

    @Column(name = "result", nullable = true)
	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

   
}
