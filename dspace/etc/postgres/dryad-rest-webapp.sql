-- Author: dan.leehr@nescent.org
--

-- Organizations
CREATE SEQUENCE organization_seq;
CREATE TABLE organization
(
  organization_id INTEGER PRIMARY KEY not null default nextval('organization_seq'),
  code VARCHAR(32) not null,
  name VARCHAR(255) not null
);

CREATE UNIQUE INDEX org_code_idx on organization(code);

-- Manuscripts
CREATE SEQUENCE manuscript_seq;
CREATE TABLE manuscript
(
  manuscript_id INTEGER PRIMARY KEY not null default nextval('manuscript_seq'),
  organization_id INTEGER not null REFERENCES organization(organization_id),
  msid VARCHAR(255) not null,
  version INTEGER not null default 1,
  active VARCHAR(5) not null default 'true',
  json_data text 
);

CREATE UNIQUE INDEX manuscript_msid_org_ver_idx on manuscript(msid, organization_id, version);
CREATE INDEX manuscript_msid_idx ON manuscript(msid);

-- OAuth2 Tokens
CREATE SEQUENCE oauth_token_seq;
CREATE TABLE oauth_token
(
  oauth_token_id INTEGER PRIMARY KEY not null default nextval('oauth_token_seq'),
  eperson_id INTEGER not null REFERENCES eperson(eperson_id),
  token VARCHAR(32) not null,
  expires DATE
);

-- API resource permissions - eperson, method, resource path
CREATE SEQUENCE rest_resource_authz_seq;
CREATE TABLE rest_resource_authz
(
  rest_resource_authz_id INTEGER PRIMARY KEY not null default nextval('rest_resource_authz_seq'),
  eperson_id INTEGER not null REFERENCES eperson(eperson_id),
  http_method VARCHAR(8) not null,
  resource_path VARCHAR(1024) not null
)
