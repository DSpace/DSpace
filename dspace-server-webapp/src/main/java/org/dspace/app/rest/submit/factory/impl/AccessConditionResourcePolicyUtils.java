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
 * Utility class to reuse methods related to item resource-policies
 * These methods are applicable in the submission when adding new AccessCondition.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class AccessConditionResourcePolicyUtils {

    private AccessConditionResourcePolicyUtils() {}

    /**
     * Based on the given access conditions, find the resource policy to apply on the given DSpace object
     * This function applies the resource policies.
     * 
     * @param context                      DSpace context object
     * @param accessConditionOptions       The configured AccessCondition options
     * @param obj                          The applicable DSpace object whose policies should be determined
     * @param newAccessConditions          The access condition containing the details for the desired policies
     * @throws SQLException                If a database error occurs
     * @throws AuthorizeException          If the user is not authorized
     * @throws ParseException              if parser error
     */
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

    /**
     * Based on the given name, startDate and endDate
     * check if it match any access condition option,
     * then apply the the resource policy on the given DSpace object,
     * otherwise it throws UnprocessableEntity exception.
     * 
     * @param context                   DSpace context object
     * @param accessConditionOptions    The configured AccessCondition options
     * @param obj                       The applicable DSpace object whose policies should be determined
     * @param name                      The name of the access condition matching the desired policies
     * @param description               An optional description for the policies
     * @param startDate                 An optional start date for the policies
     * @param endDate                   An optional end date for the policies
     * @throws SQLException             If a database error occurs
     * @throws AuthorizeException       If the user is not authorized
     * @throws ParseException           if parser error
     */
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

    /**
     * Check if the given name, startDate and endDate match any access condition options configured,
     * otherwise it throws UnprocessableEntity exception.
     * 
     * @param context                  DSpace context object
     * @param accessConditionOptions   The configured AccessCondition options
     * @param name                     The name of the access condition matching the desired policies
     * @param startDate                An optional start date for the policies
     * @param endDate                  An optional end date for the policies
     * @throws SQLException            If a database error occurs
     * @throws AuthorizeException      If the user is not authorized
     * @throws ParseException          if parser error
     */
    public static void canApplyResourcePolicy(Context context, List<AccessConditionOption> accessConditionOptions,
            String name, Date startDate, Date endDate) throws SQLException, AuthorizeException, ParseException {
        boolean found = false;
        for (AccessConditionOption ac : accessConditionOptions) {
            if (ac.getName().equalsIgnoreCase(name)) {
                found = true;
            }
        }
        if (!found) {
            throw new UnprocessableEntityException("The provided policy: " + name + " is not supported!");
        }
    }

}