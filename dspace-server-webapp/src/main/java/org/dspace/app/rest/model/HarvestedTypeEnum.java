package org.dspace.app.rest.model;

public enum HarvestedTypeEnum {
    NONE(0),
    METADATA_ONLY(1),
    METADATA_AND_REF(2),
    METADATA_AND_BITSTREAMS(3);

    private int harvestType;

    HarvestedTypeEnum(int harvestType) {
        this.harvestType = harvestType;
    }

    public int getValue() {
        return harvestType;
    }
}
