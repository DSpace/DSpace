--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

------------------------------------------------------
-- DS_3378 Lost oracle indexes
------------------------------------------------------
CREATE UNIQUE INDEX eperson_eperson on eperson(eperson_id);
CREATE UNIQUE INDEX epersongroup_eperson_group on epersongroup(eperson_group_id);
CREATE UNIQUE INDEX community_community on community(community_id);
CREATE UNIQUE INDEX collection_collection on collection(collection_id);
CREATE UNIQUE INDEX item_item on item(item_id);
CREATE UNIQUE INDEX bundle_bundle on bundle(bundle_id);
CREATE UNIQUE INDEX bitstream_bitstream on bitstream(bitstream_id);
