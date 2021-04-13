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

DROP SEQUENCE IF EXISTS potentialmatches_seq;
DROP SEQUENCE IF EXISTS jdyna_widget_seq;
DROP SEQUENCE IF EXISTS jdyna_values_seq;
DROP SEQUENCE IF EXISTS jdyna_typonestedobject_seq;
DROP SEQUENCE IF EXISTS jdyna_tab_seq;
DROP SEQUENCE IF EXISTS jdyna_scopedef_seq;
DROP SEQUENCE IF EXISTS jdyna_prop_seq;
DROP SEQUENCE IF EXISTS jdyna_pdef_seq;
DROP SEQUENCE IF EXISTS jdyna_nestedobject_seq;
DROP SEQUENCE IF EXISTS jdyna_messagebox_seq;
DROP SEQUENCE IF EXISTS jdyna_containable_seq;
DROP SEQUENCE IF EXISTS jdyna_box_seq;
DROP SEQUENCE IF EXISTS imp_record_seq;
DROP SEQUENCE IF EXISTS imp_metadatavalue_seq;
DROP SEQUENCE IF EXISTS imp_bitstream_seq;
DROP SEQUENCE IF EXISTS imp_bitstream_metadatavalue_seq;
DROP SEQUENCE IF EXISTS dedup_reject_seq;
DROP SEQUENCE IF EXISTS cris_ws_user_seq;
DROP SEQUENCE IF EXISTS cris_ws_criteria_seq;
DROP SEQUENCE IF EXISTS cris_typodynaobj_seq;
DROP SEQUENCE IF EXISTS cris_subscription_seq;
DROP SEQUENCE IF EXISTS cris_statsubscription_seq;
DROP SEQUENCE IF EXISTS cris_rpage_seq;
DROP SEQUENCE IF EXISTS cris_relpref_seq;
DROP SEQUENCE IF EXISTS cris_project_seq;
DROP SEQUENCE IF EXISTS cris_ou_seq;
DROP SEQUENCE IF EXISTS cris_orcidqueue_seq;
DROP SEQUENCE IF EXISTS cris_orcidhistory_seq;
DROP SEQUENCE IF EXISTS cris_metrics_seq;
DROP SEQUENCE IF EXISTS cris_dynaobj_seq;
ALTER TABLE IF EXISTS potentialmatches RENAME TO old_potentialmatches;
ALTER TABLE IF EXISTS cris_orcid_queue RENAME TO old_cris_orcid_queue;
ALTER TABLE IF EXISTS cris_orcid_history RENAME TO old_cris_orcid_history;
ALTER TABLE IF EXISTS jdyna_widget_text RENAME TO old_jdyna_widget_text;
ALTER TABLE IF EXISTS jdyna_widget_number RENAME TO old_jdyna_widget_number;
ALTER TABLE IF EXISTS jdyna_widget_link RENAME TO old_jdyna_widget_link;
ALTER TABLE IF EXISTS jdyna_widget_date RENAME TO old_jdyna_widget_date;
ALTER TABLE IF EXISTS jdyna_widget_classification RENAME TO old_jdyna_widget_classification;
ALTER TABLE IF EXISTS jdyna_widget_checkradio RENAME TO old_jdyna_widget_checkradio;
ALTER TABLE IF EXISTS jdyna_widget_boolean RENAME TO old_jdyna_widget_boolean;
ALTER TABLE IF EXISTS jdyna_values RENAME TO old_jdyna_values;
ALTER TABLE IF EXISTS jdyna_scopedefinition RENAME TO old_jdyna_scopedefinition;
ALTER TABLE IF EXISTS jdyna_containable RENAME TO old_jdyna_containable;
ALTER TABLE IF EXISTS jdyna_box_message RENAME TO old_jdyna_box_message;
ALTER TABLE IF EXISTS imp_record_to_item RENAME TO old_imp_record_to_item;
ALTER TABLE IF EXISTS imp_record RENAME TO old_imp_record;
ALTER TABLE IF EXISTS imp_metadatavalue RENAME TO old_imp_metadatavalue;
ALTER TABLE IF EXISTS imp_bitstream_metadatavalue RENAME TO old_imp_bitstream_metadatavalue;
ALTER TABLE IF EXISTS imp_bitstream RENAME TO old_imp_bitstream;
ALTER TABLE IF EXISTS doi2item RENAME TO old_doi2item;
ALTER TABLE IF EXISTS dedup_reject RENAME TO old_dedup_reject;
ALTER TABLE IF EXISTS cris_metrics RENAME TO old_cris_metrics;
ALTER TABLE IF EXISTS cris_ws_user2crit RENAME TO old_cris_ws_user2crit;
ALTER TABLE IF EXISTS cris_ws_user RENAME TO old_cris_ws_user;
ALTER TABLE IF EXISTS cris_ws_criteria RENAME TO old_cris_ws_criteria;
ALTER TABLE IF EXISTS cris_wgroup RENAME TO old_cris_wgroup;
ALTER TABLE IF EXISTS cris_weperson RENAME TO old_cris_weperson;
ALTER TABLE IF EXISTS cris_subscription RENAME TO old_cris_subscription;
ALTER TABLE IF EXISTS cris_statsubscription RENAME TO old_cris_statsubscription;
ALTER TABLE IF EXISTS cris_rpage RENAME TO old_cris_rpage;
ALTER TABLE IF EXISTS cris_rp_wpointer RENAME TO old_cris_rp_wpointer;
ALTER TABLE IF EXISTS cris_rp_wfile RENAME TO old_cris_rp_wfile;
ALTER TABLE IF EXISTS cris_rp_tab2policysingle RENAME TO old_cris_rp_tab2policysingle;
ALTER TABLE IF EXISTS cris_rp_tab2policygroup RENAME TO old_cris_rp_tab2policygroup;
ALTER TABLE IF EXISTS cris_rp_tab2box RENAME TO old_cris_rp_tab2box;
ALTER TABLE IF EXISTS cris_rp_tab RENAME TO old_cris_rp_tab;
ALTER TABLE IF EXISTS cris_rp_prop RENAME TO old_cris_rp_prop;
ALTER TABLE IF EXISTS cris_rp_pdef RENAME TO old_cris_rp_pdef;
ALTER TABLE IF EXISTS cris_rp_no_tp2pdef RENAME TO old_cris_rp_no_tp2pdef;
ALTER TABLE IF EXISTS cris_rp_no_tp RENAME TO old_cris_rp_no_tp;
ALTER TABLE IF EXISTS cris_rp_no_prop RENAME TO old_cris_rp_no_prop;
ALTER TABLE IF EXISTS cris_rp_no_pdef RENAME TO old_cris_rp_no_pdef;
ALTER TABLE IF EXISTS cris_rp_no RENAME TO old_cris_rp_no;
ALTER TABLE IF EXISTS cris_rp_etab2policysingle RENAME TO old_cris_rp_etab2policysingle;
ALTER TABLE IF EXISTS cris_rp_etab2policygroup RENAME TO old_cris_rp_etab2policygroup;
ALTER TABLE IF EXISTS cris_rp_etab2box RENAME TO old_cris_rp_etab2box;
ALTER TABLE IF EXISTS cris_rp_etab RENAME TO old_cris_rp_etab;
ALTER TABLE IF EXISTS cris_rp_box2policysingle RENAME TO old_cris_rp_box2policysingle;
ALTER TABLE IF EXISTS cris_rp_box2policygroup RENAME TO old_cris_rp_box2policygroup;
ALTER TABLE IF EXISTS cris_rp_box2con RENAME TO old_cris_rp_box2con;
ALTER TABLE IF EXISTS cris_rp_box RENAME TO old_cris_rp_box;
ALTER TABLE IF EXISTS cris_relpref RENAME TO old_cris_relpref;
ALTER TABLE IF EXISTS cris_project RENAME TO old_cris_project;
ALTER TABLE IF EXISTS cris_pmc_record_pubmedids RENAME TO old_cris_pmc_record_pubmedids;
ALTER TABLE IF EXISTS cris_pmc_record_handles RENAME TO old_cris_pmc_record_handles;
ALTER TABLE IF EXISTS cris_pmc_record RENAME TO old_cris_pmc_record;
ALTER TABLE IF EXISTS cris_pmc_citation_itemids RENAME TO old_cris_pmc_citation_itemids;
ALTER TABLE IF EXISTS cris_pmc_citation2record RENAME TO old_cris_pmc_citation2record;
ALTER TABLE IF EXISTS cris_pmc_citation RENAME TO old_cris_pmc_citation;
ALTER TABLE IF EXISTS cris_pj_wpointer RENAME TO old_cris_pj_wpointer;
ALTER TABLE IF EXISTS cris_pj_wfile RENAME TO old_cris_pj_wfile;
ALTER TABLE IF EXISTS cris_pj_tab2policysingle RENAME TO old_cris_pj_tab2policysingle;
ALTER TABLE IF EXISTS cris_pj_tab2policygroup RENAME TO old_cris_pj_tab2policygroup;
ALTER TABLE IF EXISTS cris_pj_tab2box RENAME TO old_cris_pj_tab2box;
ALTER TABLE IF EXISTS cris_pj_tab RENAME TO old_cris_pj_tab;
ALTER TABLE IF EXISTS cris_pj_prop RENAME TO old_cris_pj_prop;
ALTER TABLE IF EXISTS cris_pj_pdef RENAME TO old_cris_pj_pdef;
ALTER TABLE IF EXISTS cris_pj_no_tp2pdef RENAME TO old_cris_pj_no_tp2pdef;
ALTER TABLE IF EXISTS cris_pj_no_tp RENAME TO old_cris_pj_no_tp;
ALTER TABLE IF EXISTS cris_pj_no_prop RENAME TO old_cris_pj_no_prop;
ALTER TABLE IF EXISTS cris_pj_no_pdef RENAME TO old_cris_pj_no_pdef;
ALTER TABLE IF EXISTS cris_pj_no RENAME TO old_cris_pj_no;
ALTER TABLE IF EXISTS cris_pj_etab2policysingle RENAME TO old_cris_pj_etab2policysingle;
ALTER TABLE IF EXISTS cris_pj_etab2policygroup RENAME TO old_cris_pj_etab2policygroup;
ALTER TABLE IF EXISTS cris_pj_etab2box RENAME TO old_cris_pj_etab2box;
ALTER TABLE IF EXISTS cris_pj_etab RENAME TO old_cris_pj_etab;
ALTER TABLE IF EXISTS cris_pj_box2policysingle RENAME TO old_cris_pj_box2policysingle;
ALTER TABLE IF EXISTS cris_pj_box2policygroup RENAME TO old_cris_pj_box2policygroup;
ALTER TABLE IF EXISTS cris_pj_box2con RENAME TO old_cris_pj_box2con;
ALTER TABLE IF EXISTS cris_pj_box RENAME TO old_cris_pj_box;
ALTER TABLE IF EXISTS cris_ou_wpointer RENAME TO old_cris_ou_wpointer;
ALTER TABLE IF EXISTS cris_ou_wfile RENAME TO old_cris_ou_wfile;
ALTER TABLE IF EXISTS cris_ou_tab2policysingle RENAME TO old_cris_ou_tab2policysingle;
ALTER TABLE IF EXISTS cris_ou_tab2policygroup RENAME TO old_cris_ou_tab2policygroup;
ALTER TABLE IF EXISTS cris_ou_tab2box RENAME TO old_cris_ou_tab2box;
ALTER TABLE IF EXISTS cris_ou_tab RENAME TO old_cris_ou_tab;
ALTER TABLE IF EXISTS cris_ou_prop RENAME TO old_cris_ou_prop;
ALTER TABLE IF EXISTS cris_ou_pdef RENAME TO old_cris_ou_pdef;
ALTER TABLE IF EXISTS cris_ou_no_tp2pdef RENAME TO old_cris_ou_no_tp2pdef;
ALTER TABLE IF EXISTS cris_ou_no_tp RENAME TO old_cris_ou_no_tp;
ALTER TABLE IF EXISTS cris_ou_no_prop RENAME TO old_cris_ou_no_prop;
ALTER TABLE IF EXISTS cris_ou_no_pdef RENAME TO old_cris_ou_no_pdef;
ALTER TABLE IF EXISTS cris_ou_no RENAME TO old_cris_ou_no;
ALTER TABLE IF EXISTS cris_ou_etab2policysingle RENAME TO old_cris_ou_etab2policysingle;
ALTER TABLE IF EXISTS cris_ou_etab2policygroup RENAME TO old_cris_ou_etab2policygroup;
ALTER TABLE IF EXISTS cris_ou_etab2box RENAME TO old_cris_ou_etab2box;
ALTER TABLE IF EXISTS cris_ou_etab RENAME TO old_cris_ou_etab;
ALTER TABLE IF EXISTS cris_ou_box2policysingle RENAME TO old_cris_ou_box2policysingle;
ALTER TABLE IF EXISTS cris_ou_box2policygroup RENAME TO old_cris_ou_box2policygroup;
ALTER TABLE IF EXISTS cris_ou_box2con RENAME TO old_cris_ou_box2con;
ALTER TABLE IF EXISTS cris_ou_box RENAME TO old_cris_ou_box;
ALTER TABLE IF EXISTS cris_orgunit RENAME TO old_cris_orgunit;
ALTER TABLE IF EXISTS cris_do_wpointer RENAME TO old_cris_do_wpointer;
ALTER TABLE IF EXISTS cris_do_wfile RENAME TO old_cris_do_wfile;
ALTER TABLE IF EXISTS cris_do_tp2pdef RENAME TO old_cris_do_tp2pdef;
ALTER TABLE IF EXISTS cris_do_tp2notp RENAME TO old_cris_do_tp2notp;
ALTER TABLE IF EXISTS cris_do_tp RENAME TO old_cris_do_tp;
ALTER TABLE IF EXISTS cris_do_tab2policysingle RENAME TO old_cris_do_tab2policysingle;
ALTER TABLE IF EXISTS cris_do_tab2policygroup RENAME TO old_cris_do_tab2policygroup;
ALTER TABLE IF EXISTS cris_do_tab2box RENAME TO old_cris_do_tab2box;
ALTER TABLE IF EXISTS cris_do_tab RENAME TO old_cris_do_tab;
ALTER TABLE IF EXISTS cris_do_prop RENAME TO old_cris_do_prop;
ALTER TABLE IF EXISTS cris_do_pdef RENAME TO old_cris_do_pdef;
ALTER TABLE IF EXISTS cris_do_no_tp2pdef RENAME TO old_cris_do_no_tp2pdef;
ALTER TABLE IF EXISTS cris_do_no_tp RENAME TO old_cris_do_no_tp;
ALTER TABLE IF EXISTS cris_do_no_prop RENAME TO old_cris_do_no_prop;
ALTER TABLE IF EXISTS cris_do_no_pdef RENAME TO old_cris_do_no_pdef;
ALTER TABLE IF EXISTS cris_do_no RENAME TO old_cris_do_no;
ALTER TABLE IF EXISTS cris_do_etab2policysingle RENAME TO old_cris_do_etab2policysingle;
ALTER TABLE IF EXISTS cris_do_etab2policygroup RENAME TO old_cris_do_etab2policygroup;
ALTER TABLE IF EXISTS cris_do_etab2box RENAME TO old_cris_do_etab2box;
ALTER TABLE IF EXISTS cris_do_etab RENAME TO old_cris_do_etab;
ALTER TABLE IF EXISTS cris_do_box2policysingle RENAME TO old_cris_do_box2policysingle;
ALTER TABLE IF EXISTS cris_do_box2policygroup RENAME TO old_cris_do_box2policygroup;
ALTER TABLE IF EXISTS cris_do_box2con RENAME TO old_cris_do_box2con;
ALTER TABLE IF EXISTS cris_do_box RENAME TO old_cris_do_box;
ALTER TABLE IF EXISTS cris_do RENAME TO old_cris_do;

DROP INDEX IF EXISTS imp_mv_idx_impid;
DROP INDEX IF EXISTS imp_bit_idx_impid;
DROP INDEX IF EXISTS imp_bitstream_mv_idx_impid;
DROP INDEX IF EXISTS metric_bid_idx;
DROP INDEX IF EXISTS metric_resourceuid_idx;
DROP INDEX IF EXISTS metrics_last_idx;
DROP INDEX IF EXISTS metrics_uuid_idx;
ALTER TABLE IF EXISTS old_cris_metrics DROP CONSTRAINT cris_metrics_pkey;

delete from group2group where (parent_id, child_id, id) in ( select parent_id, child_id, max(id) from group2group  group by ( parent_id, child_id ) having count (*)> 1 );
