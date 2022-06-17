/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.ProcessStatus;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * This class is the DB Entity representation of the Process object to be stored in the Database
 */
@Entity
@Table(name = "process")
public class Process implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "process_id_seq")
    @SequenceGenerator(name = "process_id_seq", sequenceName = "process_id_seq", allocationSize = 1)
    @Column(name = "process_id", unique = true, nullable = false)
    private Integer processId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private EPerson ePerson;

    @Column(name = "start_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(name = "finished_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date finishedTime;

    @Column(name = "script", nullable = false)
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ProcessStatus processStatus;

    @Column(name = "parameters")
    private String parameters;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "process2bitstream",
        joinColumns = {@JoinColumn(name = "process_id")},
        inverseJoinColumns = {@JoinColumn(name = "bitstream_id")}
    )
    private List<Bitstream> bitstreams;

    /*
     * Special Groups associated with this Process
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinTable(
        name = "process2group",
        joinColumns = {@JoinColumn(name = "process_id")},
        inverseJoinColumns = {@JoinColumn(name = "group_id")}
    )
    private List<Group> groups;

    @Column(name = "creation_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationTime;

    public static final String BITSTREAM_TYPE_METADATAFIELD = "dspace.process.filetype";
    public static final String OUTPUT_TYPE = "script_output";

    protected Process() {
    }

    /**
     * This method returns the ID that the Process holds within the Database
     * @return  The ID that the process holds within the database
     */
    @Override
    public Integer getID() {
        return processId;
    }

    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    /**
     * This method returns an EPerson object. This EPerson object is the EPerson that initially created the process
     * @return  The EPerson that created the process
     */
    public EPerson getEPerson() {
        return ePerson;
    }

    public void setEPerson(EPerson ePerson) {
        this.ePerson = ePerson;
    }

    /**
     * This method returns the Start time for the Process. This reflects the time when the Process was actually started
     * @return  The start time for the Process
     */
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * This method returns the time that Process was finished
     * @return  The finished time for the Process
     */
    public Date getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(Date finishedTime) {
        this.finishedTime = finishedTime;
    }

    /**
     * This method returns the name of the Process. For example filter-media
     * @return  The name of the Process
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * This method returns a ProcessStatus value that represents the current state of the Process. These values
     * can be found within the {@link ProcessStatus} enum
     * @return  The status of the Process
     */
    public ProcessStatus getProcessStatus() {
        return processStatus;
    }

    public void setProcessStatus(ProcessStatus processStatus) {
        this.processStatus = processStatus;
    }

    /**
     * To get the parameters, use ProcessService.getParameters() to get a parsed list of DSpaceCommandLineParameters
     * This String representation is the parameter in an unparsed fashion.For example "-c test"
     * @return the raw parameter string.
     */
    protected String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    /**
     * This method returns a list of Bitstreams that will be used or created by the Process. This list contains both
     * input and output bitstreams.
     * @return  The Bitstreams that are used or created by the process
     */
    public List<Bitstream> getBitstreams() {
        if (bitstreams == null) {
            bitstreams = new ArrayList<>();
        }
        return bitstreams;
    }

    public void setBitstreams(List<Bitstream> bitstreams) {
        this.bitstreams = bitstreams;
    }

    public void removeBitstream(Bitstream bitstream) {
        getBitstreams().remove(bitstream);
    }

    public void addBitstream(Bitstream bitstream) {
        getBitstreams().add(bitstream);
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * This method will return the time when the Process was created. Note that this is potentially different from
     * the StartTime (for example if the Process was queued)
     * @return  The creation time of the Process
     */
    public Date getCreationTime() {
        return creationTime;
    }

    /**
     * This method sets the special groups associated with the Process.
     */
    public List<Group> getGroups() {
        return groups;
    }

    /**
     * This method will return special groups associated with the Process.
     * @return The special groups of this process.
     */
    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Process
     * as this object, <code>false</code> otherwise
     *
     * @param other object to compare to
     * @return <code>true</code> if object passed in represents the same
     * collection as this object
     */
    @Override
    public boolean equals(Object other) {
        return (other instanceof Process &&
            new EqualsBuilder().append(this.getID(), ((Process) other).getID())
                               .append(this.getName(), ((Process) other).getName())
                               .append(this.getBitstreams(), ((Process) other).getBitstreams())
                               .append(this.getProcessStatus(), ((Process) other).getProcessStatus())
                               .append(this.getFinishedTime(), ((Process) other).getFinishedTime())
                               .append(this.getStartTime(), ((Process) other).getStartTime())
                               .append(this.getParameters(), ((Process) other).getParameters())
                               .append(this.getCreationTime(), ((Process) other).getCreationTime())
                               .append(this.getEPerson(), ((Process) other).getEPerson())
                               .isEquals());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.getID())
            .append(this.getName())
            .append(this.getBitstreams())
            .append(this.getProcessStatus())
            .append(this.getFinishedTime())
            .append(this.getStartTime())
            .append(this.getParameters())
            .append(this.getCreationTime())
            .append(this.getEPerson())
            .toHashCode();
    }
}
