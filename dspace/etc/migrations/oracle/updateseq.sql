-- #############################################################################################
--
-- %Purpose: Set a sequence to the max value of a given attribute
--
-- #############################################################################################
--
-- Paramters:
--   1: sequence name
--   2: table name
--   3: attribute name
--
-- Sample usage:
--   @updateseq.sql my_sequence my_table my_attribute where-clause
--
--------------------------------------------------------------------------------
--
SET SERVEROUTPUT ON SIZE 1000000;
--
DECLARE
  curr  NUMBER := 0;
BEGIN
  SELECT max(&3) INTO curr FROM &2 &4;

  curr := curr + 1;

  EXECUTE IMMEDIATE 'DROP SEQUENCE &1';

  EXECUTE IMMEDIATE 'CREATE SEQUENCE &1 START WITH ' || NVL(curr,1);
END;
/
