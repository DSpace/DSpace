/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * An enum containing all the possible harvest types
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
public enum HarvestTypeEnum {
    NONE(0),
    METADATA_ONLY(1),
    METADATA_AND_REF(2),
    METADATA_AND_BITSTREAMS(3);

    private int harvestType;

    HarvestTypeEnum(int harvestType) {
        this.harvestType = harvestType;
    }

    public int getValue() {
        return harvestType;
    }

    /**
     * Creates an enum from the given integer
     * @param harvestType The harvest type
     * @return a harvestTypeEnum
     */
    public static HarvestTypeEnum fromInt(Integer harvestType) {
        if (harvestType == null) {
            return HarvestTypeEnum.NONE;
        }

        switch (harvestType) {
            case 0: return HarvestTypeEnum.NONE;
            case 1: return HarvestTypeEnum.METADATA_ONLY;
            case 2: return HarvestTypeEnum.METADATA_AND_REF;
            case 3: return HarvestTypeEnum.METADATA_AND_BITSTREAMS;
            default: throw new IllegalArgumentException("No corresponding enum value for integer");
        }
    }
}
