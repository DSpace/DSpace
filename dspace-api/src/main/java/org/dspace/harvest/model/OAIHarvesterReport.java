/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.model;

/**
 * A class that model a report of an OAI harvesting.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterReport {

    private int failureCount = 0;

    private int successCount = 0;

    private final int totalRecordSize;

    public OAIHarvesterReport(int totalRecordSize) {
        this.totalRecordSize = totalRecordSize;
    }

    public boolean noRecordImportFails() {
        return failureCount == 0;
    }

    public void incrementFailureCount() {
        this.failureCount++;
    }

    public void incrementSuccessCount() {
        this.successCount++;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public int getTotalRecordSize() {
        return totalRecordSize;
    }

    public int getCurrentRecord() {
        return successCount + failureCount + 1;
    }


}
