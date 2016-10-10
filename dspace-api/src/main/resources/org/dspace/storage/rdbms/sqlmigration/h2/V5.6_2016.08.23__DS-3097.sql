--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

------------------------------------------------------
-- DS-3097 introduced new action id for WITHDRAWN_READ 
------------------------------------------------------

UPDATE resourcepolicy SET action_id = 12 where action_id = 0 and resource_type_id = 0 and resource_id in (
	SELECT bundle2bitstream.bitstream_id FROM bundle2bitstream
		LEFT JOIN item2bundle ON bundle2bitstream.bundle_id = item2bundle.bundle_id
		LEFT JOIN item ON item2bundle.item_id = item.item_id
		WHERE item.withdrawn = 1
);

UPDATE resourcepolicy SET action_id = 12 where action_id = 0 and resource_type_id = 1 and resource_id in (
	SELECT item2bundle.bundle_id FROM item2bundle
		LEFT JOIN item ON item2bundle.item_id = item.item_id
		WHERE item.withdrawn = 1
);