START TRANSACTION;

-- Export site
CREATE TEMPORARY TABLE ext_site (uuid uuid);
INSERT INTO ext_site SELECT * FROM site;

-- Create export table for dspaceobject
CREATE TEMPORARY TABLE ext_dspaceobject (uuid uuid);
INSERT INTO ext_dspaceobject SELECT uuid FROM site;
INSERT INTO ext_dspaceobject SELECT uuid FROM collection;
INSERT INTO ext_dspaceobject SELECT uuid FROM community;
INSERT INTO ext_dspaceobject SELECT uuid FROM epersongroup;

-- Create export table for Collection
CREATE TEMPORARY TABLE ext_collection (collection_id integer, uuid uuid, workflow_step_1 uuid, workflow_step_2 uuid, workflow_step_3 uuid, submitter uuid, template_item_id uuid, logo_bitstream_id uuid, admin uuid);
INSERT INTO ext_collection SELECT * FROM collection;

-- Create export table for Community
CREATE TEMPORARY TABLE ext_community (community_id integer, uuid uuid, admin uuid, logo_bitstream_id uuid);
INSERT INTO ext_community SELECT * FROM community;

-- Remove community and collection logos, since we do not want any bitstreams
UPDATE ext_community SET logo_bitstream_id = NULL WHERE logo_bitstream_id IS NOT NULL;
UPDATE ext_collection SET logo_bitstream_id = NULL WHERE logo_bitstream_id IS NOT NULL;

-- Export relations between communities and collections
CREATE TEMPORARY TABLE ext_community2collection (collection_id uuid, community_id uuid);
INSERT INTO ext_community2collection SELECT * FROM community2collection ;

-- Export relations between communities and communities
CREATE TEMPORARY TABLE ext_community2community (parent_comm_id uuid, child_comm_id uuid);
INSERT INTO ext_community2community SELECT * FROM community2community;

-- Export required groups
CREATE TEMPORARY TABLE ext_epersongroup (eperson_group_id integer, uuid uuid, permanent boolean, name character varying(250));
INSERT INTO ext_epersongroup SELECT * FROM epersongroup WHERE uuid IN (SELECT workflow_step_1 FROM ext_collection UNION SELECT workflow_step_2 FROM ext_collection UNION SELECT workflow_step_3 FROM ext_collection UNION SELECT submitter FROM ext_collection UNION SELECT admin FROM ext_collection UNION SELECT admin FROM ext_community);
INSERT INTO ext_epersongroup SELECT * FROM epersongroup WHERE name ILIKE 'anonymous';
INSERT INTO ext_epersongroup SELECT * FROM epersongroup WHERE name ILIKE 'administrator';

-- Set the administrator for the 2nd workflow step
CREATE TEMPORARY TABLE ext_group2group (parent_id uuid, child_id uuid);
CREATE TEMPORARY TABLE ext_group2groupcache (parent_id uuid, child_id uuid);
INSERT INTO ext_group2group SELECT c.workflow_step_1, a.uuid FROM ext_collection AS c JOIN ext_epersongroup AS a ON a.name ILIKE 'administrator' AND c.workflow_step_1 IS NOT NULL;
INSERT INTO ext_group2groupcache SELECT c.workflow_step_1, a.uuid FROM ext_collection AS c JOIN ext_epersongroup AS a ON a.name ILIKE 'administrator' AND c.workflow_step_1 IS NOT NULL;
INSERT INTO ext_group2group SELECT c.workflow_step_2, a.uuid FROM ext_collection AS c JOIN ext_epersongroup AS a ON a.name ILIKE 'administrator' AND c.workflow_step_2 IS NOT NULL;
INSERT INTO ext_group2groupcache SELECT c.workflow_step_2, a.uuid FROM ext_collection AS c JOIN ext_epersongroup AS a ON a.name ILIKE 'administrator' AND c.workflow_step_2 IS NOT NULL;
INSERT INTO ext_group2group SELECT c.workflow_step_3, a.uuid FROM ext_collection AS c JOIN ext_epersongroup AS a ON a.name ILIKE 'administrator' AND c.workflow_step_3 IS NOT NULL;
INSERT INTO ext_group2groupcache SELECT c.workflow_step_3, a.uuid FROM ext_collection AS c JOIN ext_epersongroup AS a ON a.name ILIKE 'administrator' AND c.workflow_step_3 IS NOT NULL;

-- Export handles
CREATE TEMPORARY TABLE ext_handle (handle_id integer, handle character varying(256), resource_type_id integer, resource_legacy_id integer, resource_id uuid);
INSERT INTO ext_handle SELECT * FROM handle WHERE resource_id IN (SELECT uuid FROM ext_community UNION SELECT uuid FROM ext_collection);

-- Metadata of the communities, collections and epersongroups
CREATE TEMPORARY TABLE ext_metadataschemaregistry (metadata_schema_id integer, namespace character varying(256), short_id character varying(32));
INSERT INTO ext_metadataschemaregistry SELECT * FROM metadataschemaregistry;
CREATE TABLE ext_metadatafieldregistry (metadata_field_id integer, metadata_schema_id integer, element character varying(64), qualifier character varying(64), scope_note text);
INSERT INTO ext_metadatafieldregistry SELECT * FROM metadatafieldregistry;
CREATE TEMPORARY TABLE ext_metadatavalue (metadata_value_id integer, metadata_field_id integer, text_value text, text_lang character varying(24), place integer, authority character varying(100), confidence integer, dspace_object_id uuid);
INSERT INTO ext_metadatavalue SELECT * FROM metadatavalue WHERE dspace_object_id IN (SELECT uuid FROM ext_dspaceobject);

-- Resource policies
CREATE TEMPORARY TABLE ext_resourcepolicy (
  policy_id integer,
  resource_type_id integer,
  resource_id integer,
  action_id integer,
  start_date date,
  end_date date,
  rpname character varying(30),
  rptype character varying(30),
  rpdescription text,
  eperson_id uuid,
  epersongroup_id uuid,
  dspace_object uuid);
INSERT INTO ext_resourcepolicy
  SELECT * FROM resourcepolicy
    WHERE epersongroup_id IN (SELECT uuid FROM ext_epersongroup)
          AND dspace_object IN (SELECT uuid FROM ext_dspaceobject);

-- Export tables
COPY ext_collection TO '/tmp/dspace-export/ext_collection.csv' DELIMITER ',' CSV;
COPY ext_community TO '/tmp/dspace-export/ext_community.csv' DELIMITER ',' CSV;
COPY ext_community2collection TO '/tmp/dspace-export/ext_community2collection.csv' DELIMITER ',' CSV;
COPY ext_community2community  TO '/tmp/dspace-export/ext_community2community.csv' DELIMITER ',' CSV;
COPY ext_dspaceobject TO '/tmp/dspace-export/ext_dspaceobject.csv' DELIMITER ',' CSV;
COPY ext_epersongroup         TO '/tmp/dspace-export/ext_epersongroup.csv' DELIMITER ',' CSV;
COPY ext_group2group          TO '/tmp/dspace-export/ext_group2group.csv' DELIMITER ',' CSV;
COPY ext_group2groupcache     TO '/tmp/dspace-export/ext_group2groupcache.csv' DELIMITER ',' CSV;
COPY ext_handle     TO '/tmp/dspace-export/ext_handle.csv' DELIMITER ',' CSV;
COPY ext_metadatavalue        TO '/tmp/dspace-export/ext_metadatavalue.csv' DELIMITER ',' CSV;
COPY ext_site       TO '/tmp/dspace-export/ext_site.csv' DELIMITER ',' CSV;
COPY ext_metadatafieldregistry TO '/tmp/dspace-export/ext_metadatafieldregistry.csv' DELIMITER ',' CSV;
COPY ext_metadataschemaregistry  TO '/tmp/dspace-export/ext_metadataschemaregistry.csv' DELIMITER ',' CSV;
COPY ext_resourcepolicy  TO '/tmp/dspace-export/ext_resourcepolicy.csv' DELIMITER ',' CSV;

ROLLBACK;
