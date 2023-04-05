--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- ===============================================================
-- WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
--
-- DO NOT MANUALLY RUN THIS DATABASE MIGRATION. IT WILL BE EXECUTED
-- AUTOMATICALLY (IF NEEDED) BY "FLYWAY" WHEN YOU STARTUP DSPACE.
-- http://flywaydb.org/
-- ===============================================================

-------------------------------------------------------------
-- This will create the setup for the dspace 7 entities usage
-------------------------------------------------------------
CREATE SEQUENCE entity_type_id_seq;
CREATE SEQUENCE relationship_type_id_seq;
CREATE SEQUENCE relationship_id_seq;

CREATE TABLE entity_type
(
    id                      INTEGER NOT NULL PRIMARY KEY,
    label                   varchar(32) UNIQUE NOT NULL
);

CREATE TABLE relationship_type
(
    id                      INTEGER NOT NULL PRIMARY KEY,
    left_type               INTEGER NOT NULL,
    right_type              INTEGER NOT NULL,
    left_label              varchar(32) NOT NULL,
    right_label             varchar(32) NOT NULL,
    left_min_cardinality    INTEGER,
    left_max_cardinality    INTEGER,
    right_min_cardinality   INTEGER,
    right_max_cardinality   INTEGER,
    FOREIGN KEY (left_type)   REFERENCES entity_type(id),
    FOREIGN KEY (right_type)  REFERENCES entity_type(id),
    CONSTRAINT u_relationship_type_constraint UNIQUE (left_type, right_type, left_label, right_label)

);

CREATE TABLE relationship
(
    id                      INTEGER NOT NULL PRIMARY KEY,
    left_id                 raw(16) NOT NULL REFERENCES item(uuid),
    type_id                 INTEGER NOT NULL REFERENCES relationship_type(id),
    right_id                raw(16) NOT NULL REFERENCES item(uuid),
    left_place              INTEGER,
    right_place             INTEGER,
    CONSTRAINT u_constraint UNIQUE (left_id, type_id, right_id)

);

CREATE INDEX entity_type_label_idx ON entity_type(label);
CREATE INDEX rl_ty_by_left_type_idx ON relationship_type(left_type);
CREATE INDEX rl_ty_by_right_type_idx ON relationship_type(right_type);
CREATE INDEX rl_ty_by_left_label_idx ON relationship_type(left_label);
CREATE INDEX rl_ty_by_right_label_idx ON relationship_type(right_label);
CREATE INDEX relationship_by_left_id_idx ON relationship(left_id);
CREATE INDEX relationship_by_right_id_idx ON relationship(right_id);
