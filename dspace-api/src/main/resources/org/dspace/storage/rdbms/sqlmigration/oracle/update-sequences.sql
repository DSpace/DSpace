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

-- Depends on being run from sqlplus with incseq.sql in the current path
-- you can find incseq.sql at: http://www.akadia.com/services/scripts/incseq.sql
-- Here that script was renamed to updateseq.sql.

DECLARE
 PROCEDURE updateseq ( seq IN VARCHAR,
                       tbl IN VARCHAR,
                       attr IN VARCHAR,
                       cond IN VARCHAR DEFAULT '' ) IS
   curr NUMBER := 0;
   BEGIN
     EXECUTE IMMEDIATE 'SELECT max(' || attr
             || ') FROM ' || tbl
             || ' ' || cond
             INTO curr;
     curr := curr + 1;
     EXECUTE IMMEDIATE 'DROP SEQUENCE ' || seq;
     EXECUTE IMMEDIATE 'CREATE SEQUENCE '
             || seq
             || ' START WITH '
             || NVL(curr, 1);
   END updateseq;

BEGIN
  updateseq('bitstreamformatregistry_seq', 'bitstreamformatregistry',
            'bitstream_format_id');
  updateseq('fileextension_seq', 'fileextension', 'file_extension_id');
  updateseq('resourcepolicy_seq', 'resourcepolicy', 'policy_id');
  updateseq('workspaceitem_seq', 'workspaceitem', 'workspace_item_id');
  updateseq('workflowitem_seq', 'workflowitem', 'workflow_id');
  updateseq('tasklistitem_seq', 'tasklistitem', 'tasklist_id');
  updateseq('registrationdata_seq', 'registrationdata',
            'registrationdata_id');
  updateseq('subscription_seq', 'subscription', 'subscription_id');
  updateseq('metadatafieldregistry_seq', 'metadatafieldregistry',
            'metadata_field_id');
  updateseq('metadatavalue_seq', 'metadatavalue', 'metadata_value_id');
  updateseq('metadataschemaregistry_seq', 'metadataschemaregistry',
            'metadata_schema_id');
  updateseq('harvested_collection_seq', 'harvested_collection', 'id');
  updateseq('harvested_item_seq', 'harvested_item', 'id');
  updateseq('webapp_seq', 'webapp', 'webapp_id');
  updateseq('requestitem_seq', 'requestitem', 'requestitem_id');
  updateseq('handle_id_seq', 'handle', 'handle_id');

  -- Handle Sequence is a special case.  Since Handles minted by DSpace
  -- use the 'handle_seq', we need to ensure the next assigned handle
  -- will *always* be unique.  So, 'handle_seq' always needs to be set
  -- to the value of the *largest* handle suffix.  That way when the
  -- next handle is assigned, it will use the next largest number. This
  -- query does the following:
  --   For all 'handle' values which have a number in their suffix
  --   (after '/'), find the maximum suffix value, convert it to a
  --   number, and set the 'handle_seq' to start at the next value (see
  --   updateseq above for more).
  updateseq('handle_seq', 'handle',
            q'{to_number(regexp_replace(handle, '.*/', ''), '999999999999')}',
            q'{WHERE REGEXP_LIKE(handle, '^.*/[0123456789]*$')}');
END;