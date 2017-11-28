--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--------------------------------------------------------------------
-- DS-3730 Index on given tables to speed up resource policy queries
--------------------------------------------------------------------

declare
    index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX resourcepolicy_idx_start_date';
  exception
  when index_not_exists then null;
end;
/
CREATE INDEX resourcepolicy_idx_start_date ON resourcepolicy(start_date);

declare
    index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX resourcepolicy_idx_end_date';
  exception
  when index_not_exists then null;
end;
/
CREATE INDEX resourcepolicy_idx_end_date ON resourcepolicy(end_date);

declare
    index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX resourcepolicy_idx_action_id';
  exception
  when index_not_exists then null;
end;
/
CREATE INDEX resourcepolicy_idx_action_id ON resourcepolicy(action_id);