-- Author: dan.leehr@nescent.org
--

-- Manuscripts
CREATE SEQUENCE manuscript_seq;
CREATE TABLE manuscript
(
  manuscript_id INTEGER PRIMARY KEY not null default nextval('manuscript_seq'),
  organization_id INTEGER not null,
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
);

-- modifications added by daisieh on 11/11/15
ALTER TABLE manuscript ADD COLUMN date_added DATE DEFAULT current_date;
ALTER TABLE manuscript ADD COLUMN status text;

-- modifications added by daisieh on 12/06/15
CREATE VIEW journal_code_view AS
SELECT journal_id.parent_id as organization_id, journal_id.text_value as code
FROM conceptmetadatavalue as journal_id, metadataschemaregistry journalschema
  INNER JOIN metadatafieldregistry jid on journalschema.short_id='journal' and jid.metadata_schema_id=journalschema.metadata_schema_id and jid.element = 'journalID'
  WHERE jid.metadata_field_id = journal_id.field_id
  ;
CREATE VIEW journal_name_view AS
SELECT journal_id.parent_id as organization_id, journal_id.text_value as name
FROM conceptmetadatavalue as journal_id, metadataschemaregistry journalschema
  INNER JOIN metadatafieldregistry jid on journalschema.short_id='journal' and jid.metadata_schema_id=journalschema.metadata_schema_id and jid.element = 'fullname'
  WHERE jid.metadata_field_id = journal_id.field_id
  ;
CREATE VIEW journal_issn_view AS
SELECT journal_id.parent_id as organization_id, journal_id.text_value as issn
FROM conceptmetadatavalue as journal_id, metadataschemaregistry journalschema
  INNER JOIN metadatafieldregistry jid on journalschema.short_id='journal' and jid.metadata_schema_id=journalschema.metadata_schema_id and jid.element = 'issn'
  WHERE jid.metadata_field_id = journal_id.field_id
  ;

DROP TABLE if EXISTS organization;
-- can't have a foreign key constraint based on a view
ALTER TABLE manuscript DROP CONSTRAINT if EXISTS manuscript_organization_id_fkey;

CREATE VIEW organization AS
SELECT journal_code_view.organization_id, code, name, issn
from journal_code_view
  inner join journal_name_view on journal_name_view.organization_id = journal_code_view.organization_id
  left join journal_issn_view on journal_issn_view.organization_id = journal_code_view.organization_id
;

CREATE or replace VIEW journal AS
SELECT journal_issn_view.organization_id as concept_id, code, name, issn
from journal_issn_view
  inner join journal_name_view on journal_issn_view.organization_id = journal_name_view.organization_id
  inner join journal_code_view on journal_issn_view.organization_id = journal_code_view.organization_id
;