ALTER TABLE item ADD UNIQUE (item_id);
alter table item drop column discoverable;
alter table resourcepolicy drop column rpname;
alter table resourcepolicy drop column rptype;
alter table resourcepolicy drop column rpdescription;
alter table eperson drop column salt;
alter table eperson drop column digest_algorithm;
DROP VIEW community2collection2struttura ;
DROP VIEW workflow_onvalidation_item  ;
DROP VIEW workflow_owned_item  ;
DROP VIEW workflow_pooled_item  ;
--DROP TABLE versionhistory CASCADE;

INSERT INTO epersongroup (eperson_group_id) values ( nextval('epersongroup_seq') ); 
INSERT INTO metadatavalue (metadata_value_id,resource_id,metadata_field_id,text_value,text_lang,place,authority,confidence,resource_type_id) (SELECT nextval('metadatavalue_seq'),MAX(eperson_group_id),66,'EMBARGOGROUPS',null,1,null,-1,6 from epersongroup);
INSERT INTO group2group (id,parent_id,child_id) (SELECT nextval('group2group_seq'),currval('epersongroup_seq'),resource_id from metadatavalue WHERE resource_type_id=6 AND metadata_field_id=66 AND  text_value like 'Administrator' );
INSERT INTO group2group (id,parent_id,child_id) (SELECT nextval('group2group_seq'),currval('epersongroup_seq'),resource_id from metadatavalue WHERE resource_type_id=6 AND metadata_field_id=66 AND  text_value like 'INGV Users' );
INSERT INTO group2groupcache (id,parent_id,child_id) (SELECT nextval('group2groupcache_seq'),currval('epersongroup_seq'),resource_id from metadatavalue WHERE resource_type_id=6 AND metadata_field_id=66 AND  text_value like 'Administrator' );
INSERT INTO group2groupcache (id,parent_id,child_id) (SELECT nextval('group2groupcache_seq'),currval('epersongroup_seq'),resource_id from metadatavalue WHERE resource_type_id=6 AND metadata_field_id=66 AND  text_value like 'INGV Users' );
UPDATE resourcepolicy set epersongroup_id = (select resource_id from metadatavalue where resource_type_id=6 and text_value like 'INGV Users') where start_date ='2101-01-01';
UPDATE resourcepolicy set epersongroup_id = 1 where start_date ='2100-01-01';
-- IMPORT ORG UNIT : Prima parent poi suborgunit

-- IMPORT CLASSIFICAZIONE
-- DOPO AVER CARICATO LA CLASSIFICAZIONE
-- VERIFICARE I CRISID
-- VERIFICARE I CRISID
-- VERIFICARE I CRISID
update metadatavalue set authority = 'classification00006',confidence=600 where metadata_field_id=71 and text_value like '01. Atmosphere::01.01. Atmosphere::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00007',confidence=600 where metadata_field_id=71 and text_value like '01. Atmosphere::01.02. Ionosphere::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00008',confidence=600 where metadata_field_id=71 and text_value like '01. Atmosphere::01.03. Magnetosphere::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00009',confidence=600 where metadata_field_id=71 and text_value like '02. Cryosphere::02.01. Permafrost::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00010',confidence=600 where metadata_field_id=71 and text_value like '02. Cryosphere::02.02. Glaciers::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00011',confidence=600 where metadata_field_id=71 and text_value like '02. Cryosphere::02.03. Ice cores::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00012',confidence=600 where metadata_field_id=71 and text_value like '02. Cryosphere::02.04. Sea ice::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00013',confidence=600 where metadata_field_id=71 and text_value like '03. Hydrosphere::03.01. General::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00014',confidence=600 where metadata_field_id=71 and text_value like '03. Hydrosphere::03.02. Hydrology::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00015',confidence=600 where metadata_field_id=71 and text_value like '03. Hydrosphere::03.03. Physical::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00016',confidence=600 where metadata_field_id=71 and text_value like '03. Hydrosphere::03.04. Chemical and biological::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00017',confidence=600 where metadata_field_id=71 and text_value like '04. Solid Earth::04.01. Earth Interior::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00018',confidence=600 where metadata_field_id=71 and text_value like '04. Solid Earth::04.02. Exploration geophysics::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00019',confidence=600 where metadata_field_id=71 and text_value like '04. Solid Earth::04.03. Geodesy::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00020',confidence=600 where metadata_field_id=71 and text_value like '04. Solid Earth::04.04. Geology::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00021',confidence=600 where metadata_field_id=71 and text_value like '04. Solid Earth::04.05. Geomagnetism::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00022',confidence=600 where metadata_field_id=71 and text_value like '04. Solid Earth::04.06. Seismology::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00023',confidence=600 where metadata_field_id=71 and text_value like '04. Solid Earth::04.07. Tectonophysics::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00024',confidence=600 where metadata_field_id=71 and text_value like '04. Solid Earth::04.08. Volcanology::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00025',confidence=600 where metadata_field_id=71 and text_value like '05. General::05.01. Computational geophysics::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00026',confidence=600 where metadata_field_id=71 and text_value like '05. General::05.02. Data dissemination::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00027',confidence=600 where metadata_field_id=71 and text_value like '05. General::05.03. Educational, History of Science, Public Issues::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00028',confidence=600 where metadata_field_id=71 and text_value like '05. General::05.04. Instrumentation and techniques of general interest::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00029',confidence=600 where metadata_field_id=71 and text_value like '05. General::05.05. Mathematical geophysics::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00030',confidence=600 where metadata_field_id=71 and text_value like '05. General::05.06. Methods::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00031',confidence=600 where metadata_field_id=71 and text_value like '05. General::05.07. Space and Planetary sciences::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00032',confidence=600 where metadata_field_id=71 and text_value like '05. General::05.08. Risk::%' and resource_type_id=2;
update metadatavalue set authority = 'classification00033',confidence=600 where metadata_field_id=71 and text_value like '05. General::05.09. Miscellaneous::%' and resource_type_id=2;

-- IMPORT RIVISTE
--- DOPO AVER CARICATO LE RIVISTE update authority
UPDATE metadatavalue m set m.authority = 
(select do.crisid from cris_do do join journals j on j.source_id =do.sourceid and do.sourceref like j.source 
where cast( m.authority as int)=j.id  and m.metadata_field_id=42 and m.authority is not null and m.resource_type_id=2 and do.crisid like 'journals%');

----AUTORI:
INSERT INTO metadatafieldregistry (metadata_field_id,metadata_schema_id,element,qualifier,scope_note) values ( nextval('metadatafieldregistry_seq'),1,'contributor','authorall','');
INSERT INTO metadatafieldregistry (metadata_field_id,metadata_schema_id,element,qualifier,scope_note) values ( nextval('metadatafieldregistry_seq'),1,'contributor','department','');
-- VERIFICARE field_id di contributor.authorall
INSERT INTO metadatavalue (metadata_value_id,item_id,metadata_field_id,text_value,text_lang,place,authority,confidence,share_value,internalorder) (
select nextval('metadatavalue_seq'),item_id,86,left(text_value,strpos(text_value,';') -1 ),text_lang,place,authority,confidence,share_value,internalorder from metadatavalue  where metadata_field_id  =3 AND text_value LIKE '%;%');
INSERT INTO metadatavalue (metadata_value_id,item_id,metadata_field_id,text_value,text_lang,place,authority,confidence,share_value,internalorder) (
select nextval('metadatavalue_seq'),item_id,86,text_value,text_lang,place,authority,confidence,share_value,internalorder from metadatavalue  where metadata_field_id  =3 AND text_value NOT LIKE '%;%');
---AUTORI AFFILIATION
INSERT INTO metadatavalue (metadata_value_id,item_id,metadata_field_id,text_value,text_lang,place,authority,confidence,share_value,internalorder) (
select nextval('metadatavalue_seq'),item_id,87,substring( text_value from strpos(text_value,'; ') +2 ),text_lang,place,authority,confidence,share_value,internalorder from metadatavalue  where metadata_field_id  =3 AND text_value LIKE '%; %');
-- SPOSTARE contributor.author in contribur.authorall ed authorall in author
UPDATE metadatafieldregistry set qualifier = authortmp where metadata_field_id=3;
UPDATE metadatafieldregistry set qualifier = author where metadata_field_id=86;
UPDATE metadatafieldregistry set qualifier = authorall where metadata_field_id=3;

----CURATORI:
INSERT INTO metadatafieldregistry (metadata_field_id,metadata_schema_id,element,qualifier,scope_note) values ( nextval('metadatafieldregistry_seq'),1,'contributor','editorall','');
INSERT INTO metadatafieldregistry (metadata_field_id,metadata_schema_id,element,qualifier,scope_note) values ( nextval('metadatafieldregistry_seq'),1,'contributor','editordepartment','');
-- VERIFICARE field_id di contributor.editorall
INSERT INTO metadatavalue (metadata_value_id,item_id,metadata_field_id,text_value,text_lang,place,authority,confidence,share_value,internalorder) (
select nextval('metadatavalue_seq'),item_id,88,left(text_value,strpos(text_value,';') -1 ),text_lang,place,authority,confidence,share_value,internalorder from metadatavalue  where metadata_field_id  =4 AND text_value LIKE '%;%');
INSERT INTO metadatavalue (metadata_value_id,item_id,metadata_field_id,text_value,text_lang,place,authority,confidence,share_value,internalorder) (
select nextval('metadatavalue_seq'),item_id,88,text_value,text_lang,place,authority,confidence,share_value,internalorder from metadatavalue  where metadata_field_id  =4 AND text_value NOT LIKE '%;%');
---CURATORI AFFILIATION
INSERT INTO metadatavalue (metadata_value_id,item_id,metadata_field_id,text_value,text_lang,place,authority,confidence,share_value,internalorder) (
select nextval('metadatavalue_seq'),item_id,89,substring( text_value from strpos(text_value,'; ') +2 ),text_lang,place,authority,confidence,share_value,internalorder from metadatavalue  where metadata_field_id  =4 AND text_value LIKE '%; %');
-- SPOSTARE contributor.editor in contribur.editorall ed editorall in editor
UPDATE metadatafieldregistry set qualifier = editortmp where metadata_field_id=4;
UPDATE metadatafieldregistry set qualifier = editor where metadata_field_id=88;
UPDATE metadatafieldregistry set qualifier = editorall where metadata_field_id=4;

