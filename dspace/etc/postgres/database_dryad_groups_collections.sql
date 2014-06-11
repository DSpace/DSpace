--
-- This script loads minimal data into a Dryad Postgres database
-- It creates records for the Dryad collections, and epersongroups
-- It should only be run after installing the DSpace database with
-- ant fresh_install
--
-- dleehr 2014-06-11
--

--
-- epersongroup table
--

-- Anonymous and administrator are created by ant fresh_install
--INSERT INTO epersongroup VALUES (0, 'Anonymous');
--INSERT INTO epersongroup VALUES (1, 'Administrator');
INSERT INTO epersongroup VALUES (6, 'COLLECTION_1_ADMIN');
INSERT INTO epersongroup VALUES (7, 'COLLECTION_2_ADMIN');
INSERT INTO epersongroup VALUES (9, 'COLLECTION_3_SUBMIT');
INSERT INTO epersongroup VALUES (2, 'COLLECTION_1_SUBMIT');
INSERT INTO epersongroup VALUES (4, 'COLLECTION_2_SUBMIT');
INSERT INTO epersongroup VALUES (3, 'COLLECTION_1_WORKFLOW_STEP_2');
INSERT INTO epersongroup VALUES (5, 'COLLECTION_2_WORKFLOW_STEP_2');
INSERT INTO epersongroup VALUES (10, 'COLLECTION_3_ADMIN');
INSERT INTO epersongroup VALUES (8, 'Curators');
INSERT INTO epersongroup VALUES (11, 'COLLECTION_2_WORKFLOW_ROLE_curator');
INSERT INTO epersongroup VALUES (13, 'COLLECTION_6_ADMIN');
INSERT INTO epersongroup VALUES (12, 'COLLECTION_2_WORKFLOW_ROLE_editors');
INSERT INTO epersongroup VALUES (14, 'COLLECTION_6_SUBMIT');
INSERT INTO epersongroup VALUES (15, 'COLLECTION_6_WORKFLOW_ROLE_curator');
INSERT INTO epersongroup VALUES (16, 'COLLECTION_6_WORKFLOW_ROLE_editors');
INSERT INTO epersongroup VALUES (17, 'COLLECTION_7_ADMIN');
INSERT INTO epersongroup VALUES (18, 'COLLECTION_7_SUBMIT');
INSERT INTO epersongroup VALUES (19, 'COLLECTION_7_WORKFLOW_ROLE_curator');
INSERT INTO epersongroup VALUES (20, 'COLLECTION_7_WORKFLOW_ROLE_editors');

--
-- Adding administrator to Curators group
-- Submissions do not go into curation unless there is a user in the curators group.
--
INSERT INTO epersongroup2eperson (id,eperson_group_id, eperson_id) VALUES (nextval('epersongroup2eperson_seq'),8,(SELECT eperson_id FROM epersongroup2eperson WHERE eperson_group_id = 1));

--
-- Adding Groups to other groups.
--

INSERT INTO group2group VALUES (9, 6, 1);
INSERT INTO group2group VALUES (10, 6, 8);
INSERT INTO group2group VALUES (11, 7, 1);
INSERT INTO group2group VALUES (12, 7, 8);
INSERT INTO group2group VALUES (13, 9, 8);
INSERT INTO group2group VALUES (14, 9, 1);
INSERT INTO group2group VALUES (15, 2, 0);
INSERT INTO group2group VALUES (16, 2, 1);
INSERT INTO group2group VALUES (17, 2, 8);
INSERT INTO group2group VALUES (18, 4, 0);
INSERT INTO group2group VALUES (19, 4, 1);
INSERT INTO group2group VALUES (20, 4, 8);
INSERT INTO group2group VALUES (21, 3, 1);
INSERT INTO group2group VALUES (22, 3, 8);
INSERT INTO group2group VALUES (23, 5, 1);
INSERT INTO group2group VALUES (24, 5, 8);
INSERT INTO group2group VALUES (25, 10, 8);
INSERT INTO group2group VALUES (26, 10, 1);
INSERT INTO group2group VALUES (27, 11, 8);
INSERT INTO group2group VALUES (29, 13, 8);
INSERT INTO group2group VALUES (28, 12, 8);
INSERT INTO group2group VALUES (30, 14, 8);
INSERT INTO group2group VALUES (31, 15, 8);
INSERT INTO group2group VALUES (32, 16, 8);
INSERT INTO group2group VALUES (33, 17, 8);
INSERT INTO group2group VALUES (34, 18, 8);
INSERT INTO group2group VALUES (35, 19, 8);
INSERT INTO group2group VALUES (36, 20, 8);

INSERT INTO group2groupcache VALUES (9, 6, 1);
INSERT INTO group2groupcache VALUES (10, 6, 8);
INSERT INTO group2groupcache VALUES (11, 7, 1);
INSERT INTO group2groupcache VALUES (12, 7, 8);
INSERT INTO group2groupcache VALUES (13, 9, 8);
INSERT INTO group2groupcache VALUES (14, 9, 1);
INSERT INTO group2groupcache VALUES (15, 2, 0);
INSERT INTO group2groupcache VALUES (16, 2, 1);
INSERT INTO group2groupcache VALUES (17, 2, 8);
INSERT INTO group2groupcache VALUES (18, 4, 0);
INSERT INTO group2groupcache VALUES (19, 4, 1);
INSERT INTO group2groupcache VALUES (20, 4, 8);
INSERT INTO group2groupcache VALUES (21, 3, 1);
INSERT INTO group2groupcache VALUES (22, 3, 8);
INSERT INTO group2groupcache VALUES (23, 5, 1);
INSERT INTO group2groupcache VALUES (24, 5, 8);
INSERT INTO group2groupcache VALUES (25, 10, 8);
INSERT INTO group2groupcache VALUES (26, 10, 1);
INSERT INTO group2groupcache VALUES (27, 11, 8);
INSERT INTO group2groupcache VALUES (29, 13, 8);
INSERT INTO group2groupcache VALUES (28, 12, 8);
INSERT INTO group2groupcache VALUES (30, 14, 8);
INSERT INTO group2groupcache VALUES (31, 15, 8);
INSERT INTO group2groupcache VALUES (32, 16, 8);
INSERT INTO group2groupcache VALUES (33, 17, 8);
INSERT INTO group2groupcache VALUES (34, 18, 8);
INSERT INTO group2groupcache VALUES (35, 19, 8);
INSERT INTO group2groupcache VALUES (36, 20, 8);

--
-- collection table
-- 
-- Last two columns are submitter and admin, so these must be present
-- 'Dryad Data Files' and 'Dryad Data Packages' reference epersongroups, 5 and 3 which must be present

INSERT INTO collection VALUES (1, 'Dryad Data Files', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 3, NULL, 2, 6);
INSERT INTO collection VALUES (2, 'Dryad Data Packages', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 5, NULL, 4, 7);
INSERT INTO collection VALUES (3, 'BIRDD', 'Data from the BIRDD collection', 'BIRDD (Beagle Investigations Return with Darwinian Data) is a collection of data relating to Galapagos finches. It spans multiples publications from multiple researchers, but all data has been converted into standardized formats for easy comparison.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 9, 10);
INSERT INTO collection VALUES (4, 'KNB', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO collection VALUES (5, 'TreeBASE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO collection VALUES (6, 'DryadLab Packages', 'Educational Packages in DryadLab', '', NULL, NULL, '', '', '', '', NULL, NULL, NULL, 14, 13);
INSERT INTO collection VALUES (7, 'DryadLab Activities', 'Educational Activities in DryadLab', '', NULL, NULL, '', '', '', '', NULL, NULL, NULL, 18, 17);

--
-- Collection roles. Required for submission.
-- Workflow actions are based on roles.
--

INSERT INTO collectionrole VALUES (1, 'curator', 2, 11);
INSERT INTO collectionrole VALUES (3, 'curator', 6, 15);
INSERT INTO collectionrole VALUES (2, 'editors', 2, 12);
INSERT INTO collectionrole VALUES (4, 'editors', 6, 16);
INSERT INTO collectionrole VALUES (5, 'curator', 7, 19);
INSERT INTO collectionrole VALUES (6, 'editors', 7, 20);

--
-- handle table
--
-- Dryad uses the HandleManager to resolve collections like its Data Packages and Data Files collections

INSERT INTO handle VALUES (1, '10255/1', 4, 1);
INSERT INTO handle VALUES (2, '10255/2', 3, 1);
INSERT INTO handle VALUES (3, '10255/3', 3, 2);
INSERT INTO handle VALUES (148, '10255/dryad.148', 3, 3);
INSERT INTO handle VALUES (2027, '10255/dryad.2027', 3, 4);
INSERT INTO handle VALUES (2171, '10255/dryad.2171', 3, 5);
INSERT INTO handle VALUES (7871, '10255/dryad.7871', 3, 6);
INSERT INTO handle VALUES (7872, '10255/dryad.7872', 3, 7);


-- Submission fails if community2collection is not set up

--
-- community table
--

INSERT INTO community VALUES (1, 'Main', '', NULL, NULL, NULL, NULL, NULL);

--
-- community2collection
--

INSERT INTO community2collection VALUES (1, 1, 1);
INSERT INTO community2collection VALUES (2, 1, 2);
INSERT INTO community2collection VALUES (3, 1, 3);
INSERT INTO community2collection VALUES (4, 1, 4);
INSERT INTO community2collection VALUES (5, 1, 5);
INSERT INTO community2collection VALUES (6, 1, 6);
INSERT INTO community2collection VALUES (7, 1, 7);

-- resource policies for collections

INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,1,0,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,1,10,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,1,9,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,1,3,2);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,1,3,3);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,2,0,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,2,10,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,2,9,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,2,3,4);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,2,3,5);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,1,11,6);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,2,11,7);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,3,0,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,3,10,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,3,9,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,3,3,9);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,3,11,10);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,2,1,7);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,2,4,7);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,1,1,6);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,1,3,6);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,1,4,6);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,3,1,6);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,3,3,6);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,3,4,6);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,4,0,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,4,10,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,4,9,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,5,0,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,5,10,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,5,9,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,2,3,11);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,2,3,12);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,6,0,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,6,10,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,6,9,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,6,11,13);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,6,3,14);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,6,3,15);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,6,3,16);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,7,0,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,7,10,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,7,9,0);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,7,11,17);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,7,3,18);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,7,3,19);
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id) VALUES (nextval('resourcepolicy_seq'),3,7,3,20);

-- Should run update-sequences.sql after this


