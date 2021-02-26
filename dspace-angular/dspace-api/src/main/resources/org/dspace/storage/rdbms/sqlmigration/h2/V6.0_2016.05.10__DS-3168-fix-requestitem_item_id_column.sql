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
ALTER TABLE requestitem ALTER COLUMN item_id rename to item_legacy_id;
ALTER TABLE requestitem ADD COLUMN item_id UUID;
ALTER TABLE requestitem ADD CONSTRAINT requestitem_item_id_fk FOREIGN KEY (item_id) REFERENCES Item;
UPDATE requestitem SET item_id = (SELECT item.uuid FROM item WHERE requestitem.item_legacy_id = item.item_id);
ALTER TABLE requestitem DROP COLUMN item_legacy_id;

ALTER TABLE requestitem ALTER COLUMN bitstream_id rename to bitstream_legacy_id;
ALTER TABLE requestitem ADD COLUMN bitstream_id UUID;
ALTER TABLE requestitem ADD CONSTRAINT requestitem_id_fk FOREIGN KEY (bitstream_id) REFERENCES Bitstream;
UPDATE requestitem SET bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE requestitem.bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE requestitem DROP COLUMN bitstream_legacy_id;