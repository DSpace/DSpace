--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--
-- Create table to store handles being redirected to another location rather than default DSpace url
--
CREATE SEQUENCE handle_redirect_id_seq;
CREATE TABLE HandleRedirect
(
    handle_redirect_id  INTEGER PRIMARY KEY DEFAULT nextval('handle_redirect_id_seq'),
    handle              VARCHAR(256) UNIQUE,
    url                 VARCHAR(2048) UNIQUE
);
ALTER SEQUENCE handle_redirect_id_seq OWNED BY HandleRedirect.handle_redirect_id;

CREATE INDEX handleredirect_handle_idx ON HandleRedirect(handle);
