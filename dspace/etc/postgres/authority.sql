--
-- database_schema.sql
--
-- Version: $Revision$
--
-- Date:    $Date$
--
-- Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
--
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are
-- met:
--
-- - Redistributions of source code must retain the above copyright
-- notice, this list of conditions and the following disclaimer.
--
-- - Redistributions in binary form must reproduce the above copyright
-- notice, this list of conditions and the following disclaimer in the
-- documentation and/or other materials provided with the distribution.
--
-- - Neither the name of the DSpace Foundation nor the names of its
-- contributors may be used to endorse or promote products derived from
-- this software without specific prior written permission.
--
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
-- ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
-- LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
-- A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
-- HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
-- INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
-- BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
-- OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
-- ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
-- TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
-- USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
-- DAMAGE.
-------------------------------------------------------

-------------------------------------------------------
-- Sequences for creating new IDs (primary keys) for
-- tables.  Each table must have a corresponding
-- sequence called 'tablename_seq'.
-------------------------------------------------------

CREATE SEQUENCE scheme_seq;
CREATE SEQUENCE schememetadatavalue_seq;




CREATE SEQUENCE concept_seq;
CREATE SEQUENCE conceptmetadatavalue_seq;


CREATE SEQUENCE   scheme2concept_seq;

CREATE SEQUENCE term_seq;
CREATE SEQUENCE termmetadatavalue_seq;


CREATE SEQUENCE concept2term_seq;
CREATE SEQUENCE concept2termrole_seq;
CREATE SEQUENCE concept2conceptrole_seq;
CREATE SEQUENCE concept2concept_seq;

--------------------------------------------------------------------------------------------------------------
-- METADATA CONCEPT AND ATTRIBUTES
--------------------------------------------------------------------------------------------------------------
CREATE TABLE Scheme
(
  id   INTEGER PRIMARY KEY DEFAULT NEXTVAL('scheme_seq'),
  identifier          VARCHAR(256),
  created DATE,
  modified DATE,
  status VARCHAR(256),
  lang VARCHAR(24),
  topconcept BOOL,
  CONSTRAINT SchemeIndentifier UNIQUE(identifier)
);


-- Notes and Attributes describing the Concepts
CREATE TABLE SchemeMetadataValue
(
  id                 INTEGER PRIMARY KEY DEFAULT NEXTVAL('schememetadatavalue_seq'),
  parent_id         	   INTEGER REFERENCES Scheme(id),
  field_id           INTEGER REFERENCES MetadataFieldRegistry(metadata_field_id),
  text_value         TEXT,
  text_lang          VARCHAR(24),
  place              INTEGER,
  authority          VARCHAR(100),
  confidence         INTEGER DEFAULT -1,
  hidden             BOOL
);






CREATE TABLE Concept
(
  id   INTEGER PRIMARY KEY DEFAULT NEXTVAL('concept_seq'),
  identifier          VARCHAR(256),
  created DATE,
  modified DATE,
  status VARCHAR(256),
  lang VARCHAR(24),
  source VARCHAR(256),
  topconcept BOOL,
  CONSTRAINT ConceptIndentifier UNIQUE(identifier)
);



-- Notes and Attributes describing the Concepts
CREATE TABLE ConceptMetadataValue
(
  id  INTEGER PRIMARY KEY DEFAULT NEXTVAL('conceptmetadatavalue_seq'),
  parent_id         	   INTEGER REFERENCES Concept(id),
  field_id           INTEGER REFERENCES MetadataFieldRegistry(metadata_field_id),
  text_value         TEXT,
  text_lang          VARCHAR(24),
  place              INTEGER,
  authority          VARCHAR(100),
  confidence         INTEGER DEFAULT -1,
  hidden             BOOL
);


--------------------------------------------------------------------------------------------------------------
-- METADATA TERM AND ATTRIBUTES
--------------------------------------------------------------------------------------------------------------
CREATE TABLE Term
(
  id  INTEGER PRIMARY KEY DEFAULT NEXTVAL('term_seq'),
  identifier          VARCHAR(256),
  created DATE,
  modified DATE,
  source VARCHAR(256),
  status VARCHAR(256),
  literalForm TEXT,
  lang VARCHAR(24),
  CONSTRAINT TermIndentifier UNIQUE(identifier)

);

-- Notes and Attributes describing the Term
CREATE TABLE TermMetadataValue
(
  id  INTEGER PRIMARY KEY DEFAULT NEXTVAL('termmetadatavalue_seq'),
  parent_id         	   INTEGER REFERENCES Term(id),
  field_id           INTEGER REFERENCES MetadataFieldRegistry(metadata_field_id),
  text_value         TEXT,
  text_lang          VARCHAR(24),
  place              INTEGER,
  authority          VARCHAR(100),
  confidence         INTEGER DEFAULT -1,
  hidden             BOOL
);


--------------------------------------------------------------------------------------------------------------
-- METADATA CONCEPT ASSOCIATIVE RELATIONSHIP   Concept2ConceptRole    Concept2Concept
--------------------------------------------------------------------------------------------------------------
CREATE TABLE Concept2ConceptRole
(
  id             INTEGER PRIMARY KEY DEFAULT NEXTVAL('concept2conceptrole_seq'),
  role           VARCHAR(64),
  label          VARCHAR(64),
  scope_note     TEXT,
  CONSTRAINT Concept2ConceptRoleName UNIQUE(label)
);

CREATE TABLE Concept2Concept
(
  id            INTEGER PRIMARY KEY DEFAULT NEXTVAL('concept2concept_seq'),
  incoming_id   INTEGER REFERENCES Concept(id),
  outgoing_id   INTEGER REFERENCES Concept(id),
  role_id   INTEGER REFERENCES Concept2ConceptRole(id)
);



--------------------------------------------------------------------------------------------------------------
-- METADATA CONCEPT / TERM RELATIONSHIPS
--------------------------------------------------------------------------------------------------------------
CREATE TABLE Concept2TermRole
(
  id             INTEGER PRIMARY KEY DEFAULT NEXTVAL('concept2termrole_seq'),
  role           VARCHAR(64),
  label          VARCHAR(64),
  scope_note     TEXT,
  CONSTRAINT Concept2TermRoleName UNIQUE(label)
);
CREATE TABLE Concept2Term
(
  id            INTEGER PRIMARY KEY DEFAULT NEXTVAL('concept2term_seq'),
  concept_id   	INTEGER REFERENCES Concept(id),
  term_id   	INTEGER REFERENCES Term(id),
  role_id   INTEGER REFERENCES Concept2TermRole(id)
);


CREATE TABLE Scheme2Concept
(
  id            INTEGER PRIMARY KEY DEFAULT NEXTVAL('scheme2concept_seq'),
  scheme_id   	INTEGER REFERENCES Scheme(id),
  concept_id   	INTEGER REFERENCES Concept(id)

);

--------------------------------------------------------------------------------------------------------------
-- EXAMPLE DATA
--------------------------------------------------------------------------------------------------------------
--
INSERT INTO Scheme VALUES (nextval('scheme_seq'),'Author','2014-09-20','2014-09-20','Accepted','en','true');

INSERT INTO Concept2TermRole VALUES (nextval('concept2termrole_seq'),'prefLabel','prefLabel');
INSERT INTO Concept2TermRole VALUES (nextval('concept2termrole_seq'),'alt','altfLabel');

INSERT INTO Concept2ConceptRole VALUES (nextval('concept2conceptrole_seq'),'hierarchical','Broader/Narrower');
INSERT INTO Concept2ConceptRole VALUES (nextval('concept2conceptrole_seq'),'associative','Associate');
INSERT INTO Concept2ConceptRole VALUES (nextval('concept2conceptrole_seq'),'associative','Equal');


ALTER TABLE eperson ADD COLUMN orcid VARCHAR(256);

ALTER TABLE eperson ADD COLUMN access_token VARCHAR(256);





