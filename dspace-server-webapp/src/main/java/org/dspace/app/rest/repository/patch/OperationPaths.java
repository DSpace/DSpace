/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch;

public enum OperationPaths {

    OPERATION_PASSWORD_CHANGE("/password");

    private final String path;

    OperationPaths(final String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return path;
    }
}
