--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------------------------------
-- Sequences for Process within Group feature
-------------------------------------------------------------------------------

CREATE TABLE Process2Group
(
  process_id INTEGER REFERENCES Process(process_id),
  group_id UUID REFERENCES epersongroup (uuid) ON DELETE CASCADE
);