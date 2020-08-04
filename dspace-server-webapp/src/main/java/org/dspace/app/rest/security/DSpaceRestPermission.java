/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import org.dspace.core.Constants;

/**
 * Enum that lists all available "permissions" an authenticated user can have on a specific REST endpoint.
 */
public enum DSpaceRestPermission {

    READ(Constants.READ),
    WRITE(Constants.WRITE),
    DELETE(Constants.DELETE),
    ADD(Constants.ADD),
    ADMIN(Constants.ADMIN);

    private int dspaceApiActionId;

    DSpaceRestPermission(int dspaceApiActionId) {
        this.dspaceApiActionId = dspaceApiActionId;
    }

    public int getDspaceApiActionId() {
        return dspaceApiActionId;
    }

    /**
     * Convert a given object to a {@link DSpaceRestPermission} if possible.
     * @param object The object to convert
     * @return A DSpaceRestPersmission value if the conversion succeeded, null otherwise
     */
    public static DSpaceRestPermission convert(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof DSpaceRestPermission) {
            return (DSpaceRestPermission) object;
        } else if (object instanceof String) {
            try {
                return DSpaceRestPermission.valueOf((String) object);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        } else {
            return null;
        }
    }

}
