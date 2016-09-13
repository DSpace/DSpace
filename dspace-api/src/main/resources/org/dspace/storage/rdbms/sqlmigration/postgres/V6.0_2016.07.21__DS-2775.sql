--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

------------------------------------------------------
-- DS-2775 Drop unused sequences
------------------------------------------------------

DROP SEQUENCE IF EXISTS bitstream_seq;
DROP SEQUENCE IF EXISTS bundle2bitstream_seq;
DROP SEQUENCE IF EXISTS bundle_seq;
DROP SEQUENCE IF EXISTS collection2item_seq;
DROP SEQUENCE IF EXISTS collection_seq;
DROP SEQUENCE IF EXISTS community2collection_seq;
DROP SEQUENCE IF EXISTS community2community_seq;
DROP SEQUENCE IF EXISTS community_seq;
DROP SEQUENCE IF EXISTS dcvalue_seq;
DROP SEQUENCE IF EXISTS eperson_seq;
DROP SEQUENCE IF EXISTS epersongroup2eperson_seq;
DROP SEQUENCE IF EXISTS epersongroup2workspaceitem_seq;
DROP SEQUENCE IF EXISTS epersongroup_seq;
DROP SEQUENCE IF EXISTS group2group_seq;
DROP SEQUENCE IF EXISTS group2groupcache_seq;
DROP SEQUENCE IF EXISTS historystate_seq;
DROP SEQUENCE IF EXISTS item2bundle_seq;
DROP SEQUENCE IF EXISTS item_seq;
