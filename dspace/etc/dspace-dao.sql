alter table handle add column namespace text;

create view externalidentifier as
(
    select handle_id as id,
        handle as value,
        resource_type_id,
        resource_id,
        namespace
    from handle
    order by handle_id
);

alter table bitstream add column uuid varchar(36);
alter table bundle add column uuid varchar(36);
alter table item add column uuid varchar(36);
alter table collection add column uuid varchar(36);
alter table community add column uuid varchar(36);
alter table eperson add column uuid varchar(36);
alter table epersongroup add column uuid varchar(36);
alter table workspaceitem add column uuid varchar(36);
alter table bitstreamformatregistry add column uuid varchar(36);
alter table resourcepolicy add column uuid varchar(36);
alter table workflowitem add column uuid varchar(36);
alter table subscription add column uuid varchar(36);
alter table metadataschemaregistry add column uuid varchar(36);
alter table metadatafieldregistry add column uuid varchar(36);
alter table metadatavalue add column uuid varchar(36);

alter table metadatafieldregistry alter column metadata_schema_id drop not null;

update epersongroup set uuid = '3aa7309d-1bef-4f24-bd1e-ff7921238259' where eperson_group_id = 0;
update epersongroup set uuid = 'd3e477f0-d28f-413f-8a38-4379279814ed' where eperson_group_id = 1;
