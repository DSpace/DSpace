/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation.ldn;

import static java.lang.String.format;

import java.math.BigDecimal;
import java.sql.SQLException;

import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for NotifyService Score Replace patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/ldn/ldnservices/<:id-notifyService> -H "
 * Content-Type: application/json" -d '
 * [{
 *  "op": "replace",
 *  "path": "/score",
 *  "value": "score value"
 *  }]'
 * </code>
 */
@Component
public class NotifyServiceScoreReplaceOperation extends PatchOperation<NotifyServiceEntity> {

    @Autowired
    private NotifyService notifyService;

    private static final String OPERATION_PATH = "/score";

    @Override
    public NotifyServiceEntity perform(Context context, NotifyServiceEntity notifyServiceEntity, Operation operation)
        throws SQLException {
        checkOperationValue(operation.getValue());

        Object score = operation.getValue();
        if (score == null) {
            throw new DSpaceBadRequestException("The /score value must be a decimal number");
        }
        BigDecimal scoreBigDecimal = null;
        try {
            scoreBigDecimal = new BigDecimal(score.toString());
        } catch (Exception e) {
            throw new DSpaceBadRequestException(format("Score out of range [0, 1] %s", score));
        }
        if (scoreBigDecimal.compareTo(java.math.BigDecimal.ZERO) == -1 ||
            scoreBigDecimal.compareTo(java.math.BigDecimal.ONE) == 1) {
            throw new UnprocessableEntityException(format("Score out of range [0, 1] %s", score));
        }

        checkModelForExistingValue(notifyServiceEntity);
        notifyServiceEntity.setScore(new BigDecimal((String)score));
        notifyService.update(context, notifyServiceEntity);
        return notifyServiceEntity;
    }

    /**
     * Checks whether the description of notifyServiceEntity has an existing value to replace
     * @param notifyServiceEntity Object on which patch is being done
     */
    private void checkModelForExistingValue(NotifyServiceEntity notifyServiceEntity) {
        if (notifyServiceEntity.getDescription() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value (description).");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof NotifyServiceEntity &&
            operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE) &&
            operation.getPath().trim().toLowerCase().equalsIgnoreCase(OPERATION_PATH));
    }
}
