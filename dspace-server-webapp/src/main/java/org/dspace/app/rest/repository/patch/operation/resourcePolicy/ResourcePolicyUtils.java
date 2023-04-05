/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation.resourcePolicy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.ResourcePolicy;
import org.springframework.stereotype.Component;

/**
 * Util class for shared methods between the ResourcePolicy patches.
 *
 * @author Maria Verdonck (Atmire) on 14/02/2020
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ResourcePolicyUtils {

    public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Paths in json body of patched that use these resourcePolicy operations
     */
    public static final String OPERATION_PATH_STARTDATE = "/startDate";
    public static final String OPERATION_PATH_ENDDATE = "/endDate";
    public static final String OPERATION_PATH_DESCRIPTION = "/description";
    public static final String OPERATION_PATH_NAME = "/name";
    public static final String OPERATION_PATH_POLICY_TYPE = "/policyType";
    public static final String OPERATION_PATH_ACTION = "/action";

    /**
     * Throws PatchBadRequestException for missing value in the /startDate path.
     *
     * @param resource
     *            the resource to update
     * @param operation
     *            the operation to apply
     *
     */
    public void checkResourcePolicyForExistingStartDateValue(ResourcePolicy resource, Operation operation) {
        if (resource.getStartDate() == null) {
            throw new DSpaceBadRequestException("Attempting to " + operation.getOp()
                + " a non-existent start date value.");
        }
    }

    /**
     * Throws PatchBadRequestException for missing value in the /endDate path.
     *
     * @param resource
     *            the resource to update
     * @param operation
     *            the operation to apply
     *
     */
    public void checkResourcePolicyForExistingEndDateValue(ResourcePolicy resource, Operation operation) {
        if (resource.getEndDate() == null) {
            throw new DSpaceBadRequestException("Attempting to " + operation.getOp()
                + " a non-existent end date value.");
        }
    }

    /**
     * Throws PatchBadRequestException for missing value in the /startDate path.
     *
     * @param resource
     *            the resource to update
     * @param operation
     *            the operation to apply
     *
     */
    public void checkResourcePolicyForExistingNameValue(ResourcePolicy resource, Operation operation) {
        if (resource.getRpName() == null) {
            throw new DSpaceBadRequestException("Attempting to " + operation.getOp() + " a non-existent name value.");
        }
    }

    /**
     * Throws PatchBadRequestException for missing value in the /description path.
     *
     * @param resource
     *            the resource to update
     * @param operation
     *            the operation to apply
     *
     */
    public void checkResourcePolicyForExistingDescriptionValue(ResourcePolicy resource, Operation operation) {
        if (resource.getRpDescription() == null) {
            throw new DSpaceBadRequestException("Attempting to " + operation.getOp()
                + " a non-existent description value.");
        }
    }

    /**
     * Throws PatchBadRequestException if the value for startDate is not consistent with the endDate value, if present
     * (greater than).
     *
     * @param resource
     *            the resource to update
     * @param operation
     *            the operation to apply
     *
     */
    public void checkResourcePolicyForConsistentStartDateValue(ResourcePolicy resource, Operation operation) {
        String dateS = (String) operation.getValue();
        try {
            Date date = simpleDateFormat.parse(dateS);
            if (resource.getEndDate() != null && resource.getEndDate().before(date)) {
                throw new DSpaceBadRequestException("Attempting to set an invalid startDate greater than the endDate.");
            }
        } catch (ParseException e) {
            throw new DSpaceBadRequestException("Invalid startDate value", e);
        }
    }

    /**
     * Throws PatchBadRequestException if the value for endDate is not consistent with the startDate value, if present
     * (smaller than).
     *
     * @param resource
     *            the resource to update
     * @param operation
     *            the operation to apply
     *
     */
    public void checkResourcePolicyForConsistentEndDateValue(ResourcePolicy resource, Operation operation) {
        String dateS = (String) operation.getValue();
        try {
            Date date = simpleDateFormat.parse(dateS);
            if (resource.getStartDate() != null && resource.getStartDate().after(date)) {
                throw new DSpaceBadRequestException("Attempting to set an invalid endDate smaller than the startDate.");
            }
        } catch (ParseException e) {
            throw new DSpaceBadRequestException("Invalid endDate value", e);
        }
    }
}
