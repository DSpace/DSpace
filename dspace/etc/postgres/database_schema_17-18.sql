
--------------------------------------------------------------------------------------------------------------
-- FUNCTIONS
--------------------------------------------------------------------------------------------------------------

--
-- Query to request all item id values for the data files associated with all archived data packages for a
-- given journal.
-- $1: full journal name
-- eg: EXECUTE ArchivedPackageDataFileItemIdsByJournal('Evolution');
--
CREATE OR REPLACE FUNCTION ArchivedPackageDataFileItemIdsByJournal (text) RETURNS setof int AS $$
SELECT DISTINCT mdv_df.item_id
   FROM metadatavalue mdv_df
   JOIN metadatafieldregistry  mdfr_df ON mdv_df.metadata_field_id   = mdfr_df.metadata_field_id
   JOIN metadataschemaregistry mdsr_df ON mdsr_df.metadata_schema_id = mdfr_df.metadata_schema_id
  WHERE mdsr_df.short_id  = 'dc'
    AND mdfr_df.element   = 'relation'
    AND mdfr_df.qualifier = 'ispartof'
    AND mdv_df.text_value IN
    -- doi for data packages for provided journal
   (SELECT mdv_p_doi.text_value
      FROM  metadatavalue mdv_p_doi
      JOIN  metadatafieldregistry mdfr_p_doi  ON mdv_p_doi.metadata_field_id   = mdfr_p_doi.metadata_field_id
      JOIN  metadataschemaregistry mdsr_p_doi ON mdfr_p_doi.metadata_schema_id = mdsr_p_doi.metadata_schema_id
     WHERE  mdsr_p_doi.short_id  = 'dc'
       AND  mdfr_p_doi.element   = 'identifier'
       AND  mdfr_p_doi.qualifier IS NULL
       AND  mdv_p_doi.item_id IN
     -- item_id for data packages for provided journal
     (SELECT mdv_p_pub.item_id
          FROM  metadatavalue mdv_p_pub
          JOIN  metadatafieldregistry mdfr_p_pub  ON mdv_p_pub.metadata_field_id   = mdfr_p_pub.metadata_field_id
          JOIN  metadataschemaregistry mdsr_p_pub ON mdfr_p_pub.metadata_schema_id = mdsr_p_pub.metadata_schema_id
          JOIN  item item_p ON mdv_p_pub.item_id=item_p.item_id
         WHERE  mdsr_p_pub.short_id  ='prism'
           AND  mdfr_p_pub.element   ='publicationName'
           AND  mdv_p_pub.text_value = $1                                                     -- $1: journal name
           AND  item_p.in_archive    = true
    ));
$$ LANGUAGE sql;

--
-- Query to return a list of item ids for archived data packages for a given journal.
-- $1: journal name; $2: limit
-- eg: EXECUTE ArchivedPackageDataFileItemIdsByJournal('Evolution');
--
CREATE OR REPLACE FUNCTION ArchivedPackageItemIdsByJournal (text,int) RETURNS setof int AS $$
SELECT item_p.item_id
 FROM item item_p
 JOIN metadatavalue          mdv_pub   ON item_p.item_id               = mdv_pub.item_id
 JOIN metadatafieldregistry  mdfr_pub  ON mdv_pub.metadata_field_id    = mdfr_pub.metadata_field_id
 JOIN metadataschemaregistry mdsr_pub  ON mdfr_pub.metadata_schema_id  = mdsr_pub.metadata_schema_id
 JOIN metadatavalue          mdv_date  ON item_p.item_id               = mdv_date.item_id
 JOIN metadatafieldregistry  mdfr_date ON mdv_date.metadata_field_id   = mdfr_date.metadata_field_id
 JOIN metadataschemaregistry mdsr_date ON mdfr_date.metadata_schema_id = mdsr_date.metadata_schema_id
WHERE item_p.in_archive   = true
  AND mdsr_pub.short_id   = 'prism'
  AND mdfr_pub.element    = 'publicationName'
  AND mdv_pub.text_value  = $1                                                        -- journal name
  AND mdsr_date.short_id  = 'dc'
  AND mdfr_date.element   = 'date'
  AND mdfr_date.qualifier = 'accessioned'
ORDER BY mdv_date.text_value DESC
LIMIT $2                                                                             -- limit
$$ LANGUAGE sql;

--
-- Query to return a count of archived data packages for a given journal.
-- $1: journal name
--
CREATE OR REPLACE FUNCTION ArchivedPackageCountByJournal (text) RETURNS bigint AS $$
SELECT COUNT(item_p) AS total
 FROM item item_p
 JOIN metadatavalue          mdv_pub   ON item_p.item_id               = mdv_pub.item_id
 JOIN metadatafieldregistry  mdfr_pub  ON mdv_pub.metadata_field_id    = mdfr_pub.metadata_field_id
 JOIN metadataschemaregistry mdsr_pub  ON mdfr_pub.metadata_schema_id  = mdsr_pub.metadata_schema_id
WHERE item_p.in_archive   = true
  AND mdsr_pub.short_id   = 'prism'
  AND mdfr_pub.element    = 'publicationName'
  AND mdv_pub.text_value  = $1                                                      -- journal name
$$ LANGUAGE sql;

