--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------------------------
-- DS-3431 Workflow system is vulnerable to unauthorized manipulations --
-------------------------------------------------------------------------

-----------------------------------------------------------------------
-- grant claiming permissions to all workflow step groups (step 1-3) --
-----------------------------------------------------------------------
INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, epersongroup_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '3' AS resource_type_id,
    '5' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    workflow_step_1 AS epersongroup_id,
    uuid AS dspace_object
  FROM collection
  WHERE workflow_step_1 IS NOT NULL
    AND NOT EXISTS (
      SELECT 1 FROM resourcepolicy WHERE resource_type_id = 3 AND action_id = 5 AND epersongroup_id = workflow_step_1 and dspace_object = uuid
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, epersongroup_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '3' AS resource_type_id,
    '6' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    workflow_step_2 AS epersongroup_id,
    uuid AS dspace_object
  FROM collection
  WHERE workflow_step_2 IS NOT NULL
    AND NOT EXISTS (
      SELECT 1 FROM resourcepolicy WHERE resource_type_id = 3 AND action_id = 6 AND epersongroup_id = workflow_step_2 and dspace_object = uuid
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, epersongroup_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '3' AS resource_type_id,
    '7' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    workflow_step_3 AS epersongroup_id,
    uuid AS dspace_object
  FROM collection
  WHERE workflow_step_3 IS NOT NULL
    AND NOT EXISTS (
      SELECT 1 FROM resourcepolicy WHERE resource_type_id = 3 AND action_id = 7 AND epersongroup_id = workflow_step_3 and dspace_object = uuid
    );

-----------------------------------------------------------------------
-- grant add permissions to all workflow step groups (step 1-3) --
-----------------------------------------------------------------------
INSERT INTO resourcepolicy
(policy_id, resource_type_id, action_id, rptype, epersongroup_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '3' AS resource_type_id,
    '3' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    workflow_step_1 AS epersongroup_id,
    uuid AS dspace_object
  FROM collection
  WHERE workflow_step_1 IS NOT NULL
        AND NOT EXISTS (
      SELECT 1 FROM resourcepolicy WHERE resource_type_id = 3 AND action_id = 3 AND epersongroup_id = workflow_step_1 and dspace_object = uuid
  );

INSERT INTO resourcepolicy
(policy_id, resource_type_id, action_id, rptype, epersongroup_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '3' AS resource_type_id,
    '3' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    workflow_step_2 AS epersongroup_id,
    uuid AS dspace_object
  FROM collection
  WHERE workflow_step_2 IS NOT NULL
        AND NOT EXISTS (
      SELECT 1 FROM resourcepolicy WHERE resource_type_id = 3 AND action_id = 3 AND epersongroup_id = workflow_step_2 and dspace_object = uuid
  );

INSERT INTO resourcepolicy
(policy_id, resource_type_id, action_id, rptype, epersongroup_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '3' AS resource_type_id,
    '3' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    workflow_step_3 AS epersongroup_id,
    uuid AS dspace_object
  FROM collection
  WHERE workflow_step_3 IS NOT NULL
        AND NOT EXISTS (
      SELECT 1 FROM resourcepolicy WHERE resource_type_id = 3 AND action_id = 3 AND epersongroup_id = workflow_step_3 and dspace_object = uuid
  );

----------------------------------------------------------------------------------
-- grant read/write/delete/add/remove permission on workflow items to reviewers --
----------------------------------------------------------------------------------
INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '2' AS resource_type_id,
    '0' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    owner AS eperson_id,
    item_id AS dspace_object
  FROM workflowitem
  WHERE
    owner IS NOT NULL
    AND (state = 2 OR state = 4 OR state = 6)
    AND NOT EXISTS (
      SELECT 1 FROM resourcepolicy WHERE resource_type_id = 2 AND action_id = 0 AND eperson_id = owner AND dspace_object = item_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '2' AS resource_type_id,
    '1' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    owner AS eperson_id,
    item_id AS dspace_object
  FROM workflowitem
  WHERE
    owner IS NOT NULL
    AND (state = 2 OR state = 4 OR state = 6)
    AND NOT EXISTS (
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 2 AND action_id = 1 AND eperson_id = owner AND dspace_object = item_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '2' AS resource_type_id,
    '2' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    owner AS eperson_id,
    item_id AS dspace_object
  FROM workflowitem
  WHERE
    owner IS NOT NULL
    AND (state = 2 OR state = 4 OR state = 6)
    AND NOT EXISTS (
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 2 AND action_id = 2 AND eperson_id = owner AND dspace_object = item_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '2' AS resource_type_id,
    '3' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    owner AS eperson_id,
    item_id AS dspace_object
  FROM workflowitem
  WHERE
    owner IS NOT NULL
    AND (state = 2 OR state = 4 OR state = 6)
    AND NOT EXISTS (
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 2 AND action_id = 3 AND eperson_id = owner AND dspace_object = item_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '2' AS resource_type_id,
    '4' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    owner AS eperson_id,
    item_id AS dspace_object
  FROM workflowitem
  WHERE
    owner IS NOT NULL
    AND (state = 2 OR state = 4 OR state = 6)
    AND NOT EXISTS (
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 2 AND action_id = 4 AND eperson_id = owner AND dspace_object = item_id
    );

-----------------------------------------------------------------------------------
-- grant read/write/delete/add/remove permission on Bundle ORIGINAL to reviewers --
-----------------------------------------------------------------------------------
INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '1' AS resource_type_id,
    '0' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    wfi.owner AS eperson_id,
    i2b.bundle_id AS dspace_object
  FROM workflowitem AS wfi
  JOIN item2bundle AS i2b
  ON i2b.item_id = wfi.item_id
  JOIN metadatavalue AS mv
  ON mv.dspace_object_id = i2b.bundle_id
  JOIN metadatafieldregistry as mfr
  ON mv.metadata_field_id = mfr.metadata_field_id
  JOIN metadataschemaregistry as msr
  ON mfr.metadata_schema_id = msr.metadata_schema_id
  WHERE
    msr.namespace = 'http://dublincore.org/documents/dcmi-terms/'
    AND mfr.element = 'title'
    AND mfr.qualifier IS NULL
    AND mv.text_value = 'ORIGINAL'
    AND wfi.owner IS NOT NULL
    AND (wfi.state = 2 OR wfi.state = 4 OR wfi.state = 6)
    AND NOT EXISTS(
      SELECT 1 FROM resourcepolicy WHERE resource_type_id = 1 AND action_id = 0 AND resourcepolicy.eperson_id = owner AND resourcepolicy.dspace_object = i2b.bundle_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '1' AS resource_type_id,
    '1' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    wfi.owner AS eperson_id,
    i2b.bundle_id AS dspace_object
  FROM workflowitem AS wfi
  JOIN item2bundle AS i2b
  ON i2b.item_id = wfi.item_id
  JOIN metadatavalue AS mv
  ON mv.dspace_object_id = i2b.bundle_id
  JOIN metadatafieldregistry as mfr
  ON mv.metadata_field_id = mfr.metadata_field_id
  JOIN metadataschemaregistry as msr
  ON mfr.metadata_schema_id = msr.metadata_schema_id
  WHERE
    msr.namespace = 'http://dublincore.org/documents/dcmi-terms/'
    AND mfr.element = 'title'
    AND mfr.qualifier IS NULL
    AND mv.text_value = 'ORIGINAL'
    AND wfi.owner IS NOT NULL
    AND (wfi.state = 2 OR wfi.state = 4 OR wfi.state = 6)
    AND NOT EXISTS(
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 1 AND action_id = 1 AND resourcepolicy.eperson_id = owner AND resourcepolicy.dspace_object = i2b.bundle_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '1' AS resource_type_id,
    '2' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    wfi.owner AS eperson_id,
    i2b.bundle_id AS dspace_object
  FROM workflowitem AS wfi
  JOIN item2bundle AS i2b
  ON i2b.item_id = wfi.item_id
  JOIN metadatavalue AS mv
  ON mv.dspace_object_id = i2b.bundle_id
  JOIN metadatafieldregistry as mfr
  ON mv.metadata_field_id = mfr.metadata_field_id
  JOIN metadataschemaregistry as msr
  ON mfr.metadata_schema_id = msr.metadata_schema_id
  WHERE
    msr.namespace = 'http://dublincore.org/documents/dcmi-terms/'
    AND mfr.element = 'title'
    AND mfr.qualifier IS NULL
    AND mv.text_value = 'ORIGINAL'
    AND wfi.owner IS NOT NULL
    AND (wfi.state = 2 OR wfi.state = 4 OR wfi.state = 6)
    AND NOT EXISTS(
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 1 AND action_id = 2 AND resourcepolicy.eperson_id = owner AND resourcepolicy.dspace_object = i2b.bundle_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '1' AS resource_type_id,
    '3' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    wfi.owner AS eperson_id,
    i2b.bundle_id AS dspace_object
  FROM workflowitem AS wfi
  JOIN item2bundle AS i2b
  ON i2b.item_id = wfi.item_id
  JOIN metadatavalue AS mv
  ON mv.dspace_object_id = i2b.bundle_id
  JOIN metadatafieldregistry as mfr
  ON mv.metadata_field_id = mfr.metadata_field_id
  JOIN metadataschemaregistry as msr
  ON mfr.metadata_schema_id = msr.metadata_schema_id
  WHERE
    msr.namespace = 'http://dublincore.org/documents/dcmi-terms/'
    AND mfr.element = 'title'
    AND mfr.qualifier IS NULL
    AND mv.text_value = 'ORIGINAL'
    AND wfi.owner IS NOT NULL
    AND (wfi.state = 2 OR wfi.state = 4 OR wfi.state = 6)
    AND NOT EXISTS(
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 1 AND action_id = 3 AND resourcepolicy.eperson_id = owner AND resourcepolicy.dspace_object = i2b.bundle_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '1' AS resource_type_id,
    '4' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    wfi.owner AS eperson_id,
    i2b.bundle_id AS dspace_object
  FROM workflowitem AS wfi
  JOIN item2bundle AS i2b
  ON i2b.item_id = wfi.item_id
  JOIN metadatavalue AS mv
  ON mv.dspace_object_id = i2b.bundle_id
  JOIN metadatafieldregistry as mfr
  ON mv.metadata_field_id = mfr.metadata_field_id
  JOIN metadataschemaregistry as msr
  ON mfr.metadata_schema_id = msr.metadata_schema_id
  WHERE
    msr.namespace = 'http://dublincore.org/documents/dcmi-terms/'
    AND mfr.element = 'title'
    AND mfr.qualifier IS NULL
    AND mv.text_value = 'ORIGINAL'
    AND wfi.owner IS NOT NULL
    AND (wfi.state = 2 OR wfi.state = 4 OR wfi.state = 6)
    AND NOT EXISTS(
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 1 AND action_id = 4 AND resourcepolicy.eperson_id = owner AND resourcepolicy.dspace_object = i2b.bundle_id
    );


-------------------------------------------------------------------------------
-- grant read/write/delete/add/remove permission on all Bitstreams of Bundle --
-- ORIGINAL to reviewers                                                     --
-------------------------------------------------------------------------------
INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '0' AS resource_type_id,
    '0' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    wfi.owner AS eperson_id,
    b2b.bitstream_id AS dspace_object
  FROM workflowitem AS wfi
  JOIN item2bundle AS i2b
  ON i2b.item_id = wfi.item_id
  JOIN bundle2bitstream AS b2b
  ON b2b.bundle_id = i2b.bundle_id
  JOIN metadatavalue AS mv
  ON mv.dspace_object_id = i2b.bundle_id
  JOIN metadatafieldregistry as mfr
  ON mv.metadata_field_id = mfr.metadata_field_id
  JOIN metadataschemaregistry as msr
  ON mfr.metadata_schema_id = msr.metadata_schema_id
  WHERE
    msr.namespace = 'http://dublincore.org/documents/dcmi-terms/'
    AND mfr.element = 'title'
    AND mfr.qualifier IS NULL
    AND mv.text_value = 'ORIGINAL'
    AND wfi.owner IS NOT NULL
    AND (wfi.state = 2 OR wfi.state = 4 OR wfi.state = 6)
    AND NOT EXISTS(
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 0 AND action_id = 0 AND resourcepolicy.eperson_id = owner AND resourcepolicy.dspace_object = b2b.bitstream_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '0' AS resource_type_id,
    '1' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    wfi.owner AS eperson_id,
    b2b.bitstream_id AS dspace_object
  FROM workflowitem AS wfi
  JOIN item2bundle AS i2b
  ON i2b.item_id = wfi.item_id
  JOIN bundle2bitstream AS b2b
  ON b2b.bundle_id = i2b.bundle_id
  JOIN metadatavalue AS mv
  ON mv.dspace_object_id = i2b.bundle_id
  JOIN metadatafieldregistry as mfr
  ON mv.metadata_field_id = mfr.metadata_field_id
  JOIN metadataschemaregistry as msr
  ON mfr.metadata_schema_id = msr.metadata_schema_id
  WHERE
    msr.namespace = 'http://dublincore.org/documents/dcmi-terms/'
    AND mfr.element = 'title'
    AND mfr.qualifier IS NULL
    AND mv.text_value = 'ORIGINAL'
    AND wfi.owner IS NOT NULL
    AND (wfi.state = 2 OR wfi.state = 4 OR wfi.state = 6)
    AND NOT EXISTS(
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 0 AND action_id = 1 AND resourcepolicy.eperson_id = owner AND resourcepolicy.dspace_object = b2b.bitstream_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '0' AS resource_type_id,
    '2' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    wfi.owner AS eperson_id,
    b2b.bitstream_id AS dspace_object
  FROM workflowitem AS wfi
  JOIN item2bundle AS i2b
  ON i2b.item_id = wfi.item_id
  JOIN bundle2bitstream AS b2b
  ON b2b.bundle_id = i2b.bundle_id
  JOIN metadatavalue AS mv
  ON mv.dspace_object_id = i2b.bundle_id
  JOIN metadatafieldregistry as mfr
  ON mv.metadata_field_id = mfr.metadata_field_id
  JOIN metadataschemaregistry as msr
  ON mfr.metadata_schema_id = msr.metadata_schema_id
  WHERE
    msr.namespace = 'http://dublincore.org/documents/dcmi-terms/'
    AND mfr.element = 'title'
    AND mfr.qualifier IS NULL
    AND mv.text_value = 'ORIGINAL'
    AND wfi.owner IS NOT NULL
    AND (wfi.state = 2 OR wfi.state = 4 OR wfi.state = 6)
    AND NOT EXISTS(
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 0 AND action_id = 2 AND resourcepolicy.eperson_id = owner AND resourcepolicy.dspace_object = b2b.bitstream_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '0' AS resource_type_id,
    '3' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    wfi.owner AS eperson_id,
    b2b.bitstream_id AS dspace_object
  FROM workflowitem AS wfi
  JOIN item2bundle AS i2b
  ON i2b.item_id = wfi.item_id
  JOIN bundle2bitstream AS b2b
  ON b2b.bundle_id = i2b.bundle_id
  JOIN metadatavalue AS mv
  ON mv.dspace_object_id = i2b.bundle_id
  JOIN metadatafieldregistry as mfr
  ON mv.metadata_field_id = mfr.metadata_field_id
  JOIN metadataschemaregistry as msr
  ON mfr.metadata_schema_id = msr.metadata_schema_id
  WHERE
    msr.namespace = 'http://dublincore.org/documents/dcmi-terms/'
    AND mfr.element = 'title'
    AND mfr.qualifier IS NULL
    AND mv.text_value = 'ORIGINAL'
    AND wfi.owner IS NOT NULL
    AND (wfi.state = 2 OR wfi.state = 4 OR wfi.state = 6)
    AND NOT EXISTS(
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 0 AND action_id = 3 AND resourcepolicy.eperson_id = owner AND resourcepolicy.dspace_object = b2b.bitstream_id
    );

INSERT INTO resourcepolicy
  (policy_id, resource_type_id, action_id, rptype, eperson_id, dspace_object)
  SELECT
    nextval('resourcepolicy_seq') AS policy_id,
    '0' AS resource_type_id,
    '4' AS action_id,
    'TYPE_WORKFLOW' AS rptype,
    wfi.owner AS eperson_id,
    b2b.bitstream_id AS dspace_object
  FROM workflowitem AS wfi
  JOIN item2bundle AS i2b
  ON i2b.item_id = wfi.item_id
  JOIN bundle2bitstream AS b2b
  ON b2b.bundle_id = i2b.bundle_id
  JOIN metadatavalue AS mv
  ON mv.dspace_object_id = i2b.bundle_id
  JOIN metadatafieldregistry as mfr
  ON mv.metadata_field_id = mfr.metadata_field_id
  JOIN metadataschemaregistry as msr
  ON mfr.metadata_schema_id = msr.metadata_schema_id
  WHERE
    msr.namespace = 'http://dublincore.org/documents/dcmi-terms/'
    AND mfr.element = 'title'
    AND mfr.qualifier IS NULL
    AND mv.text_value = 'ORIGINAL'
    AND wfi.owner IS NOT NULL
    AND (wfi.state = 2 OR wfi.state = 4 OR wfi.state = 6)
    AND NOT EXISTS(
        SELECT 1 FROM resourcepolicy WHERE resource_type_id = 0 AND action_id = 4 AND resourcepolicy.eperson_id = owner AND resourcepolicy.dspace_object = b2b.bitstream_id
    );
