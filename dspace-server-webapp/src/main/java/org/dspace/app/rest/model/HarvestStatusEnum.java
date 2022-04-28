/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

/**
 * An enum containing all the possible harvest statuses
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
public enum HarvestStatusEnum {
    READY(0),
    BUSY(1),
    QUEUED(2),
    OAI_ERROR(3),
    UNKNOWN_ERROR(-1);

    private int harvestStatus;

    HarvestStatusEnum(int harvestStatus) {
        this.harvestStatus = harvestStatus;
    }

    public int getValue() {
        return harvestStatus;
    }

    public static HarvestStatusEnum fromInt(Integer harvestStatus) {
        if (harvestStatus == null) {
            return null;
        }

        switch (harvestStatus) {
            case -1: return HarvestStatusEnum.UNKNOWN_ERROR;
            case 0: return HarvestStatusEnum.READY;
            case 1: return HarvestStatusEnum.BUSY;
            case 2: return HarvestStatusEnum.QUEUED;
            case 3: return HarvestStatusEnum.OAI_ERROR;
            default: throw new IllegalArgumentException("No corresponding enum value for integer");
        }
    }
}
