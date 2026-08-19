--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- Normalize metadatavalue.text_lang = '*' (Item.ANY) to NULL.
--
-- Before #9714 and its backports (in 7.6.3, 8.1 and 9.0) the language argument was stored verbatim
-- on write, so any pre-fix caller that handed in Item.ANY left text_lang = '*' on whatever object it
-- was writing to: in tree, Collection.setLicense() and StructBuilder passed Item.ANY directly, and
-- metadata-import accepted '[*]' column headings. Since that fix MetadataValue.setLanguage()
-- normalizes '*' -> NULL on write and DSpaceCSV rejects '[*]' headings, but nothing ever repaired
-- the rows that already existed: the setter only runs when a row is written. Any database created
-- before those releases can still hold them.
--
-- Where such rows sit on items they break the metadata-export -> metadata-import round trip (the
-- export emits the '[*]' headings the import now rejects) and make OAI store <element name="*"/>,
-- from which oai_openaire emits an invalid xml:lang="*".
--
-- Only '*' is normalized. Empty-string languages are legal and round-trip correctly - leave them.

-- Step 1 must run before step 2: it uses the '*' marker to find the affected items, and it is
-- there for the OAI index. A successful migration flags Discovery for a full reindex (unless
-- discovery.autoReindex is false), but nothing does the same for OAI: `dspace oai import` takes
-- its watermark from the newest item.lastmodified already in its own Solr core, so without the
-- bump it never revisits these items. The bump also covers Discovery where autoReindex is off,
-- since `dspace index-discovery` reindexes documents whose search.lastindexed predates
-- last_modified. Note that content-subscription digests select on the same column
-- (CollectionUpdates / CommunityUpdates filter Solr lastModified), so the touched items are
-- listed as modified in the first digest sent after the upgrade.
UPDATE item SET last_modified = CURRENT_TIMESTAMP
 WHERE uuid IN (SELECT dspace_object_id FROM metadatavalue WHERE text_lang = '*');

-- Step 2 is deliberately not restricted to items. Rows on collections, communities and
-- bitstreams are normalized too and need no reindex: those tables have no last_modified column,
-- they are not in OAI or in metadata-export, and no indexing path reads the language of a
-- non-item DSO (SolrServiceMetadataBrowseIndexingPlugin bails on non-items, and the container
-- index factories never call getLanguage()).
UPDATE metadatavalue SET text_lang = NULL WHERE text_lang = '*';
