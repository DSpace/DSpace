DELETE FROM ONLY conceptmetadatavalue AS cmv
    USING metadataschemaregistry AS msr,
        metadatafieldregistry AS mfr
    WHERE mfr.metadata_field_id = cmv.field_id
        AND mfr.element='subscriptionPaid'
        AND mfr.metadata_schema_id = msr.metadata_schema_id
        AND msr.short_id='journal';

DELETE FROM ONLY metadatafieldregistry AS mfr USING metadataschemaregistry AS msr
    WHERE mfr.element='subscriptionPaid'
        AND mfr.metadata_schema_id = msr.metadata_schema_id
        AND msr.short_id='journal';