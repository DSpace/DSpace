--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------------------------------
-- Sequences for RegistrationData within Group feature
-------------------------------------------------------------------------------

CREATE TABLE RegistrationData2Group
(
  registrationdata_id INTEGER REFERENCES RegistrationData(registrationdata_id),
  group_id UUID REFERENCES epersongroup (uuid) ON DELETE CASCADE
);