/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.model;

/**
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class AccessConditionOption {

    private String name;

    private String groupName;

    private String selectGroupName;

    private Boolean hasStartDate;

    private Boolean hasEndDate;

    private String startDateLimit;

    private String endDateLimit;

    public String getName() {
        return name;
    }

    public void setName(String type) {
        this.name = type;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Boolean getHasStartDate() {
        return hasStartDate;
    }

    public void setHasStartDate(Boolean hasStartDate) {
        this.hasStartDate = hasStartDate;
    }

    public Boolean getHasEndDate() {
        return hasEndDate;
    }

    public void setHasEndDate(Boolean hasEndDate) {
        this.hasEndDate = hasEndDate;
    }

    public String getStartDateLimit() {
        return startDateLimit;
    }

    public void setStartDateLimit(String startDateLimit) {
        this.startDateLimit = startDateLimit;
    }

    public String getEndDateLimit() {
        return endDateLimit;
    }

    public void setEndDateLimit(String endDateLimit) {
        this.endDateLimit = endDateLimit;
    }

    public String getSelectGroupName() {
        return selectGroupName;
    }

    public void setSelectGroupName(String selectGroupName) {
        this.selectGroupName = selectGroupName;
    }


}
