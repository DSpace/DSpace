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

INSERT INTO epersongroup2eperson (id,eperson_group_id, eperson_id) VALUES (nextval('epersongroup2eperson_seq'),(SELECT eperson_group_id from epersongroup WHERE name = 'Curators'),(SELECT eperson_id FROM eperson WHERE email = 'system.curator@datadryad.org'));
