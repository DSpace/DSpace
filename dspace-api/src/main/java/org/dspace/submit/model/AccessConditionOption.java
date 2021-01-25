/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.model;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.util.DateMathParser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class represents an option available in the submission upload section to
 * set permission on a file. An option is defined by a name such as "open
 * access", "embargo", "restricted access", etc. and some optional attributes to
 * better clarify the constraints and input available to the user. For instance
 * an embargo option could allow to set a start date not longer than 3 years,
 * etc
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class AccessConditionOption {

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    GroupService groupService;

    DateMathParser dateMathParser = new DateMathParser();

    /** An unique name identifying the access contion option **/
    private String name;

    /**
     * the name of the group that will be bound to the resource policy created if
     * such option is used
     */
    private String groupName;

    /**
     * this is in alternative to the {@link #groupName}. The sub-groups listed in
     * the DSpace group identified by the name here specified will be available to
     * the user to personalize the access condition. They can be for instance
     * University Staff, University Students, etc. so that a "restricted access"
     * option can be further specified without the need to create separate access
     * condition options for each group
     */
    private String selectGroupName;

    /**
     * set to <code>true</code> if this option requires a start date to be indicated
     * for the underlying resource policy to create
     */
    private Boolean hasStartDate;

    /**
     * set to <code>true</code> if this option requires an end date to be indicated
     * for the underlying resource policy to create
     */
    private Boolean hasEndDate;

    /**
     * It contains, if applicable, the maximum start date (i.e. when the "embargo
     * expires") that can be selected. It accepts date math via joda library (such as
     * +3years)
     */
    private String startDateLimit;

    /**
     * It contains, if applicable, the maximum end date (i.e. when the "lease
     * expires") that can be selected. It accepts date math via joda library (such as
     * +3years)
     */
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

    public void createResourcePolicy(Context context, Bitstream b, String name, String description,
                                     Date startDate, Date endDate)
            throws SQLException, AuthorizeException, ParseException {
        if (getHasStartDate() && startDate == null) {
            throw new IllegalStateException("The access condition " + getName() + " requires a start date.");
        }
        if (getHasEndDate() && endDate == null) {
            throw new IllegalStateException("The access condition " + getName() + " requires an end date.");
        }
        if (!getHasStartDate() && startDate != null) {
            throw new IllegalStateException("The access condition " + getName() + " cannot contain a start date.");
        }
        if (!getHasEndDate() && endDate != null) {
            throw new IllegalStateException("The access condition " + getName() + " cannot contain an end date.");
        }

        Date earliestStartDate = null;
        if (getStartDateLimit() != null) {
            earliestStartDate = dateMathParser.parseMath(getStartDateLimit());
        }

        Date latestEndDate = null;
        if (getEndDateLimit() != null) {
            latestEndDate = dateMathParser.parseMath(getEndDateLimit());
        }

        // throw if latestEndDate before earliestStartDate
        if (earliestStartDate != null && latestEndDate != null && earliestStartDate.compareTo(latestEndDate) > 0) {
            throw new IllegalStateException(String.format(
                "The boundaries of %s overlap: [%s, %s]", getName(), getStartDateLimit(), getEndDateLimit()
            ));
        }

        // throw if startDate before earliestStartDate
        if (earliestStartDate != null && earliestStartDate.compareTo(startDate) > 0) {
            throw new IllegalStateException(String.format(
                "The start date of access condition %s should be later than %s from now.",
                getName(), getStartDateLimit()
            ));
        }

        // throw if endDate after latestEndDate
        if (latestEndDate != null && latestEndDate.compareTo(endDate) < 0) {
            throw new IllegalStateException(String.format(
                "The end date of access condition %s should be earlier than %s from now.", getName(), getEndDateLimit()
            ));
        }

        Group group = groupService.findByName(context, getGroupName());
        authorizeService.createResourcePolicy(context, b, group, null, Constants.READ,
                ResourcePolicy.TYPE_CUSTOM, name, description, startDate,
                endDate);
    }
}
