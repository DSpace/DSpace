------------------------------------------------------
-- LIBDRUM-511 - Upgrade to Service based API / Hibernate integration
------------------------------------------------------
-- Migrate etdunit
ALTER TABLE etdunit ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM etdunit;
ALTER TABLE etdunit ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE etdunit ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE etdunit ADD CONSTRAINT etdunit_id_unique UNIQUE(uuid);
ALTER TABLE etdunit ADD PRIMARY KEY (uuid);
ALTER TABLE etdunit ALTER COLUMN etdunit_id DROP NOT NULL;
CREATE INDEX etdunit_id_idx on etdunit(etdunit_id);

-- Migrate unit
ALTER TABLE unit ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
INSERT INTO dspaceobject (uuid) SELECT uuid FROM unit;
ALTER TABLE unit ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE unit ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE unit ADD CONSTRAINT unit_id_unique UNIQUE(uuid);
ALTER TABLE unit ADD PRIMARY KEY (uuid);
ALTER TABLE unit ALTER COLUMN unit_id DROP NOT NULL;
CREATE INDEX unit_id_idx on unit(unit_id);

-- Migrate collection2etdunit
ALTER TABLE collection2etdunit RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE collection2etdunit RENAME COLUMN etdunit_id to etdunit_legacy_id;
ALTER TABLE collection2etdunit ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
ALTER TABLE collection2etdunit ADD COLUMN etdunit_id UUID REFERENCES EtdUnit(uuid);
UPDATE collection2etdunit SET collection_id = Collection.uuid FROM Collection WHERE collection2etdunit.collection_legacy_id = Collection.collection_id;
UPDATE collection2etdunit SET etdunit_id = EtdUnit.uuid FROM EtdUnit WHERE collection2etdunit.etdunit_legacy_id = EtdUnit.etdunit_id;
ALTER TABLE collection2etdunit ALTER COLUMN collection_id SET NOT NULL;
ALTER TABLE collection2etdunit ALTER COLUMN etdunit_id SET NOT NULL;
ALTER TABLE collection2etdunit DROP COLUMN collection_legacy_id;
ALTER TABLE collection2etdunit DROP COLUMN etdunit_legacy_id;
ALTER TABLE collection2etdunit DROP COLUMN id;
ALTER TABLE collection2etdunit add primary key (collection_id,etdunit_id);
CREATE INDEX collection2etdunit_collection on collection2etdunit(collection_id);
CREATE INDEX collection2etdunit_etdunit on collection2etdunit(etdunit_id);

-- Migrate epersongroup2unit
ALTER TABLE epersongroup2unit RENAME COLUMN eperson_group_id to eperson_group_legacy_id;
ALTER TABLE epersongroup2unit RENAME COLUMN unit_id to unit_legacy_id;
ALTER TABLE epersongroup2unit ADD COLUMN eperson_group_id UUID REFERENCES epersongroup(uuid);
ALTER TABLE epersongroup2unit ADD COLUMN unit_id UUID REFERENCES EtdUnit(uuid);
UPDATE epersongroup2unit SET eperson_group_id = epersongroup.uuid FROM epersongroup WHERE epersongroup2unit.eperson_group_legacy_id = epersongroup.eperson_group_id;
UPDATE epersongroup2unit SET unit_id = EtdUnit.uuid FROM EtdUnit WHERE epersongroup2unit.unit_legacy_id = EtdUnit.unit_id;
ALTER TABLE epersongroup2unit ALTER COLUMN eperson_group_id SET NOT NULL;
ALTER TABLE epersongroup2unit ALTER COLUMN unit_id SET NOT NULL;
ALTER TABLE epersongroup2unit DROP COLUMN eperson_group_legacy_id;
ALTER TABLE epersongroup2unit DROP COLUMN unit_legacy_id;
ALTER TABLE epersongroup2unit DROP COLUMN id;
ALTER TABLE epersongroup2unit add primary key (eperson_group_id,unit_id);
CREATE INDEX epersongroup2unit_epersongroup on epersongroup2unit(eperson_group_id);
CREATE INDEX epersongroup2unit_unit on epersongroup2unit(unit_id);

-- Drop etdunit sequences
DROP SEQUENCE IF EXISTS etdunit_seq;
DROP SEQUENCE IF EXISTS collection2etdunit_seq;
DROP SEQUENCE IF EXISTS unit_seq;
DROP SEQUENCE IF EXISTS epersongroup2unit_seq;