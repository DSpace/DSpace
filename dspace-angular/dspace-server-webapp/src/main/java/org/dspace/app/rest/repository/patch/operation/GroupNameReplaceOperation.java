/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for Group name replacement patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/epersons/groups/<:id-group> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /name", "value": "new name"]'
 * </code>
 */
@Component
public class GroupNameReplaceOperation<R> extends PatchOperation<R> {

    @Autowired
    GroupService groupService;

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_NAME = "/name";

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(object, operation)) {
            GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
            Group group = (Group) object;
            checkModelForExistingValue(group);
            checkForProcessability(context, group);
            try {
                groupService.setName(group, (String) operation.getValue());
            } catch (SQLException e) {
                throw new DSpaceBadRequestException(
                        "SQLException in GroupNameReplaceOperation.perform "
                                + "trying to replace the name of the group.", e);
            }
            return object;
        } else {
            throw new DSpaceBadRequestException(
                    "GroupNameReplaceOperation does not support this operation");
        }
    }

    /**
     * Checks whether the name of Group has an existing value to replace
     *
     * @param group Object on which patch is being done
     */
    private void checkModelForExistingValue(Group group) {
        if (group.getName() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value (name).");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof Group
                && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_NAME));
    }

    /**
     * Checks whether the Group is unprocessable
     *
     * @param context the context
     * @param group Object on which patch is being done
     */
    private void checkForProcessability(Context context, Group group) {
        try {
            if (group.isPermanent()) {
                throw new UnprocessableEntityException("A permanent group's name cannot be patched");
            }
            final DSpaceObject parentObject = groupService.getParentObject(context, group);
            if (parentObject != null) {
                throw new UnprocessableEntityException(
                        "This group's name cannot be patched"
                                + " as it has a parent " + parentObject.getType()
                                + " with id " + parentObject.getID()
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
