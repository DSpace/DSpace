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
--   @updateseq-oracle.sql my_sequence my_table my_attribute
--
--------------------------------------------------------------------------------
--
SET SERVEROUTPUT ON SIZE 1000000;
--
DECLARE
  dummy NUMBER := 0;
  curr  NUMBER := 0;
BEGIN
  --
  --SELECT &1..nextval INTO dummy FROM dual;
  --dbms_output.put('start with next value=' || dummy);
  --
  SELECT max(&3) INTO curr FROM &2;

  DROP SEQUENCE &1;
  CREATE SEQUENCE &1 START WITH curr;

  --
  dbms_output.put_line(', end=' || dummy);
  --
END;
/
