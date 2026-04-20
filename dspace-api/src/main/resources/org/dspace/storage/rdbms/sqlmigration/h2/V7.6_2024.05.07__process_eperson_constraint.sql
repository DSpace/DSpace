--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- If Process references an EPerson that no longer exists, set the "user_id" to null.
UPDATE process SET user_id = null WHERE NOT EXISTS (SELECT * FROM EPerson where uuid = process.user_id);

-- Add new constraint where process.user_id is nullified if referenced EPerson is deleted.
ALTER TABLE process ADD CONSTRAINT process_eperson FOREIGN KEY (user_id) REFERENCES EPerson(uuid) ON DELETE SET NULL;
