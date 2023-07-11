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
