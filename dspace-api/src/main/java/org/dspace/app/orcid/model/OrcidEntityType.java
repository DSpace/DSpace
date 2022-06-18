/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model;

/**
 * The types of activities defined on ORCID that can be synchronized.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OrcidEntityType {

    /**
     * The publication/work activity.
     */
    PUBLICATION,

    /**
     * The funding activity.
     */
    FUNDING;

}
