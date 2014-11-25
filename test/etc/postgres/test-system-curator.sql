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
