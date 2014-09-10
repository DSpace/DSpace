-- Author: dan.leehr@nescent.org
--

-- Manuscripts
DROP TABLE manuscript CASCADE;
DROP SEQUENCE manuscript_seq CASCADE;

-- Organizations
DROP TABLE organization CASCADE;
DROP SEQUENCE organization_seq CASCADE;

-- OAuth2 Tokens
DROP TABLE oauth_token;
DROP SEQUENCE oauth_token_seq CASCADE;

-- Authorizations
DROP TABLE rest_resource_authz CASCADE;
DROP SEQUENCE rest_resource_authz_seq CASCADE;
