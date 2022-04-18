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
import java.util.Objects;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
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

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    DateMathParser dateMathParser = new DateMathParser();

    /** An unique name identifying the access contion option **/
    private String name;

    /**
     * the name of the group that will be bound to the resource policy created if
     * such option is used
     */
    private String groupName;

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

    /**
     * Explanation see: {@link #startDateLimit}
     * @return startDateLimit
     */
    public String getStartDateLimit() {
        return startDateLimit;
    }

    public void setStartDateLimit(String startDateLimit) {
        this.startDateLimit = startDateLimit;
    }

    /**
     * Explanation see: {@link #endDateLimit}
     * @return endDateLimit
     */
    public String getEndDateLimit() {
        return endDateLimit;
    }

    public void setEndDateLimit(String endDateLimit) {
        this.endDateLimit = endDateLimit;
    }

    /**
     * Create a new resource policy for a DSpaceObject
     * @param context DSpace context
     * @param obj DSpaceObject for which resource policy is created
     * @param name name of the resource policy
     * @param description description of the resource policy
     * @param startDate start date of the resource policy. If {@link #getHasStartDate()} returns false,
     *                  startDate should be null. Otherwise startDate may not be null.
     * @param endDate end date of the resource policy. If {@link #getHasEndDate()} returns false,
     *                endDate should be null. Otherwise endDate may not be null.
     */
    public void createResourcePolicy(Context context, DSpaceObject obj, String name, String description,
                                     Date startDate, Date endDate)
            throws SQLException, AuthorizeException, ParseException {
        validateResourcePolicy(context, name, startDate, endDate);
        Group group = groupService.findByName(context, getGroupName());
        authorizeService.createResourcePolicy(context, obj, group, null, Constants.READ,
                ResourcePolicy.TYPE_CUSTOM, name, description, startDate,
                endDate);
    }

    /**
     * Validate ResourcePolicy and after update it
     * 
     * @param context               DSpace context
     * @param resourcePolicy        ResourcePolicy to update
     * @throws SQLException         If database error
     * @throws AuthorizeException   If authorize error
     * @throws ParseException       If parser error
     */
    public void updateResourcePolicy(Context context, ResourcePolicy resourcePolicy)
           throws SQLException, AuthorizeException, ParseException {
        validateResourcePolicy(context, resourcePolicy.getRpName(),
                               resourcePolicy.getStartDate(), resourcePolicy.getEndDate());
        resourcePolicyService.update(context, resourcePolicy);
    }

    /**
     * Validate the policy properties, throws exceptions if any is not valid
     * 
     * @param context                DSpace context
     * @param name                   Name of the resource policy
     * @param startDate              Start date of the resource policy. If {@link #getHasStartDate()}
     *                                    returns false, startDate should be null. Otherwise startDate may not be null.
     * @param endDate                End date of the resource policy. If {@link #getHasEndDate()}
     *                                    returns false, endDate should be null. Otherwise endDate may not be null.
     */
    private void validateResourcePolicy(Context context, String name, Date startDate, Date endDate)
           throws SQLException, AuthorizeException, ParseException {
        if (getHasStartDate() && Objects.isNull(startDate)) {
            throw new IllegalStateException("The access condition " + getName() + " requires a start date.");
        }
        if (getHasEndDate() && Objects.isNull(endDate)) {
            throw new IllegalStateException("The access condition " + getName() + " requires an end date.");
        }
        if (!getHasStartDate() && Objects.nonNull(startDate)) {
            throw new IllegalStateException("The access condition " + getName() + " cannot contain a start date.");
        }
        if (!getHasEndDate() && Objects.nonNull(endDate)) {
            throw new IllegalStateException("The access condition " + getName() + " cannot contain an end date.");
        }

        Date latestStartDate = null;
        if (Objects.nonNull(getStartDateLimit())) {
            latestStartDate = dateMathParser.parseMath(getStartDateLimit());
        }

        Date latestEndDate = null;
        if (Objects.nonNull(getEndDateLimit())) {
            latestEndDate = dateMathParser.parseMath(getEndDateLimit());
        }

        // throw if startDate after latestStartDate
        if (Objects.nonNull(startDate) && Objects.nonNull(latestStartDate) && startDate.after(latestStartDate)) {
            throw new IllegalStateException(String.format(
                "The start date of access condition %s should be earlier than %s from now.",
                getName(), getStartDateLimit()
            ));
        }

        // throw if endDate after latestEndDate
        if (Objects.nonNull(endDate) && Objects.nonNull(latestEndDate)  && endDate.after(latestEndDate)) {
            throw new IllegalStateException(String.format(
                "The end date of access condition %s should be earlier than %s from now.",
                getName(), getEndDateLimit()
            ));
        }
    }

}