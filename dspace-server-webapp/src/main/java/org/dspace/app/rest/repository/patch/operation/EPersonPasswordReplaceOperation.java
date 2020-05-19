/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson password patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /password", "value": "newpassword"]'
 * </code>
 */
@Component
public class EPersonPasswordReplaceOperation<R> extends PatchOperation<R> {

    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(EPersonPasswordReplaceOperation.class);

    /**
     * Path in json body of patch that uses this operation
     */
    public static final String OPERATION_PASSWORD_CHANGE = "/password";
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    @Autowired
    private RequestService requestService;

    @Autowired
    private AccountService accountService;

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(object, operation)) {
            EPerson eperson = (EPerson) object;
            String token = requestService.getCurrentRequest().getHttpServletRequest().getParameter("token");
            checkModelForExistingValue(eperson);
            if (StringUtils.isNotBlank(token)) {
                patchWithToken(context,eperson, token, operation);
            }
            if (!AuthorizeUtil.authorizeUpdatePassword(context, eperson.getEmail())) {
                throw new DSpaceBadRequestException("Password cannot be updated for the given EPerson with email: " +
                                                        eperson.getEmail());
            }
            ePersonService.setPassword(eperson, (String) operation.getValue());
            return object;
        } else {
            throw new DSpaceBadRequestException("EPersonPasswordReplaceOperation does not support this operation");
        }
    }

    private void patchWithToken(Context context, EPerson eperson, String token, Operation operation) {
        try {
            EPerson ePersonFromToken = accountService.getEPerson(context, token);
            if (ePersonFromToken == null) {
                throw new AccessDeniedException("The token in the parameter: " + token + " couldn't" +
                                                    " be associated with an EPerson");
            }
            if (!ePersonFromToken.getID().equals(eperson.getID())) {
                throw new AccessDeniedException("The token in the parameter belongs to a different EPerson" +
                                                    " than the uri indicates");
            }
            accountService.deleteToken(context, token);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Checks whether the ePerson has a password via the ePersonService to checking if it has a non null password hash
     *      throws a DSpaceBadRequestException if not pw hash was present
     * @param ePerson   Object on which patch is being performed
     */
    private void checkModelForExistingValue(EPerson ePerson) {
        if (ePersonService.getPasswordHash(ePerson) == null
                || ePersonService.getPasswordHash(ePerson).getHash() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value (netID).");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof EPerson && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PASSWORD_CHANGE));
    }
}
