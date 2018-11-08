--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE cris_rp_prop
  DROP CONSTRAINT cris_rp_prop_positiondef_typo_id_parent_id_key;

ALTER TABLE cris_rp_no_prop
  DROP CONSTRAINT cris_rp_no_prop_positiondef_typo_id_parent_id_key;

ALTER TABLE cris_pj_prop
  DROP CONSTRAINT cris_pj_prop_positiondef_typo_id_parent_id_key;

ALTER TABLE cris_pj_no_prop
  DROP CONSTRAINT cris_pj_no_prop_positiondef_typo_id_parent_id_key;

ALTER TABLE cris_ou_prop
  DROP CONSTRAINT cris_ou_prop_positiondef_typo_id_parent_id_key;

ALTER TABLE cris_ou_no_prop
  DROP CONSTRAINT cris_ou_no_prop_positiondef_typo_id_parent_id_key;

ALTER TABLE cris_do_prop
  DROP CONSTRAINT cris_do_prop_positiondef_typo_id_parent_id_key;

ALTER TABLE cris_do_no_prop
  DROP CONSTRAINT cris_do_no_prop_positiondef_typo_id_parent_id_key;
  
-- we need these constraints to be deferred to allow hibernate to sort INSERT/DELETE batch queries
ALTER TABLE cris_rp_prop
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;
  
ALTER TABLE cris_rp_no_prop
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;
  
ALTER TABLE cris_pj_prop
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;

ALTER TABLE cris_pj_no_prop
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;

ALTER TABLE cris_ou_prop
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;
  
ALTER TABLE cris_ou_no_prop
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;
  
ALTER TABLE cris_do_prop
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;
  
ALTER TABLE cris_do_no_prop
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;  