-- Database data for Dryad metadata schemas as of 2014-06-11
-- Captured statically for use in a test database, should not be used in production deployments
-- instead, load metadata schemas using MetadataImporter
--
-- This file was created from
-- pg_dump -t metadataschemaregistry --column-inserts --data-only
-- pg_dump -t metadatafieldregistry --column-inserts --data-only
-- 

--
-- Data for Name: metadataschemaregistry
--

INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (1, 'http://dublincore.org/documents/dcmi-terms/', 'dc');
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (2, 'http://rs.tdwg.org/dwc/dwcore/', 'dwc');
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (3, 'http://purl.org/dryad/terms/', 'dryad');
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (4, 'http://www.dspace.org/workflow', 'workflow');
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (5, 'http://www.dspace.org/system', 'internal');
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (6, 'http://prismstandard.org/namespaces/basic/2.0/', 'prism');

--
-- Data for Name: metadatafieldregistry
--

INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (1, 1, 'contributor', NULL, 'A person, organization, or service responsible for the content of the resource.  Catch-all for unspecified contributors.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (2, 1, 'contributor', 'advisor', 'Use primarily for thesis advisor.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (3, 1, 'contributor', 'author', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (4, 1, 'contributor', 'editor', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (5, 1, 'contributor', 'illustrator', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (6, 1, 'contributor', 'other', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (7, 1, 'coverage', 'spatial', 'Spatial characteristics of content.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (8, 1, 'coverage', 'temporal', 'Temporal characteristics of content.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (9, 1, 'creator', NULL, 'Do not use; only for harvested metadata.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (10, 1, 'date', NULL, 'Use qualified form if possible.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (11, 1, 'date', 'accessioned', 'Date DSpace takes possession of item.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (12, 1, 'date', 'available', 'Date or date range item became available to the public.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (13, 1, 'date', 'copyright', 'Date of copyright.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (14, 1, 'date', 'created', 'Date of creation or manufacture of intellectual content if different from date.issued.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (15, 1, 'date', 'issued', 'Date of publication or distribution.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (16, 1, 'date', 'submitted', 'Recommend for theses/dissertations.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (17, 1, 'identifier', NULL, 'Catch-all for unambiguous identifiers not defined by
    qualified form; use identifier.other for a known identifier common
    to a local collection instead of unqualified form.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (18, 1, 'identifier', 'citation', 'Human-readable, standard bibliographic citation 
    of non-DSpace format of this item');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (19, 1, 'identifier', 'govdoc', 'A government document number');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (20, 1, 'identifier', 'isbn', 'International Standard Book Number');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (21, 1, 'identifier', 'issn', 'International Standard Serial Number');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (22, 1, 'identifier', 'sici', 'Serial Item and Contribution Identifier');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (23, 1, 'identifier', 'ismn', 'International Standard Music Number');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (24, 1, 'identifier', 'other', 'A known identifier type common to a local collection.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (25, 1, 'identifier', 'uri', 'Uniform Resource Identifier');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (26, 1, 'description', NULL, 'Catch-all for any description not defined by qualifiers.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (27, 1, 'description', 'abstract', 'Abstract or summary.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (28, 1, 'description', 'provenance', 'The history of custody of the item since its creation, including any changes successive custodians made to it.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (29, 1, 'description', 'sponsorship', 'Information about sponsoring agencies, individuals, or
    contractual arrangements for the item.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (30, 1, 'description', 'statementofresponsibility', 'To preserve statement of responsibility from MARC records.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (31, 1, 'description', 'tableofcontents', 'A table of contents for a given item.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (32, 1, 'description', 'uri', 'Uniform Resource Identifier pointing to description of
    this item.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (33, 1, 'format', NULL, 'Catch-all for any format information not defined by qualifiers.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (34, 1, 'format', 'extent', 'Size or duration.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (35, 1, 'format', 'medium', 'Physical medium.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (36, 1, 'format', 'mimetype', 'Registered MIME type identifiers.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (37, 1, 'language', NULL, 'Catch-all for non-ISO forms of the language of the
    item, accommodating harvested values.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (38, 1, 'language', 'iso', 'Current ISO standard for language of intellectual content, including country codes (e.g. "en_US").');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (39, 1, 'publisher', NULL, 'Entity responsible for publication, distribution, or imprint.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (40, 1, 'relation', NULL, 'Catch-all for references to other related items.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (41, 1, 'relation', 'isformatof', 'References additional physical form.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (42, 1, 'relation', 'ispartof', 'References physically or logically containing item.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (43, 1, 'relation', 'ispartofseries', 'Series name and number within that series, if available.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (44, 1, 'relation', 'haspart', 'References physically or logically contained item.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (45, 1, 'relation', 'isversionof', 'References earlier version.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (46, 1, 'relation', 'hasversion', 'References later version.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (47, 1, 'relation', 'isbasedon', 'References source.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (48, 1, 'relation', 'isreferencedby', 'Pointed to by referenced resource.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (49, 1, 'relation', 'requires', 'Referenced resource is required to support function,
    delivery, or coherence of item.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (50, 1, 'relation', 'replaces', 'References preceeding item.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (51, 1, 'relation', 'isreplacedby', 'References succeeding item.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (52, 1, 'relation', 'uri', 'References Uniform Resource Identifier for related item.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (53, 1, 'rights', NULL, 'Terms governing use and reproduction.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (54, 1, 'rights', 'uri', 'References terms governing use and reproduction.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (55, 1, 'source', NULL, 'Do not use; only for harvested metadata.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (56, 1, 'source', 'uri', 'Do not use; only for harvested metadata.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (57, 1, 'subject', NULL, 'Uncontrolled index term.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (58, 1, 'subject', 'classification', 'Catch-all for value from local classification system;
    global classification systems will receive specific qualifier');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (59, 1, 'subject', 'ddc', 'Dewey Decimal Classification Number');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (60, 1, 'subject', 'lcc', 'Library of Congress Classification Number');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (61, 1, 'subject', 'lcsh', 'Library of Congress Subject Headings');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (62, 1, 'subject', 'mesh', 'MEdical Subject Headings');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (63, 1, 'subject', 'other', 'Local controlled vocabulary; global vocabularies will receive specific qualifier.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (64, 1, 'title', NULL, 'Title statement/title proper.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (65, 1, 'title', 'alternative', 'Varying (or substitute) form of title proper appearing in item,
    e.g. abbreviation or translation');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (66, 1, 'type', NULL, 'Nature or genre of content.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (67, 1, 'date', 'embargoedUntil', 'Embargo');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (68, 1, 'contributor', 'correspondingAuthor', 'Primary Contact');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (69, 2, 'ScientificName', NULL, 'Taxonomic names');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (70, 1, 'workflow', 'submitted', 'Boolean used in the submission process only.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (71, 1, 'workflow', 'selectjournal', 'Boolean used in the submission process only.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (72, 1, 'type', 'embargo', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (73, 1, 'submit', 'showEmbargo', 'Boolean used in the submission process only.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (74, 1, 'identifier', 'manuscriptNumber', 'Manuscript number');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (75, 1, 'identifier', 'externalresourceXml', 'External source xml value');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (76, 1, 'date', 'updated', 'The last time the item was updated via the SWORD interface');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (77, 1, 'description', 'version', 'The Peer Reviewed status of an item');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (78, 1, 'identifier', 'slug', 'a uri supplied via the sword slug header, as a suggested uri for the item');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (79, 1, 'language', 'rfc3066', 'the rfc3066 form of the language for the item');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (80, 1, 'rights', 'holder', 'The owner of the copyright');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (81, 3, 'externalIdentifier', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (82, 3, 'status', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (83, 3, 'citationTitle', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (84, 4, 'step', 'inProgressUsers', 'Contains the userIds of the users who are performing a step');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (85, 4, 'step', 'finishedUsers', 'Contains the userIds of the users who are performed a step');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (86, 4, 'step', 'reviewerKey', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (87, 4, 'step', 'approved', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (88, 4, 'step', 'rejectDate', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (89, 4, 'submit', 'toworkflow', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (90, 4, 'submit', 'skipReviewStage', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (91, 4, 'archive', 'mailUsers', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (92, 4, 'review', 'mailUsers', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (93, 4, 'genbank', 'token', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (94, 5, 'workflow', 'submitted', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (95, 5, 'workflow', 'selectjournal', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (96, 5, 'submit', 'showEmbargo', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (97, 6, 'publicationName', NULL, 'Title of the magazine, or other publication, in which a resource was/will be published.');
