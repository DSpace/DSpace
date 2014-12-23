-- Database data for Dryad metadata and bitstream registries as of 2014-12-02
-- Captured statically for use in a test database, should not be used in production deployments
-- instead, load metadata schemas using MetadataImporter and install bitstream registries
-- using ant fresh_install
--
-- This file was created from
-- pg_dump -t metadataschemaregistry --column-inserts --data-only
-- pg_dump -t metadatafieldregistry --column-inserts --data-only
-- pg_dump -t bitstreamformatregistry --column-inserts --data-only

--
-- Data for Name: metadataschemaregistry
--

INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (1, 'http://dublincore.org/documents/dcmi-terms/', 'dc');
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (4, 'http://purl.org/dryad/terms/', 'dryad');
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (5, 'http://prismstandard.org/namespaces/basic/2.0/', 'prism');
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (6, 'http://www.dspace.org/workflow', 'workflow');
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (7, 'http://www.dspace.org/system', 'internal');
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (3, 'http://rs.tdwg.org/dwc/dwcore/', 'dwc');
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id) VALUES (8, 'http://purl.org/dspace/models/person', 'person');

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
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (67, 3, 'ScientificName', NULL, 'http://wiki.tdwg.org/twiki/bin/view/DarwinCore/ScientificName');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (68, 1, 'date', 'updated', 'The last time the item was updated via the SWORD interface');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (69, 1, 'description', 'version', 'The Peer Reviewed status of an item');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (70, 1, 'identifier', 'slug', 'a uri supplied via the sword slug header, as a suggested uri for the item');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (71, 1, 'language', 'rfc3066', 'the rfc3066 form of the language for the item');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (72, 1, 'rights', 'holder', 'The owner of the copyright');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (73, 1, 'date', 'embargoedUntil', 'Until this date, the item is under embargo and not visible to normal users.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (74, 1, 'contributor', 'correspondingAuthor', '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (75, 4, 'status', NULL, '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (76, 1, 'identifier', 'manuscriptNumber', 'A number used for internal tracking by the publisher before the item''s publication has been finalized.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (77, 5, 'publicationName', NULL, 'Title of the magazine, or other publication, in which a resource was/will be published.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (82, 1, 'coverage', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (83, 1, 'submit', 'showEmbargo', '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (79, 1, 'workflow', 'submitted', 'This field is used internally by the submission system.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (80, 1, 'type', 'embargo', 'This field is used internally to manage embargoes.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (78, 1, 'relation', 'external', 'Link to related content in an external repository. (deprecated, use dryad.external instead)');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (81, 4, 'external', NULL, 'Link to related content in an external repository. This is typically stored as an XML chunk containing the URL or other identifier as well as the repository name.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (84, 4, 'externalIdentifier', NULL, '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (85, 6, 'archive', 'mailUsers', '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (86, 6, 'step', 'approved', '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (87, 6, 'step', 'finishedUsers', '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (88, 6, 'step', 'inProgressUsers', '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (89, 6, 'step', 'rejectDate', '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (90, 6, 'step', 'reviewerKey', '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (91, 6, 'submit', 'skipReviewStage', '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (92, 6, 'submit', 'toworkflow', '');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (93, 4, 'curatorNote', NULL, ' Free text note about the status of the item. Although this can only be edited by a curator, it is publicly visible.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (94, 7, 'workflow', 'submitted', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (95, 7, 'workflow', 'selectjournal', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (96, 7, 'submit', 'showEmbargo', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (97, 6, 'review', 'mailUsers', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (98, 1, 'audience', 'educationLevel', 'to describe educational modules in DryadLab');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (99, 4, 'learningOutcome', NULL, 'Used to store learning outcomes for DryadLab modules unless or until an externally defined field is selected for this purpose.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (100, 4, 'prerequisiteKnowledge', NULL, 'For DryadLab use only.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (101, 4, 'instructorBackground', NULL, 'For DryadLab use only.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (102, 4, 'descriptionOfActivity', NULL, 'For DryadLab use only.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (103, 4, 'citationTitle', NULL, 'A title to be used in citations, for cases where the main title refers to a subsection of the whole document.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (104, 1, 'workflow', 'selectjournal', 'Boolean used in the submission process only.');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (105, 1, 'identifier', 'externalresourceXml', 'External source xml value');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (106, 6, 'genbank', 'token', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (107, 8, 'orcid', 'id', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (108, 8, 'familyName', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (109, 8, 'givenName', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (110, 8, 'creditName', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (111, 8, 'otherName', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (112, 8, 'country', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (113, 8, 'keyword', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (114, 8, 'external', 'identifier', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (115, 8, 'researcher', 'url', NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (116, 8, 'biography', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (117, 8, 'email', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (118, 8, 'institution', NULL, NULL);
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (119, 1, 'date', 'blackoutUntil', 'Date on which submission should be released from blackout');
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note) VALUES (120, 4, 'curatorNotePublic', NULL, NULL);

--
-- Data for Name: bitstreamformatregistry
--

INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (1, 'application/octet-stream', 'Unknown', 'Unknown data format', 0, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (2, 'text/plain', 'License', 'Item-specific license agreed upon to submission', 1, true);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (5, 'text/plain', 'Text', 'Plain Text', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (61, 'application/x-bzip2', 'Bzip2', 'Bzip2 archive', 0, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (21, 'application/vnd.visio', 'Microsoft Visio', 'Microsoft Visio', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (22, 'application/x-filemaker', 'FMP3', 'Filemaker Pro', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (23, 'image/x-ms-bmp', 'BMP', 'Microsoft Windows bitmap', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (24, 'application/x-photoshop', 'Photoshop', 'Photoshop', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (47, 'audio/mpeg', 'MP3', 'MP3 audio', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (27, 'audio/x-mpeg', 'MPEG Audio', 'MPEG Audio', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (28, 'application/vnd.ms-project', 'Microsoft Project', 'Microsoft Project', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (55, 'application/x-tar', 'TAR archive', 'Tape Archive File (TAR)', 0, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (31, 'application/x-tex', 'TeX', 'Tex/LateX document', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (32, 'application/x-dvi', 'TeX dvi', 'TeX dvi format', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (34, 'application/wordperfect5.1', 'WordPerfect', 'WordPerfect 5.1 document', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (35, 'audio/x-pn-realaudio', 'RealAudio', 'RealAudio file', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (36, 'image/x-photo-cd', 'Photo CD', 'Kodak Photo CD image', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (29, 'application/mathematica', 'Mathematica', 'Mathematica Notebook', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (38, 'text/plain', 'Nexus', 'Nexus', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (12, 'image/jpeg', 'JPEG', 'JPEG Image', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (42, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 'Microsoft PowerPoint OpenXML', 'Microsoft PowerPoint OpenXML', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (41, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'Microsoft Word OpenXML', 'Microsoft Word OpenXML', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (40, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'Microsoft Excel OpenXML', 'Microsoft Excel OpenXML', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (18, 'audio/x-wav', 'WAV', 'Wave Audio Format', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (46, 'application/vnd.google-earth.kml+xml', 'KML', 'Keyhole Markup Language (KML)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (3, 'application/pdf', 'Adobe PDF', 'Adobe Portable Document Format (PDF)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (4, 'text/xml', 'XML', 'Extensible Markup Language (XML)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (50, 'text/plain', 'Newick', 'Newick tree file', 0, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (7, 'text/css', 'CSS', 'Cascading Style Sheets (CSS)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (51, 'application/x-python', 'Python', 'Python program', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (6, 'text/html', 'HTML', 'Hypertext Markup Language (HTML)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (8, 'application/msword', 'Microsoft Word', 'Microsoft Word 97-2007', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (9, 'application/vnd.ms-powerpoint', 'Microsoft Powerpoint', 'Microsoft Powerpoint 97-2007', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (54, 'text/plain', 'Perl', 'Perl program', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (10, 'application/vnd.ms-excel', 'Microsoft Excel', 'Microsoft Excel 97-2007', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (11, 'application/marc', 'MARC', 'Machine-Readable Cataloging records (MARC)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (56, 'text/plain', 'FASTA QUAL File', 'FASTA QUAL File', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (13, 'image/gif', 'GIF', 'Graphics Interchange Format (GIF)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (33, 'application/sgml', 'SGML', 'Standard Generalized Markup Language (SGML)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (14, 'image/png', 'PNG', 'Portable Network Graphics (PNG)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (15, 'image/tiff', 'TIFF', 'Tag Image File Format (TIFF)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (16, 'audio/x-aiff', 'AIFF', 'Audio Interchange File Format (AIFF)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (17, 'audio/basic', 'Basic Audio', 'Basic Audio (AU)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (19, 'video/mpeg', 'MPEG', 'Moving Picture Experts Group (MPEG)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (20, 'application/rtf', 'RTF', 'Rich Text Format (RTF)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (25, 'application/postscript', 'Postscript', 'Postscript', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (26, 'video/quicktime', 'Quicktime', 'Quicktime Video', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (30, 'application/x-latex', 'LaTeX', 'LaTeX document', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (39, 'text/csv', 'Comma-separated values', 'Comma-separated values (CSV)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (43, 'application/zip', 'Zip', 'Zip archive', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (57, 'application/x-tar', 'TGZ', 'UNIX Tar File Gzipped (TGZ)', 0, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (45, 'application/rdf+xml', 'OWL', 'Web Ontology Language (OWL)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (48, 'application/x-gzip', 'GNU ZIP', 'GZip archive', 0, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (49, 'text/plain', 'R script', 'R script', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (52, 'video/avi', 'AVI video', 'Audio Video Interleave (AVI)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (53, 'video/mp4', 'MPEG-4', 'MPEG-4 video', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (58, 'text/plain', 'Phylip', 'Phylogeny Inference Package (Phylip)', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (59, 'application/x-rar-compressed', 'RAR', 'Roshal ARchive (RAR)', 0, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (60, 'application/vnd.ms-excel.sheet.binary.macroEnabled.12', 'Microsoft Excel Binary XML', 'Microsoft Excel Binary XML', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (44, 'application/x-fasta', 'FASTA', 'FASTA sequence file', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (62, 'application/vnd.oasis.opendocument.text', 'ODF Text', 'Open Document Format Text File', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (63, 'application/vnd.oasis.opendocument.spreadsheet', 'ODF Spreadsheet', 'Open Document Format Spreadsheet', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (64, 'application/vnd.oasis.opendocument.presentation', 'ODF Presentation', 'Open Document Format Presentation', 1, false);
INSERT INTO bitstreamformatregistry (bitstream_format_id, mimetype, short_description, description, support_level, internal) VALUES (65, 'application/vnd.oasis.opendocument.database', 'ODF Database', 'Open Document Format Database', 1, false);
