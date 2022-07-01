/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson challenge patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{"op":"add","path":"/password","value":"newpassword"},
 * { "op": "add", "path": /challenge", "value": "oldPassword"]'
 * </code>
 */
@Component
public class EPersonChallengeAddOperation<R> extends PatchOperation<R> {

    /**
     * Path in json body of patch that uses this operation
     */
    public static final String OPERATION_PASSWORD_CHANGE = "/challenge";

    @Override
    public R perform(Context context, R object, Operation operation) {
        return object;
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof EPerson && operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PASSWORD_CHANGE));
    }
}
