--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Insert default relationship type on collections
-----------------------------------------------------------------------------------

INSERT INTO metadatavalue (confidence, place,text_value, dspace_object_id, metadata_field_id)
select -1, 0, 'Publication', uuid, (SELECT metadata_field_id
                       FROM metadatafieldregistry
                       WHERE element = 'type' and metadata_schema_id = (
                           SELECT metadata_schema_id
                           FROM metadataschemaregistry
                           WHERE short_id = 'relationship'
                       ))
FROM collection
where uuid not in (
    select c.uuid
    from collection c inner join metadatavalue m on m.dspace_object_id = c.uuid and m.metadata_field_id = (
        select metadata_field_id
        from metadatafieldregistry reg inner join metadataschemaregistry m2 on reg.metadata_schema_id = m2.metadata_schema_id
        where m2.short_id='relationship' and reg.element='type'
    )
)

