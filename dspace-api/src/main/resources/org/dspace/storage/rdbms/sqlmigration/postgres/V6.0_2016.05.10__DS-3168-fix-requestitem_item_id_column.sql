--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

---------------------------------------------------------------
-- DS-3168 Embargo request Unknown Entity RequestItem
---------------------------------------------------------------
-- convert the item_id and bitstream_id columns from integer to UUID
---------------------------------------------------------------
ALTER TABLE requestitem RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE requestitem ADD COLUMN item_id UUID REFERENCES Item(uuid);
UPDATE requestitem SET item_id = (SELECT item.uuid FROM item WHERE requestitem.item_legacy_id = item.item_id);
ALTER TABLE requestitem DROP COLUMN item_legacy_id;
CREATE INDEX requestitem_item on requestitem(item_id);

ALTER TABLE requestitem RENAME COLUMN bitstream_id to bitstream_legacy_id;
ALTER TABLE requestitem ADD COLUMN bitstream_id UUID REFERENCES Bitstream(uuid);
UPDATE requestitem SET bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE requestitem.bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE requestitem DROP COLUMN bitstream_legacy_id;
CREATE INDEX requestitem_bitstream on requestitem(bitstream_id);