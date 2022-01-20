--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

----------------------------------------------------
-- Add a sorting number to the item table so the item findAll returns items in the same order
----------------------------------------------------
CREATE SEQUENCE item_sorting_number_seq;
AlTER TABLE item ADD sorting_number INTEGER DEFAULT nextval('item_sorting_number_seq');
