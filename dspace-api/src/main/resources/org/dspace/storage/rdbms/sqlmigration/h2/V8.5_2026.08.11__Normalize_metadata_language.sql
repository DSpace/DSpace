--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- Normalize metadatavalue.text_lang = '*' (Item.ANY) to NULL.
--
-- Since #9714 (7.6.3, 8.1, 9.0) MetadataValue.setLanguage() normalizes '*' -> NULL on write, but
-- nothing repaired the rows that already existed. They break the metadata-export -> metadata-import
-- round trip, and any crosswalk that copies the language into an xml:lang attribute emits an
-- invalid '*'. Only '*' is normalized; an empty-string language is legal and round-trips correctly.

-- Step 1 must run before step 2, which removes the '*' it selects on, and is there for OAI:
-- `dspace oai import` only revisits items newer than its own watermark. Discovery covers itself
-- via discovery.autoReindex, and via this bump where that is off. Side effect: the touched items
-- appear in the first content-subscription digest after the upgrade.
UPDATE item SET last_modified = CURRENT_TIMESTAMP
 WHERE uuid IN (SELECT dspace_object_id FROM metadatavalue WHERE text_lang = '*');

-- Step 2 is deliberately not restricted to items: collections, communities and bitstreams are
-- normalized too. They have no last_modified to bump.
UPDATE metadatavalue SET text_lang = NULL WHERE text_lang = '*';
