--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- SQL code to update the ID (primary key) generating sequences, if some
-- import operation has set explicit IDs.
--
-- Sequences are used to generate IDs for new rows in the database.  If a
-- bulk import operation, such as an SQL dump, specifies primary keys for
-- imported data explicitly, the sequences are out of sync and need updating.
-- This SQL code does just that.
--
-- This should rarely be needed; any bulk import should be performed using the
-- org.dspace.content API which is safe to use concurrently and in multiple
-- JVMs.  The SQL code below will typically only be required after a direct
-- SQL data dump from a backup or somesuch.

SELECT setval('alert_id_seq', max(alert_id)) FROM systemwidealert;
SELECT setval('bitstreamformatregistry_seq', max(bitstream_format_id)) FROM bitstreamformatregistry;
SELECT setval('checksum_history_check_id_seq', max(check_id)) FROM checksum_history;
SELECT setval('cwf_claimtask_seq', max(claimtask_id)) FROM cwf_claimtask;
SELECT setval('cwf_collectionrole_seq', max(collectionrole_id)) FROM cwf_collectionrole;
SELECT setval('cwf_in_progress_user_seq', max(in_progress_user_id)) FROM cwf_in_progress_user;
SELECT setval('cwf_pooltask_seq', max(pooltask_id)) FROM cwf_pooltask;
SELECT setval('cwf_workflowitem_seq', max(workflowitem_id)) FROM cwf_workflowitem;
SELECT setval('cwf_workflowitemrole_seq', max(workflowitemrole_id)) FROM cwf_workflowitemrole;
SELECT setval('doi_seq', max(doi_id)) FROM doi;
SELECT setval('entity_type_id_seq', max(id)) FROM entity_type;
SELECT setval('fileextension_seq', max(file_extension_id)) FROM fileextension;
SELECT setval('handle_id_seq', max(handle_id)) FROM handle;
SELECT setval('harvested_collection_seq', max(id)) FROM harvested_collection;
SELECT setval('harvested_item_seq', max(id)) FROM harvested_item;
SELECT setval('metadatafieldregistry_seq', max(metadata_field_id)) FROM metadatafieldregistry;
SELECT setval('metadataschemaregistry_seq', max(metadata_schema_id)) FROM metadataschemaregistry;
SELECT setval('metadatavalue_seq', max(metadata_value_id)) FROM metadatavalue;
SELECT setval('openurltracker_seq', max(tracker_id)) FROM openurltracker;
SELECT setval('orcid_history_id_seq', max(id)) FROM orcid_history;
SELECT setval('orcid_queue_id_seq', max(id)) FROM orcid_queue;
SELECT setval('orcid_token_id_seq', max(id)) FROM orcid_token;
SELECT setval('process_id_seq', max(process_id)) FROM process;
SELECT setval('registrationdata_seq', max(registrationdata_id)) FROM registrationdata;
SELECT setval('relationship_id_seq', max(id)) FROM relationship;
SELECT setval('relationship_type_id_seq', max(id)) FROM relationship_type;
SELECT setval('requestitem_seq', max(requestitem_id)) FROM requestitem;
SELECT setval('resourcepolicy_seq', max(policy_id)) FROM resourcepolicy;
SELECT setval('subscription_parameter_seq', max(subscription_id)) FROM subscription_parameter;
SELECT setval('subscription_seq', max(subscription_id)) FROM subscription;
SELECT setval('supervision_orders_seq', max(id)) FROM supervision_orders;
SELECT setval('versionhistory_seq', max(versionhistory_id)) FROM versionhistory;
SELECT setval('versionitem_seq', max(versionitem_id)) FROM versionitem;
SELECT setval('webapp_seq', max(webapp_id)) FROM webapp;
SELECT setval('workspaceitem_seq', max(workspace_item_id)) FROM workspaceitem;

-- Handle Sequence is a special case.  Since Handles minted by DSpace use the 'handle_seq',
-- we need to ensure the next assigned handle will *always* be unique.  So, 'handle_seq'
-- always needs to be set to the value of the *largest* handle suffix.  That way when the
-- next handle is assigned, it will use the next largest number. This query does the following:
--  For all 'handle' values which have a number in their suffix (after '/'), find the maximum
--  suffix value, convert it to a 'bigint' type, and set the 'handle_seq' to that max value.
SELECT setval('handle_seq',
              CAST (
                    max(
                        to_number(regexp_replace(handle, '.*/', ''), '999999999999')
                       )
                    AS BIGINT)
             )
    FROM handle
    WHERE handle SIMILAR TO '%/[0123456789]*';
