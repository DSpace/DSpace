/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class is the representation of the ProcessLog entity stored in the process_log table in the DB
 */
@Entity
@Table(name = "process_log")
public class ProcessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "process_log_id_seq")
    @SequenceGenerator(name = "process_log_id_seq", sequenceName = "process_log_id_seq", allocationSize = 1)
    @Column(name = "process_log_id", unique = true, nullable = false)
    private Integer processLogId;

    @ManyToOne
    @JoinColumn(name = "process_id", nullable = false)
    private Process process;

    @Column(name = "output", nullable = false)
    private String output;

    @Column(name = "time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date time;

    @Column(name = "log_level", nullable = false)
    private ProcessLogLevel processLogLevel;

    protected ProcessLog() {
    }

    public Integer getProcessLogId() {
        return processLogId;
    }

    public void setProcessLogId(Integer processLogId) {
        this.processLogId = processLogId;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public ProcessLogLevel getProcessLogLevel() {
        return processLogLevel;
    }

    public void setProcessLogLevel(ProcessLogLevel processLogLevel) {
        this.processLogLevel = processLogLevel;
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Bitstream
     * as this object, <code>false</code> otherwise
     *
     * @param other object to compare to
     * @return <code>true</code> if object passed in represents the same
     * collection as this object
     */
    @Override
    public boolean equals(Object other) {
        return (other instanceof ProcessLog &&
            new EqualsBuilder().append(this.getProcessLogId(), ((ProcessLog) other).getProcessLogId())
                               .append(this.getProcess(), ((ProcessLog) other).getProcess())
                               .append(this.getOutput(), ((ProcessLog) other).getOutput())
                               .append(this.getTime(), ((ProcessLog) other).getTime())
                               .append(this.getProcessLogLevel(), ((ProcessLog) other).getProcessLogLevel())
                               .isEquals());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.getProcessLogId())
            .append(this.getProcess())
            .append(this.getOutput())
            .append(this.getTime())
            .append(this.getProcessLogLevel())
            .toHashCode();
    }
}
