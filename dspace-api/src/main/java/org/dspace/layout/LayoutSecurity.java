/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * This enum defines the possible values for the security of the layout components
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public enum LayoutSecurity {
    PUBLIC(0),
    ADMINISTRATOR(1),
    OWNER_ONLY(2),
    OWNER_AND_ADMINISTRATOR(3),
    CUSTOM_DATA(4);

    private int value;
    private LayoutSecurity(int value) {
        this.value = value;
    }
    public int getValue() {
        return this.value;
    }

    /**
     * Returns the LayoutSecurity instance associated with the value passed in input.
     * Null if not managed
     * @param val
     * @return {@link LayoutSecurity}
     */
    public static LayoutSecurity valueOf(int val) {
        LayoutSecurity value = null;
        switch (val) {
            case 0:
                value = LayoutSecurity.PUBLIC;
                break;
            case 1:
                value = LayoutSecurity.ADMINISTRATOR;
                break;
            case 2:
                value = LayoutSecurity.OWNER_ONLY;
                break;
            case 3:
                value = LayoutSecurity.OWNER_AND_ADMINISTRATOR;
                break;
            case 4:
                value = LayoutSecurity.CUSTOM_DATA;
                break;
            default:
                value = null;
        }
        return value;
    }
}
