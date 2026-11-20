--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create TABLE mfa for multi-factor authentication settings
-----------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS mfa
(
    uuid         UUID         NOT NULL PRIMARY KEY,
    eperson_uuid UUID         NOT NULL REFERENCES eperson(uuid) ON DELETE CASCADE,
    mfa_type     VARCHAR(32)  NOT NULL DEFAULT 'TOTP',
    secret       VARCHAR(128) NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_on   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT mfa_eperson_type_unique UNIQUE (eperson_uuid, mfa_type)
);

CREATE INDEX IF NOT EXISTS idx_mfa_eperson_uuid ON mfa(eperson_uuid);

-----------------------------------------------------------------------------------
-- Create TABLE mfa_recovery_code for backup codes
-----------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS mfa_recovery_code
(
    uuid     UUID         NOT NULL PRIMARY KEY,
    mfa_uuid UUID         NOT NULL REFERENCES mfa(uuid) ON DELETE CASCADE,
    code_hash VARCHAR(128) NOT NULL,
    used     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_mfa_recovery_code_mfa_uuid ON mfa_recovery_code(mfa_uuid);
