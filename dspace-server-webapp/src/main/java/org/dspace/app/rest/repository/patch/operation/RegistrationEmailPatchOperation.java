/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.RegistrationTypeEnum;
import org.dspace.eperson.dto.RegistrationDataChanges;
import org.dspace.eperson.dto.RegistrationDataPatch;
import org.dspace.eperson.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for RegistrationData email patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/eperson/registration/<:registration-id>?token=<:token> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "/email", "value": "new@email"]'
 * </code>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@Component
public class RegistrationEmailPatchOperation<R extends RegistrationData> extends PatchOperation<R> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_EMAIL = "/email";

    @Autowired
    private AccountService accountService;

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());

        RegistrationDataPatch registrationDataPatch;
        try {
            String email = getTextValue(operation);
            registrationDataPatch =
                new RegistrationDataPatch(
                    object,
                    new RegistrationDataChanges(
                        email,
                        registrationTypeFor(context, object, email)
                    )
                );
        } catch (IllegalArgumentException e) {
            throw new UnprocessableEntityException(
                "Cannot perform the patch operation",
                e
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (!supports(object, operation)) {
            throw new UnprocessableEntityException(
                MessageFormat.format(
                    "RegistrationEmailReplaceOperation does not support {0} operation",
                    operation.getOp()
                )
            );
        }

        if (!isOperationAllowed(operation, object)) {
            throw new UnprocessableEntityException(
                MessageFormat.format(
                    "Attempting to perform {0} operation over {1} value (e-mail).",
                    operation.getOp(),
                    object.getEmail() == null ? "null" : "not null"
                )
            );
        }


        try {
            return (R) accountService.renewRegistrationForEmail(context, registrationDataPatch);
        } catch (AuthorizeException e) {
            throw new DSpaceBadRequestException(
                MessageFormat.format(
                    "Cannot perform {0} operation over {1} value (e-mail).",
                    operation.getOp(),
                    object.getEmail() == null ? "null" : "not null"
                ),
                e
            );
        }
    }

    private static String getTextValue(Operation operation) {
        Object value = operation.getValue();

        if (value instanceof String) {
            return ((String) value);
        }

        if (value instanceof JsonValueEvaluator) {
            return Optional.of((JsonValueEvaluator) value)
                           .map(JsonValueEvaluator::getValueNode)
                           .filter(nodes -> !nodes.isEmpty())
                           .map(nodes -> nodes.get(0))
                           .map(JsonNode::asText)
                           .orElseThrow(() -> new DSpaceBadRequestException("No value provided for operation"));
        }
        throw new DSpaceBadRequestException("Invalid patch value for operation!");
    }

    private RegistrationTypeEnum registrationTypeFor(
        Context context, R object, String email
    )
        throws SQLException {
        if (accountService.existsAccountWithEmail(context, email)) {
            return RegistrationTypeEnum.VALIDATION_ORCID;
        }
        return object.getRegistrationType();
    }


    /**
     * Checks whether the email of RegistrationData has an existing value to replace or adds a new value.
     *
     * @param operation        operation to check
     * @param registrationData Object on which patch is being done
     */
    private boolean isOperationAllowed(Operation operation, RegistrationData registrationData) {
        return isReplaceOperationAllowed(operation, registrationData) ||
            isAddOperationAllowed(operation, registrationData);
    }

    private boolean isAddOperationAllowed(Operation operation, RegistrationData registrationData) {
        return operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD) && registrationData.getEmail() == null;
    }

    private static boolean isReplaceOperationAllowed(Operation operation, RegistrationData registrationData) {
        return operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE) && registrationData.getEmail() != null;
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof RegistrationData &&
            (
                operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE) ||
                    operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD)
            ) &&
            operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_EMAIL));
    }
}

