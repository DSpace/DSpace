/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.supervision.enumeration;


/**
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public enum SupervisionOrderType {
    OBSERVER,
    NONE,
    EDITOR;

    public static boolean invalid(String type) {
        try {
            SupervisionOrderType.valueOf(type);
            return false;
        } catch (IllegalArgumentException ignored) {
            return true;
        }
    }
}
