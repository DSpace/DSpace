-- Author: dan.leehr@nescent.org
--

-- Manuscripts
DROP TABLE manuscript_active_version CASCADE;
DROP SEQUENCE manuscript_active_version_seq;

DROP TABLE manuscript CASCADE;
DROP SEQUENCE manuscript_seq CASCADE;

-- Organizations
DROP TABLE organization CASCADE;
DROP SEQUENCE organization_seq CASCADE;

-- OAuth2 Tokens
DROP TABLE oauth_token;
DROP SEQUENCE oauth_token_seq CASCADE;
