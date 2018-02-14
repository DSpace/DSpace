--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

------------------------------------------------------
-- DS-2701 Service based API / Hibernate integration
------------------------------------------------------
UPDATE collection SET workflow_step_1 = null;
UPDATE collection SET workflow_step_2 = null;
UPDATE collection SET workflow_step_3 = null;

-- cwf_workflowitem

ALTER TABLE cwf_workflowitem RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE cwf_workflowitem ADD item_id RAW(16) REFERENCES Item(uuid);
UPDATE cwf_workflowitem SET item_id = (SELECT item.uuid FROM item WHERE cwf_workflowitem.item_legacy_id = item.item_id);
DECLARE
  COUNT_INDEXES INTEGER;
  INDEX_NAME VARCHAR(256);
  TABLE_NAME_D VARCHAR(256) := 'cwf_workflowitem';
  COLUMN_NAME_D VARCHAR(256) := 'item_legacy_id';
BEGIN
  select count(c.INDEX_NAME) into COUNT_INDEXES
  from USER_INDEXES i, USER_IND_COLUMNS c
  where i.TABLE_NAME = upper(TABLE_NAME_D)
        and i.UNIQUENESS = 'UNIQUE'
        and i.TABLE_NAME = c.TABLE_NAME
        and i.INDEX_NAME = c.INDEX_NAME
        and c.COLUMN_NAME = upper(COLUMN_NAME_D);

  IF COUNT_INDEXES > 0 THEN
    select c.INDEX_NAME into INDEX_NAME
    from USER_INDEXES i, USER_IND_COLUMNS c
    where i.TABLE_NAME = upper(TABLE_NAME_D)
          and i.UNIQUENESS = 'UNIQUE'
          and i.TABLE_NAME = c.TABLE_NAME
          and i.INDEX_NAME = c.INDEX_NAME
          and c.COLUMN_NAME = upper(COLUMN_NAME_D);
    EXECUTE IMMEDIATE 'ALTER TABLE ' || TABLE_NAME_D || ' DROP constraint ' || INDEX_NAME;
  END IF;
END;
/

ALTER TABLE cwf_workflowitem DROP COLUMN item_legacy_id;

ALTER TABLE cwf_workflowitem RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE cwf_workflowitem ADD collection_id RAW(16) REFERENCES Collection(uuid);
UPDATE cwf_workflowitem SET collection_id = (SELECT collection.uuid FROM collection WHERE cwf_workflowitem.collection_legacy_id = collection.collection_id);
DECLARE
  COUNT_INDEXES INTEGER;
  INDEX_NAME VARCHAR(256);
  TABLE_NAME_D VARCHAR(256) := 'cwf_workflowitem';
  COLUMN_NAME_D VARCHAR(256) := 'collection_legacy_id';
BEGIN
  select count(c.INDEX_NAME) into COUNT_INDEXES
  from USER_INDEXES i, USER_IND_COLUMNS c
  where i.TABLE_NAME = upper(TABLE_NAME_D)
        and i.UNIQUENESS = 'UNIQUE'
        and i.TABLE_NAME = c.TABLE_NAME
        and i.INDEX_NAME = c.INDEX_NAME
        and c.COLUMN_NAME = upper(COLUMN_NAME_D);

  IF COUNT_INDEXES > 0 THEN
    select c.INDEX_NAME into INDEX_NAME
    from USER_INDEXES i, USER_IND_COLUMNS c
    where i.TABLE_NAME = upper(TABLE_NAME_D)
          and i.UNIQUENESS = 'UNIQUE'
          and i.TABLE_NAME = c.TABLE_NAME
          and i.INDEX_NAME = c.INDEX_NAME
          and c.COLUMN_NAME = upper(COLUMN_NAME_D);
    EXECUTE IMMEDIATE 'ALTER TABLE ' || TABLE_NAME_D || ' DROP constraint ' || INDEX_NAME;
  END IF;
END;
/

ALTER TABLE cwf_workflowitem DROP COLUMN collection_legacy_id;

UPDATE cwf_workflowitem SET multiple_titles = '0' WHERE multiple_titles IS NULL;
UPDATE cwf_workflowitem SET published_before = '0' WHERE published_before IS NULL;
UPDATE cwf_workflowitem SET multiple_files = '0' WHERE multiple_files IS NULL;

-- cwf_collectionrole
ALTER TABLE cwf_collectionrole RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE cwf_collectionrole ADD collection_id RAW(16) REFERENCES Collection(uuid);
UPDATE cwf_collectionrole SET collection_id = (SELECT collection.uuid FROM collection WHERE cwf_collectionrole.collection_legacy_id = collection.collection_id);
DECLARE
  COUNT_INDEXES INTEGER;
  INDEX_NAME VARCHAR(256);
  TABLE_NAME_D VARCHAR(256) := 'cwf_collectionrole';
  COLUMN_NAME_D VARCHAR(256) := 'collection_legacy_id';
BEGIN
  select count(c.INDEX_NAME) into COUNT_INDEXES
  from USER_INDEXES i, USER_IND_COLUMNS c
  where i.TABLE_NAME = upper(TABLE_NAME_D)
        and i.UNIQUENESS = 'UNIQUE'
        and i.TABLE_NAME = c.TABLE_NAME
        and i.INDEX_NAME = c.INDEX_NAME
        and c.COLUMN_NAME = upper(COLUMN_NAME_D);

  IF COUNT_INDEXES > 0 THEN
    select c.INDEX_NAME into INDEX_NAME
    from USER_INDEXES i, USER_IND_COLUMNS c
    where i.TABLE_NAME = upper(TABLE_NAME_D)
          and i.UNIQUENESS = 'UNIQUE'
          and i.TABLE_NAME = c.TABLE_NAME
          and i.INDEX_NAME = c.INDEX_NAME
          and c.COLUMN_NAME = upper(COLUMN_NAME_D);
    EXECUTE IMMEDIATE 'ALTER TABLE ' || TABLE_NAME_D || ' DROP constraint ' || INDEX_NAME;
  END IF;
END;
/

ALTER TABLE cwf_collectionrole DROP COLUMN collection_legacy_id;

ALTER TABLE cwf_collectionrole RENAME COLUMN group_id to group_legacy_id;
ALTER TABLE cwf_collectionrole ADD group_id RAW(16) REFERENCES epersongroup(uuid);
UPDATE cwf_collectionrole SET group_id = (SELECT epersongroup.uuid FROM epersongroup WHERE cwf_collectionrole.group_legacy_id = epersongroup.eperson_group_id);
DECLARE
  COUNT_INDEXES INTEGER;
  INDEX_NAME VARCHAR(256);
  TABLE_NAME_D VARCHAR(256) := 'cwf_collectionrole';
  COLUMN_NAME_D VARCHAR(256) := 'group_legacy_id';
BEGIN
  select count(c.INDEX_NAME) into COUNT_INDEXES
  from USER_INDEXES i, USER_IND_COLUMNS c
  where i.TABLE_NAME = upper(TABLE_NAME_D)
        and i.UNIQUENESS = 'UNIQUE'
        and i.TABLE_NAME = c.TABLE_NAME
        and i.INDEX_NAME = c.INDEX_NAME
        and c.COLUMN_NAME = upper(COLUMN_NAME_D);

  IF COUNT_INDEXES > 0 THEN
    select c.INDEX_NAME into INDEX_NAME
    from USER_INDEXES i, USER_IND_COLUMNS c
    where i.TABLE_NAME = upper(TABLE_NAME_D)
          and i.UNIQUENESS = 'UNIQUE'
          and i.TABLE_NAME = c.TABLE_NAME
          and i.INDEX_NAME = c.INDEX_NAME
          and c.COLUMN_NAME = upper(COLUMN_NAME_D);
    EXECUTE IMMEDIATE 'ALTER TABLE ' || TABLE_NAME_D || ' DROP constraint ' || INDEX_NAME;
  END IF;
END;
/

ALTER TABLE cwf_collectionrole DROP COLUMN group_legacy_id;


-- cwf_workflowitemrole
ALTER TABLE cwf_workflowitemrole RENAME COLUMN group_id to group_legacy_id;
ALTER TABLE cwf_workflowitemrole ADD group_id RAW(16) REFERENCES epersongroup(uuid);
UPDATE cwf_workflowitemrole SET group_id = (SELECT epersongroup.uuid FROM epersongroup WHERE cwf_workflowitemrole.group_legacy_id = epersongroup.eperson_group_id);
DECLARE
  COUNT_INDEXES INTEGER;
  INDEX_NAME VARCHAR(256);
  TABLE_NAME_D VARCHAR(256) := 'cwf_workflowitemrole';
  COLUMN_NAME_D VARCHAR(256) := 'group_legacy_id';
BEGIN
  select count(c.INDEX_NAME) into COUNT_INDEXES
  from USER_INDEXES i, USER_IND_COLUMNS c
  where i.TABLE_NAME = upper(TABLE_NAME_D)
        and i.UNIQUENESS = 'UNIQUE'
        and i.TABLE_NAME = c.TABLE_NAME
        and i.INDEX_NAME = c.INDEX_NAME
        and c.COLUMN_NAME = upper(COLUMN_NAME_D);

  IF COUNT_INDEXES > 0 THEN
    select c.INDEX_NAME into INDEX_NAME
    from USER_INDEXES i, USER_IND_COLUMNS c
    where i.TABLE_NAME = upper(TABLE_NAME_D)
          and i.UNIQUENESS = 'UNIQUE'
          and i.TABLE_NAME = c.TABLE_NAME
          and i.INDEX_NAME = c.INDEX_NAME
          and c.COLUMN_NAME = upper(COLUMN_NAME_D);
    EXECUTE IMMEDIATE 'ALTER TABLE ' || TABLE_NAME_D || ' DROP constraint ' || INDEX_NAME;
  END IF;
END;
/

ALTER TABLE cwf_workflowitemrole DROP COLUMN group_legacy_id;

ALTER TABLE cwf_workflowitemrole RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE cwf_workflowitemrole ADD eperson_id RAW(16) REFERENCES eperson(uuid);
UPDATE cwf_workflowitemrole SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE cwf_workflowitemrole.eperson_legacy_id = eperson.eperson_id);
DECLARE
  COUNT_INDEXES INTEGER;
  INDEX_NAME VARCHAR(256);
  TABLE_NAME_D VARCHAR(256) := 'cwf_workflowitemrole';
  COLUMN_NAME_D VARCHAR(256) := 'eperson_legacy_id';
BEGIN
  select count(c.INDEX_NAME) into COUNT_INDEXES
  from USER_INDEXES i, USER_IND_COLUMNS c
  where i.TABLE_NAME = upper(TABLE_NAME_D)
        and i.UNIQUENESS = 'UNIQUE'
        and i.TABLE_NAME = c.TABLE_NAME
        and i.INDEX_NAME = c.INDEX_NAME
        and c.COLUMN_NAME = upper(COLUMN_NAME_D);

  IF COUNT_INDEXES > 0 THEN
    select c.INDEX_NAME into INDEX_NAME
    from USER_INDEXES i, USER_IND_COLUMNS c
    where i.TABLE_NAME = upper(TABLE_NAME_D)
          and i.UNIQUENESS = 'UNIQUE'
          and i.TABLE_NAME = c.TABLE_NAME
          and i.INDEX_NAME = c.INDEX_NAME
          and c.COLUMN_NAME = upper(COLUMN_NAME_D);
    EXECUTE IMMEDIATE 'ALTER TABLE ' || TABLE_NAME_D || ' DROP constraint ' || INDEX_NAME;
  END IF;
END;
/

ALTER TABLE cwf_workflowitemrole DROP COLUMN eperson_legacy_id;

-- cwf_pooltask
ALTER TABLE cwf_pooltask RENAME COLUMN group_id to group_legacy_id;
ALTER TABLE cwf_pooltask ADD group_id RAW(16) REFERENCES epersongroup(uuid);
UPDATE cwf_pooltask SET group_id = (SELECT epersongroup.uuid FROM epersongroup WHERE cwf_pooltask.group_legacy_id = epersongroup.eperson_group_id);
DECLARE
  COUNT_INDEXES INTEGER;
  INDEX_NAME VARCHAR(256);
  TABLE_NAME_D VARCHAR(256) := 'cwf_pooltask';
  COLUMN_NAME_D VARCHAR(256) := 'group_legacy_id';
BEGIN
  select count(c.INDEX_NAME) into COUNT_INDEXES
  from USER_INDEXES i, USER_IND_COLUMNS c
  where i.TABLE_NAME = upper(TABLE_NAME_D)
        and i.UNIQUENESS = 'UNIQUE'
        and i.TABLE_NAME = c.TABLE_NAME
        and i.INDEX_NAME = c.INDEX_NAME
        and c.COLUMN_NAME = upper(COLUMN_NAME_D);

  IF COUNT_INDEXES > 0 THEN
    select c.INDEX_NAME into INDEX_NAME
    from USER_INDEXES i, USER_IND_COLUMNS c
    where i.TABLE_NAME = upper(TABLE_NAME_D)
          and i.UNIQUENESS = 'UNIQUE'
          and i.TABLE_NAME = c.TABLE_NAME
          and i.INDEX_NAME = c.INDEX_NAME
          and c.COLUMN_NAME = upper(COLUMN_NAME_D);
    EXECUTE IMMEDIATE 'ALTER TABLE ' || TABLE_NAME_D || ' DROP constraint ' || INDEX_NAME;
  END IF;
END;
/

ALTER TABLE cwf_pooltask DROP COLUMN group_legacy_id;

ALTER TABLE cwf_pooltask RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE cwf_pooltask ADD eperson_id RAW(16) REFERENCES eperson(uuid);
UPDATE cwf_pooltask SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE cwf_pooltask.eperson_legacy_id = eperson.eperson_id);
DECLARE
  COUNT_INDEXES INTEGER;
  INDEX_NAME VARCHAR(256);
  TABLE_NAME_D VARCHAR(256) := 'cwf_pooltask';
  COLUMN_NAME_D VARCHAR(256) := 'eperson_legacy_id';
BEGIN
  select count(c.INDEX_NAME) into COUNT_INDEXES
  from USER_INDEXES i, USER_IND_COLUMNS c
  where i.TABLE_NAME = upper(TABLE_NAME_D)
        and i.UNIQUENESS = 'UNIQUE'
        and i.TABLE_NAME = c.TABLE_NAME
        and i.INDEX_NAME = c.INDEX_NAME
        and c.COLUMN_NAME = upper(COLUMN_NAME_D);

  IF COUNT_INDEXES > 0 THEN
    select c.INDEX_NAME into INDEX_NAME
    from USER_INDEXES i, USER_IND_COLUMNS c
    where i.TABLE_NAME = upper(TABLE_NAME_D)
          and i.UNIQUENESS = 'UNIQUE'
          and i.TABLE_NAME = c.TABLE_NAME
          and i.INDEX_NAME = c.INDEX_NAME
          and c.COLUMN_NAME = upper(COLUMN_NAME_D);
    EXECUTE IMMEDIATE 'ALTER TABLE ' || TABLE_NAME_D || ' DROP constraint ' || INDEX_NAME;
  END IF;
END;
/

ALTER TABLE cwf_pooltask DROP COLUMN eperson_legacy_id;

-- cwf_claimtask
ALTER TABLE cwf_claimtask RENAME COLUMN owner_id to eperson_legacy_id;
ALTER TABLE cwf_claimtask ADD owner_id RAW(16) REFERENCES eperson(uuid);
UPDATE cwf_claimtask SET owner_id = (SELECT eperson.uuid FROM eperson WHERE cwf_claimtask.eperson_legacy_id = eperson.eperson_id);
DECLARE
  COUNT_INDEXES INTEGER;
  INDEX_NAME VARCHAR(256);
  TABLE_NAME_D VARCHAR(256) := 'cwf_claimtask';
  COLUMN_NAME_D VARCHAR(256) := 'eperson_legacy_id';
BEGIN
  select count(c.INDEX_NAME) into COUNT_INDEXES
  from USER_INDEXES i, USER_IND_COLUMNS c
  where i.TABLE_NAME = upper(TABLE_NAME_D)
        and i.UNIQUENESS = 'UNIQUE'
        and i.TABLE_NAME = c.TABLE_NAME
        and i.INDEX_NAME = c.INDEX_NAME
        and c.COLUMN_NAME = upper(COLUMN_NAME_D);

  IF COUNT_INDEXES > 0 THEN
    select c.INDEX_NAME into INDEX_NAME
    from USER_INDEXES i, USER_IND_COLUMNS c
    where i.TABLE_NAME = upper(TABLE_NAME_D)
          and i.UNIQUENESS = 'UNIQUE'
          and i.TABLE_NAME = c.TABLE_NAME
          and i.INDEX_NAME = c.INDEX_NAME
          and c.COLUMN_NAME = upper(COLUMN_NAME_D);
    EXECUTE IMMEDIATE 'ALTER TABLE ' || TABLE_NAME_D || ' DROP constraint ' || INDEX_NAME;
  END IF;
END;
/

ALTER TABLE cwf_claimtask DROP COLUMN eperson_legacy_id;


-- cwf_in_progress_user
ALTER TABLE cwf_in_progress_user RENAME COLUMN user_id to eperson_legacy_id;
ALTER TABLE cwf_in_progress_user ADD user_id RAW(16) REFERENCES eperson(uuid);
UPDATE cwf_in_progress_user SET user_id = (SELECT eperson.uuid FROM eperson WHERE cwf_in_progress_user.eperson_legacy_id = eperson.eperson_id);
DECLARE
  COUNT_INDEXES INTEGER;
  INDEX_NAME VARCHAR(256);
  TABLE_NAME_D VARCHAR(256) := 'cwf_in_progress_user';
  COLUMN_NAME_D VARCHAR(256) := 'eperson_legacy_id';
BEGIN
  select count(c.INDEX_NAME) into COUNT_INDEXES
  from USER_INDEXES i, USER_IND_COLUMNS c
  where i.TABLE_NAME = upper(TABLE_NAME_D)
        and i.UNIQUENESS = 'UNIQUE'
        and i.TABLE_NAME = c.TABLE_NAME
        and i.INDEX_NAME = c.INDEX_NAME
        and c.COLUMN_NAME = upper(COLUMN_NAME_D);

  IF COUNT_INDEXES > 0 THEN
    select c.INDEX_NAME into INDEX_NAME
    from USER_INDEXES i, USER_IND_COLUMNS c
    where i.TABLE_NAME = upper(TABLE_NAME_D)
          and i.UNIQUENESS = 'UNIQUE'
          and i.TABLE_NAME = c.TABLE_NAME
          and i.INDEX_NAME = c.INDEX_NAME
          and c.COLUMN_NAME = upper(COLUMN_NAME_D);
    EXECUTE IMMEDIATE 'ALTER TABLE ' || TABLE_NAME_D || ' DROP constraint ' || INDEX_NAME;
  END IF;
END;
/

ALTER TABLE cwf_in_progress_user DROP COLUMN eperson_legacy_id;
UPDATE cwf_in_progress_user SET finished = '0' WHERE finished IS NULL;

