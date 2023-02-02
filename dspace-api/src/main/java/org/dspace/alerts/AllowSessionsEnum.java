/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.alerts;

/**
 * Enum representing the options for allowing sessions:
 *  ALLOW_ALL_SESSIONS -    Will allow all users to log in and continue their sessions
 *  ALLOW_CURRENT_SESSIONS_ONLY -   Will prevent non admin users from logging in, however logged-in users
 *                                  will remain logged in
 *  ALLOW_ADMIN_SESSIONS_ONLY - Only admin users can log in, non admin sessions will be interrupted
 *
 *  NOTE: This functionality can be stored in the database, but no support is present right now to interrupt and prevent
 *  sessions.
 */
public enum AllowSessionsEnum {
    ALLOW_ALL_SESSIONS("all"),
    ALLOW_CURRENT_SESSIONS_ONLY("current"),
    ALLOW_ADMIN_SESSIONS_ONLY("admin");

    private String allowSessionsType;

    AllowSessionsEnum(String allowSessionsType) {
        this.allowSessionsType = allowSessionsType;
    }

    public String getValue() {
        return allowSessionsType;
    }

    public static AllowSessionsEnum fromString(String alertAllowSessionType) {
        if (alertAllowSessionType == null) {
            return AllowSessionsEnum.ALLOW_ALL_SESSIONS;
        }

        switch (alertAllowSessionType) {
            case "all":
                return AllowSessionsEnum.ALLOW_ALL_SESSIONS;
            case "current":
                return AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY;
            case "admin" :
                return AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY;
            default:
                throw new IllegalArgumentException("No corresponding enum value for provided string: "
                                                           + alertAllowSessionType);
        }
    }


}
