-- Author: dan.leehr@nescent.org
--
-- PREFIX is a reserved word in postgres, so use doi_prefix
-- In 'doi:10.5061/dryad.1a34f' -> 'http://datadryad.org/handle/10255/dryad.4567'
-- doi_prefix: '10.5061'
-- doi_suffix: 'dryad.1a34f'
-- url: 'http://datadryad.org/handle/10255/dryad.4567'
-- doi_prefix + doi_suffix must be unique together
-- multiple prefix+suffix may map the the same url
-- url need not be unique, but doi_prefix + doi_suffix + url must be unique


CREATE SEQUENCE doi_seq;
CREATE TABLE doi
(
  doi_id INTEGER PRIMARY KEY not null default nextval('doi_seq'),
  doi_prefix VARCHAR(32) not null,
  doi_suffix VARCHAR(256) not null,
  url VARCHAR(1024) not null
);

CREATE UNIQUE INDEX doi_pfx_sfx_idx ON doi(doi_prefix,doi_suffix);
CREATE UNIQUE INDEX doi_pfx_sfx_url_idx ON doi(doi_prefix,doi_suffix,url);
CREATE INDEX doi_url_idx ON doi(url);
