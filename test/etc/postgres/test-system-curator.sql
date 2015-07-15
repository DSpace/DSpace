--
-- This script creates an eperson in the Dryad Postgres database for testing
-- dleehr 2014-11-25
--

--
-- Add the system curator to the eperson table
--

INSERT INTO eperson VALUES (getnextid('eperson'), 'system.curator@datadryad.org', '50a67500487f6a4c36a133cf796f7af1','System','Curator','t',null,'t',null,null,0,null,'en','t');

--
-- Put the system curator in the curators group
--

-- Workflow fails if there is a null eperson_id in the epersongroup2eperson table
-- It would only exist after a fresh_install, which is not done in the test environment
-- so we delete invalid rows here.
DELETE FROM epersongroup2eperson WHERE eperson_id is null;

INSERT INTO epersongroup2eperson (id,eperson_group_id, eperson_id) VALUES (nextval('epersongroup2eperson_seq'),(SELECT eperson_group_id from epersongroup WHERE name = 'Curators'),(SELECT eperson_id FROM eperson WHERE email = 'system.curator@datadryad.org'));



--
-- Editable Authority Control
--

INSERT INTO scheme (id, identifier, created, modified, status, lang, topconcept) VALUES (1, 'Journal', '2015-03-28', '2015-03-28', 'Published', 'en', NULL);

INSERT INTO concept (id, identifier, created, modified, status, lang, source, topconcept) VALUES (1, 'a3ceae4381184055a4d4fff98687d6cd', '2015-03-28', '2015-03-28', 'ACCEPTED', 'en', 'LOCAL-DryadJournal', true);
INSERT INTO concept (id, identifier, created, modified, status, lang, source, topconcept) VALUES (2, 'f976841ef6f94b4aa746a749da4d78b2', '2015-03-28', '2015-03-28', 'ACCEPTED', 'en', 'LOCAL-DryadJournal', true);

INSERT INTO scheme2concept (id, scheme_id, concept_id) VALUES (1, 1, 1);
INSERT INTO scheme2concept (id, scheme_id, concept_id) VALUES (2, 1, 2);

INSERT INTO term (id, identifier, created, modified, source, status, literalform, lang) VALUES (1, '0316b156fd134ab08e0bbf19bc914a2f', '2015-03-28', '2015-03-28', 'LOCAL-DryadJournal', NULL, 'Dryad Testing Blackout Journal', 'en');
INSERT INTO term (id, identifier, created, modified, source, status, literalform, lang) VALUES (2, '90b68d3e002e4f3496598846baf71faf', '2015-03-28', '2015-03-28', 'LOCAL-DryadJournal', NULL, 'Dryad Testing Journal', 'en');

INSERT INTO concept2term (id, concept_id, term_id, role_id) VALUES (1, 1, 1, 1);
INSERT INTO concept2term (id, concept_id, term_id, role_id) VALUES (2, 2, 2, 1);

INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (1, 1, 121, 'testbo', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (2, 1, 122, 'Dryad Testing Blackout Journal', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (3, 1, 123, '/opt/dryad/submission/journalMetadata/testbo', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (4, 1, 124, 'manuscriptCentral', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (5, 1, 125, 'true', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (6, 1, 127, 'true', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (7, 1, 126, 'true', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (8, 1, 128, 'true', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (9, 1, 129, 'true', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (10, 1, 131, 'ryan-dryad-testing@scherle.org', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (11, 1, 135, 'test', '', 1, NULL, 0, NULL);

INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (11, 2, 121, 'test', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (12, 2, 122, 'Dryad Testing Journal', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (13, 2, 123, '/opt/dryad/submission/journalMetadata/test', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (14, 2, 124, 'manuscriptCentral', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (15, 2, 125, 'true', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (16, 2, 127, 'true', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (17, 2, 126, 'true', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (18, 2, 128, 'false', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (19, 2, 129, 'true', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (20, 2, 131, 'ryan-dryad-testing@scherle.org', '', 1, NULL, 0, NULL);
INSERT INTO conceptmetadatavalue (id, parent_id, field_id, text_value, text_lang, place, authority, confidence, hidden) VALUES (21, 2, 135, 'test', '', 1, NULL, 0, NULL);
