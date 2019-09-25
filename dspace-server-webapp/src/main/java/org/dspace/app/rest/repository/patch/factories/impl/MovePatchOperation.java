package org.dspace.app.rest.repository.patch.factories.impl;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.MoveOperation;
import org.dspace.app.rest.model.patch.Operation;

/**
 * Created by kristof on 24/09/2019
 */
public abstract class MovePatchOperation<R extends RestModel, T> extends PatchOperation<R, T> {

    protected int from;
    protected int to;

    @Override
    public R perform(R resource, Operation operation) {
        checkMoveOperation(operation);
        return move(resource, operation);
    }

    abstract R move(R resource, Operation operation);

    private void checkMoveOperation(Operation operation) {
        if(!(operation instanceof MoveOperation)) {
            throw new DSpaceBadRequestException("Expected a MoveOperation, but received " + operation.getClass().getName() + " instead.");
        }
        from = getLocationFromPath(((MoveOperation)operation).getFrom());
        to = getLocationFromPath(operation.getPath());
        if(from == to) {
            throw new DSpaceBadRequestException("The \"from\" location must be different from the \"to\" location.");
        }
        if(from < 0) {
            throw new DSpaceBadRequestException("A negative \"from\" location was provided: " + from);
        }
        if(to < 0) {
            throw new DSpaceBadRequestException("A negative \"to\" location was provided: " + to);
        }
    }

    protected int getLocationFromPath(String path) {
        String[] parts = StringUtils.split(path, "/");
        String locationStr;
        if(parts.length > 1) {
            if(StringUtils.equals(parts[parts.length-1], "href")) {
                locationStr = parts[parts.length-2];
            } else {
                locationStr = parts[parts.length-1];
            }
        } else {
            locationStr = parts[0];
        }
        return Integer.parseInt(locationStr);
    }
}

