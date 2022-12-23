/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.alerts;

/**
 * Enum representing the options for allowing sessions
 */
public enum AllowSessionsEnum {
    ALL(0),
    CURRENT(1),
    ADMIN(2);

    private int allowSessionsType;

    AllowSessionsEnum(int allowSessionsType) {
        this.allowSessionsType = allowSessionsType;
    }

    public int getValue() {
        return allowSessionsType;
    }

    public static AllowSessionsEnum fromInt(Integer alertAllowSessionType) {
        if (alertAllowSessionType == null) {
            return AllowSessionsEnum.ALL;
        }

        switch (alertAllowSessionType) {
            case 0:
                return AllowSessionsEnum.ALL;
            case 1:
                return AllowSessionsEnum.CURRENT;
            case 2:
                return AllowSessionsEnum.ADMIN;
            default:
                throw new IllegalArgumentException("No corresponding enum value for integer");
        }
    }


}
