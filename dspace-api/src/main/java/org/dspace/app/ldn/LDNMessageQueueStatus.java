/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

public enum LDNMessageQueueStatus {

    /**
     * Resulting processing status of an LDN Message (aka queue management)
     */
    QUEUED, PROCESSING, PROCESSED, FAILED;
}
