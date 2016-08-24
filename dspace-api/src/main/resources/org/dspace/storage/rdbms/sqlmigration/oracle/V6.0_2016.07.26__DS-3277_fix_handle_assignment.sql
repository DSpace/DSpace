--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

----------------------------------------------------------------------------------
-- DS-3277 : 'handle_id' column needs its own separate sequence, so that Handles 
-- can be minted from 'handle_seq'
----------------------------------------------------------------------------------
-- Create a new sequence for 'handle_id' column.
-- The role of this sequence is to simply provide a unique internal ID to the database.
CREATE SEQUENCE handle_id_seq;
-- Initialize new 'handle_id_seq' to the maximum value of 'handle_id'
DECLARE
  curr  NUMBER := 0;
BEGIN
  SELECT max(handle_id) INTO curr FROM handle;

  curr := curr + 1;

  EXECUTE IMMEDIATE 'DROP SEQUENCE handle_id_seq';

  EXECUTE IMMEDIATE 'CREATE SEQUENCE handle_id_seq START WITH ' || NVL(curr,1);
END;
/

-- Ensure the 'handle_seq' is updated to the maximum *suffix* in 'handle' column,
-- as this sequence is used to mint new Handles.
-- Code borrowed from update-sequences.sql and updateseq.sql
DECLARE
  curr  NUMBER := 0;
BEGIN
  SELECT max(to_number(regexp_replace(handle, '.*/', ''), '999999999999')) INTO curr FROM handle WHERE REGEXP_LIKE(handle, '^.*/[0123456789]*$');

  curr := curr + 1;

  EXECUTE IMMEDIATE 'DROP SEQUENCE handle_seq';

  EXECUTE IMMEDIATE 'CREATE SEQUENCE handle_seq START WITH ' || NVL(curr,1);
END;
/