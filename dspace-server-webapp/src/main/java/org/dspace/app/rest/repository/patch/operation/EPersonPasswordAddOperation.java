/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.PasswordNotValidException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.exception.WrongCurrentPasswordException;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.ValidatePasswordService;
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
 * Implementation for EPerson password patches. This Add Operation will add a
 * new password the an eperson if it had no password before, or will replace the
 * existing password with the new value.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "add", "path": "
 * /password", "value": {"new_password": "newpassword", "current_password": "currentpassword"}]'
 * </code>
 */
@Component
public class EPersonPasswordAddOperation<R> extends PatchOperation<R> {

    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(EPersonPasswordAddOperation.class);

    /**
     * Path in json body of patch that uses this operation
     */
    public static final String OPERATION_PASSWORD_CHANGE = "/password";
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    @Autowired
    private RequestService requestService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ValidatePasswordService validatePasswordService;

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public R perform(Context context, R object, Operation operation) {

        if (!supports(object, operation)) {
            throw new DSpaceBadRequestException(this.getClass().getName() + " does not support this operation");
        }

        PasswordVO passwordVO = parseOperationValue(operation);

        String newPassword = passwordVO.getNewPassword()
            .orElseThrow(() -> new DSpaceBadRequestException("No password provided"));

        EPerson eperson = (EPerson) object;
        if (!AuthorizeUtil.authorizeUpdatePassword(context, eperson.getEmail())) {
            throw new DSpaceBadRequestException("Password cannot be updated for the given EPerson with email: " +
                                                    eperson.getEmail());
        }

        if (!validatePasswordService.isPasswordValid(newPassword)) {
            throw new PasswordNotValidException();
        }

        String token = requestService.getCurrentRequest().getHttpServletRequest().getParameter("token");
        if (StringUtils.isNotBlank(token)) {
            verifyAndDeleteToken(context, eperson, token, operation);
        } else if (eperson.hasPasswordSet()) {
            verifyCurrentPassword(context, eperson, passwordVO);
        }

        ePersonService.setPassword(eperson, newPassword);

        return object;
    }

    private PasswordVO parseOperationValue(Operation operation) {

        if (operation.getValue() == null) {
            throw new UnprocessableEntityException("No value provided for operation " + operation.getPath());
        }

        try {
            return (PasswordVO) ((JsonValueEvaluator) operation.getValue()).evaluate(PasswordVO.class);
        } catch (Exception ex) {
            throw new UnprocessableEntityException("Invalid value provided for operation " + operation.getPath(), ex);
        }

    }

    private void verifyAndDeleteToken(Context context, EPerson eperson, String token, Operation operation) {
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
            context.setCurrentUser(ePersonFromToken);
            accountService.deleteToken(context, token);
        } catch (SQLException | AuthorizeException e) {
            log.error("Failed to verify or delete the token for an EPerson patch", e);
        }
    }

    private void verifyCurrentPassword(Context context, EPerson eperson, PasswordVO passwordVO) {

        String currentPassword = passwordVO.getCurrentPassword()
            .orElseThrow(() -> new WrongCurrentPasswordException("No current password provided"));

        boolean canChangePassword = authenticationService.canChangePassword(context, eperson, currentPassword);
        if (!canChangePassword) {
            throw new WrongCurrentPasswordException("The provided password is wrong");
        }

    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof EPerson && operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PASSWORD_CHANGE));
    }

    /**
     * Value object that stores the new password to set and the current password to
     * verify. This object models the value of the operation.
     *
     * @author Luca Giamminonni (luca.giamminonni at 4science.it)
     *
     */
    public static class PasswordVO {

        @JsonProperty("new_password")
        private String newPassword;

        @JsonProperty("current_password")
        private String currentPassword;

        public Optional<String> getNewPassword() {
            return Optional.ofNullable(newPassword);
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public Optional<String> getCurrentPassword() {
            return Optional.ofNullable(currentPassword);
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

    }
}
