/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

import java.util.stream.Stream;

public enum ImportAction {

    ADD,
    UPDATE,
    DELETE,
    NOT_SPECIFIED;

    public static boolean isValid(String actionAsString) {
        return Stream.of(values()).anyMatch(action -> action.name().equalsIgnoreCase(actionAsString));
    }

}
