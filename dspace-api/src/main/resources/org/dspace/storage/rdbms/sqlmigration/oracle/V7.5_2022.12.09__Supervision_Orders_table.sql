--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------------------------------
-- Table to store supervision orders
-------------------------------------------------------------------------------

CREATE TABLE supervision_orders
(
  id INTEGER PRIMARY KEY,
  item_id UUID REFERENCES Item(uuid) ON DELETE CASCADE,
  eperson_group_id UUID REFERENCES epersongroup(uuid) ON DELETE CASCADE
);

CREATE SEQUENCE supervision_orders_seq;

INSERT INTO supervision_orders (id, item_id, eperson_group_id)
SELECT supervision_orders_seq.nextval AS id, w.item_id, e.uuid
FROM epersongroup2workspaceitem ew INNER JOIN workspaceitem w
ON ew.workspace_item_id = w.workspace_item_id
INNER JOIN epersongroup e
ON ew.eperson_group_id = e.uuid;


-- UPDATE policies for supervision orders
-- items, bundles and bitstreams

DECLARE
BEGIN

FOR rec IN
(
SELECT so.item_id as dspace_object, so.eperson_group_id, rp.resource_type_id
FROM supervision_orders so
INNER JOIN RESOURCEPOLICY rp on so.item_id = rp.dspace_object
AND so.eperson_group_id = rp.epersongroup_id
WHERE rp.rptype IS NULL

UNION

SELECT ib.bundle_id as dspace_object, so.eperson_group_id, rp.resource_type_id
FROM supervision_orders so
INNER JOIN item2bundle ib ON so.item_id = ib.item_id
INNER JOIN RESOURCEPOLICY rp on ib.bundle_id = rp.dspace_object
AND so.eperson_group_id = rp.epersongroup_id
WHERE rp.rptype IS NULL

UNION

SELECT bs.bitstream_id as dspace_object, so.eperson_group_id, rp.resource_type_id
FROM supervision_orders so
INNER JOIN item2bundle ib ON so.item_id = ib.item_id
INNER JOIN bundle2bitstream bs ON ib.bundle_id = bs.bundle_id
INNER JOIN RESOURCEPOLICY rp on bs.bitstream_id = rp.dspace_object
AND so.eperson_group_id = rp.epersongroup_id
WHERE rp.rptype IS NULL
)

LOOP

UPDATE RESOURCEPOLICY SET rptype = 'TYPE_SUBMISSION'
where dspace_object = rec.dspace_object
AND epersongroup_id = rec.eperson_group_id
AND rptype IS NULL;

END LOOP;
END;

-------------------------------------------------------------------------------
-- drop epersongroup2workspaceitem table
-------------------------------------------------------------------------------

DROP TABLE epersongroup2workspaceitem;