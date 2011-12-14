  
  -- Esquemas adicionales (uso los IDs explicitos porque necesito referenciarlos ne la creacion de los metadatos)
INSERT INTO metadataschemaregistry VALUES (2, 'http://www.loc.gov/mods/v3', 'mods');
INSERT INTO metadataschemaregistry VALUES (3, 'http://path.to.eprints.namespace', 'eprints');
INSERT INTO metadataschemaregistry VALUES (4, 'http://path.to.etd.namespace', 'thesis');
  
  -- Nuevos metadatos  
  -- INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note);
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'contributor','director','Director de una Tesis');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'contributor','codirector','Co-Director de una Tesis');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'identifier','doi','Identificar DOI');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'identifier','purl','Identificador PURL');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'identifier','patent','Identificador de Patente');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'author','person','Autor de un documento (persona seleccionada desde la base de datos)');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'author','corporate','Autor Institucional (Institucion seleccionada desde la base de datos)');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'title','subtitle','Subtítulo principal de la obra');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'description','fulltext','Indica si se posee el texto completo del recurso');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'subject','keywords','Palabras claves, extraidas del texto');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'date','exposure','Fecha de exposicion de una Tesis o un Objeto de Conferencia');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'source','title','Nombre de un Evento (congreso, conferencia, etc)');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'contributor','juror','Jurado de una Tesis');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'contributor','translator','Traductor de un libro');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'contributor','compiler','Compilador de un libro');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'contributor','colaborator','Colaborador en la creación de un libro');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),1,'description','note','Nota o comentario');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),2,'location',null,'Localización Física del recurso: URL de acceso a la biblioteca o información acerca de su ubicación física');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),2,'originInfo','place','Entidad de origen');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),3,'status',null,'Evaluación realizada sobre el artículo');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),3,'AffilliatedInstitution',null,'Lugar de desarrollo');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),4,'degree','name','Grado alcanzado por el autor de una tesis');
INSERT INTO metadatafieldregistry VALUES (getnextid('metadatafieldregistry'),4,'degree','grantor','Institucion responsable de avalar la tesis');

-- Actualización de las secuencias
SELECT setval('metadataschemaregistry_seq', max(metadata_schema_id)) FROM metadataschemaregistry;
SELECT setval('metadatafieldregistry_seq', max(metadata_field_id)) FROM metadatafieldregistry;