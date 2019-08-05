package org.dspace.app.rest.model;

import org.dspace.harvest.HarvestedCollection;

public enum HarvestedTypeEnum {
    TYPE_NONE(0),
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

    private HarvestedTypeEnum fromString(String harvestType) {
        int type;

        /** if (harvestType.equals("METADATA_ONLY")) {
            type = HarvestedCollection.TYPE_DMD;
        } else if (harvestType.equals("METADATA_AND_REF")) {
            type = HarvestedCollection.TYPE_DMDREF;
        } else if (harvestType.equals("METADATA_AND_BITSTREAMS")) {
            type = HarvestedCollection.TYPE_FULL;
        } else {
            throw new IllegalArgumentException();
        } **/

        return HarvestedTypeEnum.valueOf(harvestType);
    }
}
