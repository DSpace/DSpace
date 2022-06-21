--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------------------------------
-- Table to store Groups related to a Process on its creation
-------------------------------------------------------------------------------

CREATE TABLE Process2Group
(
    process_id INTEGER REFERENCES Process(process_id),
    group_id UUID REFERENCES epersongroup (uuid) ON DELETE CASCADE,
    CONSTRAINT PK_Process2Group PRIMARY KEY (process_id, group_id)
);