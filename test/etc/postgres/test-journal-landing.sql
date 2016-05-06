
CREATE OR REPLACE FUNCTION getMetadataFieldRegistryId (text,text,text) RETURNS int AS $$
SELECT mdfr.metadata_field_id
  FROM metadatafieldregistry mdfr JOIN metadataschemaregistry mdsr
    ON mdsr.metadata_schema_id=mdfr.metadata_schema_id
 WHERE mdsr.short_id=$1                     -- 'dc'
   AND mdfr.element=$2                      -- 'relation'
   AND (($3 IS NULL AND mdfr.qualifier IS NULL)
        OR (mdfr.qualifier=$3))             -- 'ispartof'
$$
LANGUAGE sql;

CREATE OR REPLACE FUNCTION journalLandingTestData() RETURNS void AS $$
DECLARE
    id1 INTEGER; id2 INTEGER; id3 INTEGER; id4 INTEGER;
    PPN INTEGER; DDA INTEGER; DCI INTEGER; DRP INTEGER; COLF INTEGER; COLP INTEGER;
    JIN INTEGER;
    conf  CONSTANT INTEGER := -1;
    pl    CONSTANT INTEGER :=  1;
BEGIN

    SELECT (COALESCE(MAX(item_id), 0) + 1) INTO id1 FROM item;
    SELECT (COALESCE(MAX(item_id), 0) + 2) INTO id2 FROM item;
    SELECT (COALESCE(MAX(item_id), 0) + 3) INTO id3 FROM item;
    SELECT (COALESCE(MAX(id),      0) + 1) INTO id4 FROM concept;

    SELECT getMetadataFieldRegistryId('prism'   , 'publicationName' , NULL          ) INTO PPN;
    SELECT getMetadataFieldRegistryId('dc'      , 'date'            , 'accessioned' ) INTO DDA;
    SELECT getMetadataFieldRegistryId('dc'      , 'identifier'      , NULL          ) INTO DCI;
    SELECT getMetadataFieldRegistryId('dc'      , 'relation'        , 'ispartof'    ) INTO DRP;
    SELECT getMetadataFieldRegistryId('journal' , 'issn'            , NULL          ) INTO JIN;

    SELECT collection_id INTO COLF FROM collection WHERE name='Dryad Data Files';
    SELECT collection_id INTO COLP FROM collection WHERE name='Dryad Data Packages';

    -- data packages
    INSERT INTO item (item_id, submitter_id, in_archive, withdrawn, last_modified, owning_collection)
        VALUES(id1,NULL,true,false,'2015-02-24 19:32:24.958+00',COLP);
    INSERT INTO item (item_id, submitter_id, in_archive, withdrawn, last_modified, owning_collection)
        VALUES(id2,NULL,true,false,'2015-02-25 19:32:24.958+00',COLP);

    -- data files
    INSERT INTO item (item_id, submitter_id, in_archive, withdrawn, last_modified, owning_collection)
        VALUES(id3,NULL,true,false,'2015-02-26 19:32:28.561+00',COLF);

    -- concept metadata values
    INSERT INTO concept(id, identifier, created, modified, status, topconcept)
        VALUES (id4, '4718f3c5c9fd4774a98a0f12c6430a38', '2015-03-10', '2015-03-10', 'ACCEPTED', true);
    INSERT INTO conceptmetadatavalue(parent_id, field_id, text_value)
        VALUES (id4, JIN, '1111-1111');
    INSERT INTO term(identifier,literalform)
        VALUES ('4718f3c5c9fd4774a98a0f12c6430a38', 'Evolution');
    INSERT INTO concept2term(concept_id,term_id,role_id)
        VALUES ((SELECT id FROM term             WHERE IDENTIFIER='4718f3c5c9fd4774a98a0f12c6430a38')
              , (SELECT id FROM concept          WHERE IDENTIFIER='4718f3c5c9fd4774a98a0f12c6430a38')
              , (SELECT id FROM concept2termrole WHERE ROLE      ='prefLabel'                       ));

    -- metadata values
    INSERT INTO metadatavalue (item_id, metadata_field_id, text_value, text_lang, place, authority, confidence)
        VALUES(id1, PPN, 'Evolution'              , NULL, pl, NULL, conf);
    INSERT INTO metadatavalue (item_id, metadata_field_id, text_value, text_lang, place, authority, confidence)
        VALUES(id1, DDA, '2015-02-24T19:32:20Z'   , NULL, pl, NULL, conf);
    INSERT INTO metadatavalue (item_id, metadata_field_id, text_value, text_lang, place, authority, confidence)
        VALUES(id1, DCI, 'doi:10.5061/dryad.aaaa', NULL, pl, NULL, conf);

    INSERT INTO metadatavalue (item_id, metadata_field_id, text_value, text_lang, place, authority, confidence)
        VALUES(id2, PPN, 'Evolution'              , NULL, pl, NULL, conf);
    INSERT INTO metadatavalue (item_id, metadata_field_id, text_value, text_lang, place, authority, confidence)
        VALUES(id2, DDA, '2015-02-25T19:32:20Z'   , NULL, pl, NULL, conf);
    INSERT INTO metadatavalue (item_id, metadata_field_id, text_value, text_lang, place, authority, confidence)
        VALUES(id2, DCI, 'doi:10.5061/dryad.bbbb', NULL, pl, NULL, conf);

    INSERT INTO metadatavalue (item_id, metadata_field_id, text_value, text_lang, place, authority, confidence)
        VALUES(id3, DCI, 'doi:10.5061/dryad.aaaa/1', NULL, pl, NULL, conf);
    INSERT INTO metadatavalue (item_id, metadata_field_id, text_value, text_lang, place, authority, confidence)
        VALUES(id3, DRP, 'doi:10.5061/dryad.aaaa', NULL, pl, NULL, conf);

END;
$$ LANGUAGE 'plpgsql';

SELECT * FROM journalLandingTestData();

DROP FUNCTION getMetadataFieldRegistryId(text,text,text);
DROP FUNCTION journalLandingTestData();
