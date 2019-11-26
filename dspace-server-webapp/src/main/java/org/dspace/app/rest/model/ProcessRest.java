/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;
import org.dspace.content.ProcessStatus;
import org.dspace.scripts.Process;

/**
 * This class serves as a REST representation for the {@link Process} class
 */
public class ProcessRest extends BaseObjectRest<Integer> {
    public static final String NAME = "process";
    public static final String PLURAL_NAME = "processes";
    public static final String CATEGORY = RestAddressableModel.SYSTEM;


    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return RestResourceController.class;
    }

    public String getType() {
        return NAME;
    }

    private String scriptName;
    private UUID userId;
    private Integer processId;
    private Date startTime;
    private Date endTime;
    private ProcessStatus processStatus;
    @JsonProperty(value = "parameters")
    private List<ParameterValueRest> parameterRestList;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Integer getProcessId() {
        return processId;
    }

    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    public ProcessStatus getProcessStatus() {
        return processStatus;
    }

    public void setProcessStatus(ProcessStatus processStatus) {
        this.processStatus = processStatus;
    }

    public List<ParameterValueRest> getParameterRestList() {
        return parameterRestList;
    }

    public void setParameterRestList(List<ParameterValueRest> parameterRestList) {
        this.parameterRestList = parameterRestList;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @JsonIgnore
    @Override
    public Integer getId() {
        return id;
    }
}
