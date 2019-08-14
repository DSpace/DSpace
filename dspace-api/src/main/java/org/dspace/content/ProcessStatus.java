/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

/**
 * This Enum holds a representation of all the possible states that a Process can be in
 */
public enum ProcessStatus {
    SCHEDULED,
    RUNNING,
    COMPLETED,
    FAILED

}
