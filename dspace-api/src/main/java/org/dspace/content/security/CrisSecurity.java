/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

/**
 * Enum that model all the allowed values for an item access security
 * configuration.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum CrisSecurity {

    NONE,
    ADMIN,
    OWNER,
    CUSTOM,
    ITEM_ADMIN,
    SUBMITTER,
    SUBMITTER_GROUP,
    GROUP,
    ALL;

}
