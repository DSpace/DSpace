/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.profile;

import static org.apache.commons.lang3.EnumUtils.isValidEnum;

/**
 * Enum that models all the available values of the property that which
 * determines which users can disconnect a profile from an ORCID account.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OrcidProfileDisconnectionMode {

    /**
     * The disconnection is disabled.
     */
    DISABLED,

    /**
     * Only the profile's owner can disconnect that profile from ORCID.
     */
    ONLY_OWNER,

    /**
     * Only the admins can disconnect profiles from ORCID.
     */
    ONLY_ADMIN,

    /**
     * Only the admin or the profile's owner can disconnect that profile from ORCID.
     */
    ADMIN_AND_OWNER;

    public static boolean isValid(String mode) {
        return mode != null ? isValidEnum(OrcidProfileDisconnectionMode.class, mode.toUpperCase()) : false;
    }

    public static OrcidProfileDisconnectionMode fromString(String mode) {
        return isValid(mode) ? OrcidProfileDisconnectionMode.valueOf(mode.toUpperCase()) : null;
    }

}
