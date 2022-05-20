------------------------------------------------------
-- LIBDRUM-511 - Upgrade to Service based API / Hibernate integration
------------------------------------------------------
ALTER TABLE collection2etdunit DROP CONSTRAINT IF EXISTS collection2etdunit_etdunit_id_fkey;
ALTER TABLE epersongroup2unit DROP CONSTRAINT IF EXISTS epersongroup2unit_unit_id_fkey;

-- Migrate etdunit
-- ALTER TABLE etdunit ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
ALTER TABLE etdunit ADD COLUMN uuid UUID DEFAULT random_uuid();
ALTER TABLE etdunit ADD CONSTRAINT etdunit_uuid_unique UNIQUE(uuid);
--
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM etdunit;
ALTER TABLE etdunit ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE etdunit ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE etdunit ADD CONSTRAINT etdunit_id_unique UNIQUE(uuid);
ALTER TABLE etdunit DROP CONSTRAINT IF EXISTS etdunit_pkey;
-- ALTER TABLE etdunit ADD PRIMARY KEY (uuid);
ALTER TABLE etdunit DROP CONSTRAINT CONSTRAINT_F267;
ALTER TABLE etdunit DROP PRIMARY KEY;
ALTER TABLE etdunit ADD PRIMARY KEY (uuid);
--
ALTER TABLE etdunit ALTER COLUMN etdunit_id DROP NOT NULL;
CREATE INDEX etdunit_id_idx on etdunit(etdunit_id);

-- Migrate unit
-- ALTER TABLE unit ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
ALTER TABLE unit ADD COLUMN uuid UUID DEFAULT random_uuid();
ALTER TABLE unit ADD CONSTRAINT unit_uuid_unique UNIQUE(uuid);
--
INSERT INTO dspaceobject (uuid) SELECT uuid FROM unit;
ALTER TABLE unit ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE unit ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE unit ADD CONSTRAINT unit_id_unique UNIQUE(uuid);
ALTER TABLE unit DROP CONSTRAINT IF EXISTS unit_pkey;
-- ALTER TABLE unit ADD PRIMARY KEY (uuid);
ALTER TABLE unit DROP CONSTRAINT CONSTRAINT_472C;
ALTER TABLE unit DROP PRIMARY KEY;
ALTER TABLE unit ADD PRIMARY KEY (uuid);
--
ALTER TABLE unit ALTER COLUMN unit_id DROP NOT NULL;
CREATE INDEX unit_id_idx on unit(unit_id);

-- Migrate collection2etdunit
ALTER TABLE collection2etdunit ALTER COLUMN collection_id RENAME to collection_legacy_id;
ALTER TABLE collection2etdunit ALTER COLUMN etdunit_id RENAME to etdunit_legacy_id;
-- ALTER TABLE collection2etdunit ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
ALTER TABLE collection2etdunit ADD COLUMN collection_id UUID;
ALTER TABLE collection2etdunit ADD FOREIGN KEY (collection_id) REFERENCES Collection(uuid);
--
-- ALTER TABLE collection2etdunit ADD COLUMN etdunit_id UUID REFERENCES EtdUnit(uuid);
ALTER TABLE collection2etdunit ADD COLUMN etdunit_id UUID;
ALTER TABLE collection2etdunit ADD FOREIGN KEY (etdunit_id) REFERENCES EtdUnit(uuid);
--
UPDATE collection2etdunit SET collection_id = (SELECT Collection.uuid FROM Collection WHERE collection2etdunit.collection_legacy_id = Collection.collection_id);
UPDATE collection2etdunit SET etdunit_id = (SELECT EtdUnit.uuid FROM EtdUnit WHERE collection2etdunit.etdunit_legacy_id = EtdUnit.etdunit_id);
ALTER TABLE collection2etdunit ALTER COLUMN collection_id SET NOT NULL;
ALTER TABLE collection2etdunit ALTER COLUMN etdunit_id SET NOT NULL;
ALTER TABLE collection2etdunit DROP COLUMN collection_legacy_id;
ALTER TABLE collection2etdunit DROP COLUMN etdunit_legacy_id;
ALTER TABLE collection2etdunit DROP COLUMN id;
ALTER TABLE collection2etdunit add primary key (collection_id,etdunit_id);
CREATE INDEX collection2etdunit_collection on collection2etdunit(collection_id);
CREATE INDEX collection2etdunit_etdunit on collection2etdunit(etdunit_id);

-- Migrate epersongroup2unit
ALTER TABLE epersongroup2unit ALTER COLUMN eperson_group_id RENAME to eperson_group_legacy_id;
ALTER TABLE epersongroup2unit ALTER COLUMN unit_id RENAME to unit_legacy_id;
-- ALTER TABLE epersongroup2unit ADD COLUMN eperson_group_id UUID REFERENCES epersongroup(uuid);
ALTER TABLE epersongroup2unit ADD COLUMN eperson_group_id UUID;
ALTER TABLE epersongroup2unit ADD FOREIGN KEY (eperson_group_id) REFERENCES epersongroup(uuid);
--
-- ALTER TABLE epersongroup2unit ADD COLUMN unit_id UUID REFERENCES Unit(uuid);
ALTER TABLE epersongroup2unit ADD COLUMN unit_id UUID;
ALTER TABLE epersongroup2unit ADD FOREIGN KEY (unit_id) REFERENCES Unit(uuid);
---
UPDATE epersongroup2unit SET eperson_group_id = (SELECT epersongroup.uuid FROM epersongroup WHERE epersongroup2unit.eperson_group_legacy_id = epersongroup.eperson_group_id);
UPDATE epersongroup2unit SET unit_id = (SELECT Unit.uuid FROM Unit WHERE epersongroup2unit.unit_legacy_id = Unit.unit_id);
ALTER TABLE epersongroup2unit ALTER COLUMN eperson_group_id SET NOT NULL;
ALTER TABLE epersongroup2unit ALTER COLUMN unit_id SET NOT NULL;
ALTER TABLE epersongroup2unit DROP COLUMN eperson_group_legacy_id;
ALTER TABLE epersongroup2unit DROP COLUMN unit_legacy_id;
ALTER TABLE epersongroup2unit DROP COLUMN id;
ALTER TABLE epersongroup2unit add primary key (eperson_group_id,unit_id);
CREATE INDEX epersongroup2unit_epersongroup on epersongroup2unit(eperson_group_id);
CREATE INDEX epersongroup2unit_unit on epersongroup2unit(unit_id);

-- Drop etdunit and unit sequences
DROP SEQUENCE IF EXISTS etdunit_seq;
DROP SEQUENCE IF EXISTS collection2etdunit_seq;
DROP SEQUENCE IF EXISTS unit_seq;
DROP SEQUENCE IF EXISTS epersongroup2unit_seq;

-- Migrate Collection2WorkspaceItem
ALTER TABLE collection2workspaceitem ALTER COLUMN collection_id RENAME to collection_legacy_id;
-- ALTER TABLE collection2workspaceitem ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
ALTER TABLE collection2workspaceitem ADD COLUMN collection_id UUID;
ALTER TABLE collection2workspaceitem ADD FOREIGN KEY (collection_id) REFERENCES Collection(uuid);
--
UPDATE collection2workspaceitem SET collection_id = (SELECT Collection.uuid FROM Collection WHERE collection2workspaceitem.collection_legacy_id = Collection.collection_id);
ALTER TABLE collection2workspaceitem DROP COLUMN collection_legacy_id;
ALTER TABLE collection2workspaceitem DROP COLUMN id;
ALTER TABLE collection2workspaceitem ALTER COLUMN workspace_item_id SET NOT NULL;
ALTER TABLE collection2workspaceitem ALTER COLUMN collection_id SET NOT NULL;
ALTER TABLE collection2workspaceitem add primary key (workspace_item_id,collection_id);
CREATE INDEX collection2workspaceitem_group on collection2workspaceitem(collection_id);

-- Migrate Collection2WorkflowItem
ALTER TABLE collection2workflowitem ALTER COLUMN collection_id RENAME to collection_legacy_id;
-- ALTER TABLE collection2workflowitem ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
ALTER TABLE collection2workflowitem ADD COLUMN collection_id UUID;
ALTER TABLE collection2workflowitem ADD FOREIGN KEY (collection_id) REFERENCES Collection(uuid);
--
UPDATE collection2workflowitem SET collection_id = (SELECT Collection.uuid FROM Collection WHERE collection2workflowitem.collection_legacy_id = Collection.collection_id);
ALTER TABLE collection2workflowitem DROP COLUMN collection_legacy_id;
ALTER TABLE collection2workflowitem DROP COLUMN id;
ALTER TABLE collection2workflowitem ALTER COLUMN workflow_id SET NOT NULL;
ALTER TABLE collection2workflowitem ALTER COLUMN collection_id SET NOT NULL;
ALTER TABLE collection2workflowitem add primary key (workflow_id,collection_id);
CREATE INDEX collection2workflowitem_group on collection2workflowitem(collection_id);


-- Remove view columns no longer used by
ALTER TABLE item DROP COLUMN views;
ALTER TABLE bitstream DROP COLUMN views;