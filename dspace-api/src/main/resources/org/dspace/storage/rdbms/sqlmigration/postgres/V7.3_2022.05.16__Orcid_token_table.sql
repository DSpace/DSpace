--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create table for ORCID access tokens
-----------------------------------------------------------------------------------

CREATE SEQUENCE orcid_token_id_seq;

CREATE TABLE orcid_token
(
    id INTEGER NOT NULL,
    eperson_id uuid NOT NULL UNIQUE,
    profile_item_id uuid,
    access_token VARCHAR(100) NOT NULL,
    CONSTRAINT orcid_token_pkey PRIMARY KEY (id),
    CONSTRAINT orcid_token_eperson_id_fkey FOREIGN KEY (eperson_id) REFERENCES eperson (uuid),
    CONSTRAINT orcid_token_profile_item_id_fkey FOREIGN KEY (profile_item_id) REFERENCES item (uuid)
);
