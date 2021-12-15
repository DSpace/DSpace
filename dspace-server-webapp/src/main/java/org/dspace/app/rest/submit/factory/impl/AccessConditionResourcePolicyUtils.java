/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.AccessConditionDTO;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.submit.model.AccessConditionOption;

/**
 * Utility class to reuse methods related to resource-policies
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class AccessConditionResourcePolicyUtils {

    private AccessConditionResourcePolicyUtils() {}

    public static void findApplyResourcePolicy(Context context, List<AccessConditionOption> accessConditionOptions,
            DSpaceObject obj, List<AccessConditionDTO> newAccessConditions)
            throws SQLException, AuthorizeException, ParseException {
        for (AccessConditionDTO newAccessCondition : newAccessConditions) {
            String name = newAccessCondition.getName();
            String description = newAccessCondition.getDescription();

            Date startDate = newAccessCondition.getStartDate();
            Date endDate = newAccessCondition.getEndDate();

            findApplyResourcePolicy(context, accessConditionOptions, obj, name, description, startDate, endDate);
        }
    }

    public static void findApplyResourcePolicy(Context context,
            List<AccessConditionOption> accessConditionOptions, DSpaceObject obj, String name,
            String description, Date startDate, Date endDate) throws SQLException, AuthorizeException, ParseException {
        boolean found = false;
        for (AccessConditionOption accessConditionOption : accessConditionOptions) {
            if (!found && accessConditionOption.getName().equalsIgnoreCase(name)) {
                accessConditionOption.createResourcePolicy(context, obj, name, description, startDate, endDate);
                found = true;
            }
        }
        if (!found) {
            throw new UnprocessableEntityException("The provided policy: " + name + " is not supported!");
        }
    }

    public static void canApplyResourcePolicy(Context context, List<AccessConditionOption> accessConditionOptions,
            String name, Date startDate, Date endDate) throws SQLException, AuthorizeException, ParseException {
        accessConditionOptions.stream()
            .filter(ac -> ac.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new UnprocessableEntityException("The provided policy: " + name + " is not supported!"));
    }

}