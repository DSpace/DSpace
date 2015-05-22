--   The contents of this file are subject to the license and copyright
--   detailed in the LICENSE and NOTICE files at the root of the source
--   tree and available online at
--   
--   https://github.com/CILEA/dspace-cris/wiki/License
--
-- SQL commands to populate CRIS with default metadata definitions
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.

/*

Target Server Type    : PGSQL
Target Server Version : 90003
File Encoding         : 65001

Date: 2012-11-22 14:55:29
*/

-- ----------------------------
-- Records of cris_ou_box
-- ----------------------------
INSERT INTO "public"."cris_ou_box" VALUES ('300', 'f', null, '1000',  'details', 'Details', 'f', '3');
INSERT INTO "public"."cris_ou_box" VALUES ('451', 'f', 'dspaceitems', '0', 'publication', 'OrgUnit''s Publications', 'f', '3');
INSERT INTO "public"."cris_ou_box" VALUES ('452', 'f', 'dspaceitems', '0', 'rppublication', 'OrgUnit''s Researchers publications', 'f', '3');
INSERT INTO "public"."cris_ou_box" VALUES ('453', 'f', 'persons', '0', 'rp', 'OrgUnit''s Researchers', 'f', '3');
INSERT INTO "public"."cris_ou_box" VALUES ('454', 'f', 'projects', '0', 'projects', 'OrgUnit funded Projects', 'f', '3');
INSERT INTO "public"."cris_ou_box" VALUES ('750', 'f', null, '10000', 'orgcard', 'Card', 't', '3');
INSERT INTO "public"."cris_ou_box" VALUES ('800', 't', null, '0', 'description', 'Description', 'f', '3');
INSERT INTO "public"."cris_ou_box" VALUES ('801', 'f', 'organizations', '0', 'organizations', 'SubOrgUnits', 'f', '3');
INSERT INTO "public"."cris_ou_box" VALUES ('850', 'f', 'projects', '0', 'rpprojects', 'OrgUnit''s Researchers projects', 'f', '3');



-- ----------------------------
-- Records of cris_ou_edittab
-- ----------------------------
INSERT INTO "public"."cris_ou_etab" VALUES ('451', null, 'f', null, '0', 'editinformation', 'OrgUnit''s Metadata', '1', null);


-- ----------------------------
-- Records of cris_ou_edittab2box
-- ----------------------------
INSERT INTO "public"."cris_ou_etab2box" VALUES ('451', '300');
INSERT INTO "public"."cris_ou_etab2box" VALUES ('451', '800');

-- ----------------------------
-- Records of cris_ou_pdef
-- ----------------------------
INSERT INTO "public"."cris_ou_pdef" VALUES ('1000', '1', 'f', '70', 'em', 'em',  '0', null, 'Director', '0', 'em', 't', 'f', 'f', '800', 'f', 'director', 'f', 'f', '1000');
INSERT INTO "public"."cris_ou_pdef" VALUES ('1050', '3', 'f', '70', 'em', 'em',  '3', null, 'Organization name', '0', 'em', 't', 'f', 'f', '999', 'f', 'name', 'f', 'f', '1050');
INSERT INTO "public"."cris_ou_pdef" VALUES ('1651', '1', 'f', '70', 'em', 'em',  '0', null, 'Parent OrgUnit', '0', 'em', 'f', 'f', 'f', '700', 'f', 'parentorgunit', 'f', 'f', '1651');
INSERT INTO "public"."cris_ou_pdef" VALUES ('1652', '1', 'f', '0', 'em', 'em',  '0', null, 'Scientifics Board', '0', 'em', 'f', 'f', 'f', '10', 't', 'boards', 'f', 'f', '1652');
INSERT INTO "public"."cris_ou_pdef" VALUES ('1653', '1', 'f', '0', 'em', 'em',  '3', null, 'Established', '0', 'em', 'f', 't', 'f', '40', 'f', 'date', 'f', 'f', '1653');
INSERT INTO "public"."cris_ou_pdef" VALUES ('1700', '1', 'f', '0', 'em', 'em', '0', null, null, '0', 'em', 'f', 'f', 'f', '0', 'f', 'description', 'f', 'f', '1700');
INSERT INTO "public"."cris_ou_pdef" VALUES ('1701', '1', 'f', '15', 'em', 'em',  '13', null, null, '0', 'em', 'f', 'f', 'f', '1000', 'f', 'logo', 'f', 'f', '1701');

-- ----------------------------
-- Records of cris_ou_tab
-- ----------------------------
INSERT INTO "public"."cris_ou_tab" VALUES ('304', null, 'f', null, '0', 'informations', 'Informations', '3');
INSERT INTO "public"."cris_ou_tab" VALUES ('400', null, 'f', null, '0', 'publications', 'Publications', '3');
INSERT INTO "public"."cris_ou_tab" VALUES ('402', null, 'f', null, '0', 'projects', 'Projects', '3');
INSERT INTO "public"."cris_ou_tab" VALUES ('404', null, 'f', null, '0', 'people', 'Peoples', '3');

-- ----------------------------
-- Records of cris_ou_tab2box
-- ----------------------------
INSERT INTO "public"."cris_ou_tab2box" VALUES ('304', '300');
INSERT INTO "public"."cris_ou_tab2box" VALUES ('304', '800');
INSERT INTO "public"."cris_ou_tab2box" VALUES ('304', '801');
INSERT INTO "public"."cris_ou_tab2box" VALUES ('400', '451');
INSERT INTO "public"."cris_ou_tab2box" VALUES ('400', '452');
INSERT INTO "public"."cris_ou_tab2box" VALUES ('400', '750');
INSERT INTO "public"."cris_ou_tab2box" VALUES ('402', '454');
INSERT INTO "public"."cris_ou_tab2box" VALUES ('402', '750');
INSERT INTO "public"."cris_ou_tab2box" VALUES ('402', '850');
INSERT INTO "public"."cris_ou_tab2box" VALUES ('404', '453');
INSERT INTO "public"."cris_ou_tab2box" VALUES ('404', '750');

-- ----------------------------
-- Records of cris_ou_wfile
-- ----------------------------
INSERT INTO "public"."cris_ou_wfile" VALUES ('1701', 'Upload logo', null, 't', '40', false);

-- ----------------------------
-- Records of cris_pj_box
-- ----------------------------
INSERT INTO "public"."cris_pj_box" VALUES ('100', 'f', null, '999', 'grants', 'Grants', 'f', '3');
INSERT INTO "public"."cris_pj_box" VALUES ('450', 'f', 'dspaceitems', '10', 'dspaceitems', 'Publications', 'f', '3');
INSERT INTO "public"."cris_pj_box" VALUES ('650', 'f', null, '1000', 'primarydata', 'Primary Data', 'f', '3');
INSERT INTO "public"."cris_pj_box" VALUES ('700', 'f', null, '1001', 'projectcard', 'Project Card', 'f', '3');
INSERT INTO "public"."cris_pj_box" VALUES ('701', 'f', null, '10', 'description', 'Description', 'f', '3');


-- ----------------------------
-- Records of cris_pj_edittab
-- ----------------------------
INSERT INTO "public"."cris_pj_etab" VALUES ('103', null, 'f', null, '0', 'editinformation', 'Information', '1', null);
INSERT INTO "public"."cris_pj_etab" VALUES ('352', null, 'f', null, '10', 'editgrants', 'Grants', '1', null);


-- ----------------------------
-- Records of cris_pj_edittab2box
-- ----------------------------
INSERT INTO "public"."cris_pj_etab2box" VALUES ('103', '650');
INSERT INTO "public"."cris_pj_etab2box" VALUES ('103', '701');
INSERT INTO "public"."cris_pj_etab2box" VALUES ('352', '100');

-- ----------------------------
-- Records of cris_pj_no_pdef
-- ----------------------------
INSERT INTO "public"."cris_pj_no_pdef" VALUES ('1554', '1', 'f', '0', 'em', 'em',  '0', null, 'Grant type', '0', 'em', 'f', 'f', 'f', '100', 'f', 'granttype', 'f', 'f', '1554');
INSERT INTO "public"."cris_pj_no_pdef" VALUES ('1555', '1', 'f', '0', 'em', 'em',  '0', null, 'Amount', '0', 'em', 'f', 'f', 'f', '0', 'f', 'grantamount', 'f', 'f', '1555');
INSERT INTO "public"."cris_pj_no_pdef" VALUES ('1556', '1', 'f', '0', 'em', 'em',  '0', null, 'Duration', '0', 'em', 'f', 'f', 'f', '0', 'f', 'grantduration', 'f', 'f', '1556');
INSERT INTO "public"."cris_pj_no_pdef" VALUES ('1557', '1', 'f', '0', 'em', 'em',  '0', null, 'Agencies', '0', 'em', 'f', 'f', 'f', '0', 'f', 'agencies', 'f', 'f', '1557');

-- ----------------------------
-- Records of cris_pj_no_typo
-- ----------------------------
INSERT INTO "public"."cris_pj_no_tp" VALUES ('24', 'Grant', 'grant', '1', null, 'f', 'f', 't', '0', 't');

-- ----------------------------
-- Records of cris_pj_no_typo2pdef
-- ----------------------------
INSERT INTO "public"."cris_pj_no_tp2pdef" VALUES ('24', '1554');
INSERT INTO "public"."cris_pj_no_tp2pdef" VALUES ('24', '1555');
INSERT INTO "public"."cris_pj_no_tp2pdef" VALUES ('24', '1556');
INSERT INTO "public"."cris_pj_no_tp2pdef" VALUES ('24', '1557');

-- ----------------------------
-- Records of cris_pj_pdef
-- ----------------------------
INSERT INTO "public"."cris_pj_pdef" VALUES ('100', '1', 'f', '70', 'em', 'em', '0', null, 'Project title', '11','em',  't', 'f', 'f', '999', 'f', 'title', 'f', 'f', '100');
INSERT INTO "public"."cris_pj_pdef" VALUES ('101', '1', 'f', '0', 'em', 'em', '0', null, 'Expected Completion', '0','em',  'f', 't', 'f', '5', 'f', 'expdate', 'f', 'f', '101');
INSERT INTO "public"."cris_pj_pdef" VALUES ('950', '1', 'f', '70', 'em', 'em', '3', null, 'Project Coordinator', '0','em',  't', 'f', 'f', '800', 'f', 'principalinvestigator', 'f', 'f', '950');
INSERT INTO "public"."cris_pj_pdef" VALUES ('951', '1', 'f', '0', 'em', 'em', '0', null, 'Co-Investigator(s)', '0','em',  'f', 'f', 'f', '0', 't', 'coinvestigators', 'f', 'f', '951');
INSERT INTO "public"."cris_pj_pdef" VALUES ('1550', '1', 'f', '1', 'em', 'em', '0', null, 'Start date', '0','em',  'f', 'f', 'f', '10', 'f', 'startdate', 'f', 'f', '1550');
INSERT INTO "public"."cris_pj_pdef" VALUES ('1551', '1', 'f', '15', 'em', 'em', '13', null, null, '0','em',  'f', 'f', 'f', '1000', 'f', 'logo', 'f', 'f', '1551');
INSERT INTO "public"."cris_pj_pdef" VALUES ('1552', '1', 'f', '0', 'em', 'em', '3', null, 'Status', '10','em',  'f', 'f', 'f', '20', 'f', 'status', 'f', 'f', '1552');
INSERT INTO "public"."cris_pj_pdef" VALUES ('1553', '1', 'f', '70', 'em', 'em', '0', null, 'Code', '11','em',  'f', 'f', 'f', '900', 'f', 'code', 'f', 'f', '1553');
INSERT INTO "public"."cris_pj_pdef" VALUES ('1600', '1', 'f', '0', 'em', 'em', '0', null, 'Abstract', '0','em',  'f', 't', 'f', '0', 'f', 'abstract', 'f', 'f', '1600');
INSERT INTO "public"."cris_pj_pdef" VALUES ('1601', '1', 'f', '0', 'em', 'em', '0', null, 'Keyword(s)', '0','em',  'f', 'f', 'f', '0', 't', 'keywords', 'f', 'f', '1601');

-- ----------------------------
-- Records of cris_pj_tab
-- ----------------------------
INSERT INTO "public"."cris_pj_tab" VALUES ('102', null, 'f', null, '10', 'grants', 'Grants', '3');
INSERT INTO "public"."cris_pj_tab" VALUES ('350', null, 'f', null, '0', 'informations', 'Informations', '3');

-- ----------------------------
-- Records of cris_pj_tab2box
-- ----------------------------
INSERT INTO "public"."cris_pj_tab2box" VALUES ('102', '100');
INSERT INTO "public"."cris_pj_tab2box" VALUES ('102', '450');
INSERT INTO "public"."cris_pj_tab2box" VALUES ('102', '700');
INSERT INTO "public"."cris_pj_tab2box" VALUES ('350', '650');
INSERT INTO "public"."cris_pj_tab2box" VALUES ('350', '701');

-- ----------------------------
-- Records of cris_pj_wfile
-- ----------------------------
INSERT INTO "public"."cris_pj_wfile" VALUES ('1551', 'Insert logo', null, 't', '40', false);
-- ----------------------------
-- Records of cris_rp_box
-- ----------------------------
INSERT INTO "public"."cris_rp_box" VALUES ('50', 'f', null, '1000', 'researcherprofile', 'Profile', 'f', '3');
INSERT INTO "public"."cris_rp_box" VALUES ('200', 'f', null, '10', 'qualifications', 'Professional Qualifications', 'f', '3');
INSERT INTO "public"."cris_rp_box" VALUES ('250', 'f', null, '10', 'media', 'Media Contact Directory', 'f', '3');
INSERT INTO "public"."cris_rp_box" VALUES ('350', 'f', 'dspaceitems', '0', 'dspaceitems', 'Publications', 'f', '3');
INSERT INTO "public"."cris_rp_box" VALUES ('400', 'f', 'projects', '0', 'projects', 'Projects', 'f', '3');
INSERT INTO "public"."cris_rp_box" VALUES ('550', 'f', null, '1000', 'namecard', 'Name Card', 't', '3');


-- ----------------------------
-- Records of cris_rp_edittab
-- ----------------------------
INSERT INTO "public"."cris_rp_etab" VALUES ('101', null, 'f', null, '0', 'editinformation', 'Edit Personal Information', '2', null);
INSERT INTO "public"."cris_rp_etab" VALUES ('251', null, 'f', null, '0', 'editotherinfo', 'Edit Other', '1', null);


-- ----------------------------
-- Records of cris_rp_edittab2box
-- ----------------------------
INSERT INTO "public"."cris_rp_etab2box" VALUES ('101', '50');
INSERT INTO "public"."cris_rp_etab2box" VALUES ('251', '200');
INSERT INTO "public"."cris_rp_etab2box" VALUES ('251', '250');



-- ----------------------------
-- Records of cris_rp_no_pdef
-- ----------------------------
INSERT INTO "public"."cris_rp_no_pdef" VALUES ('300', '3', 'f', '0','em', 'em', '0', null, 'EN', '0','em',  't', 'f', 'f', '0', 'f', 'writtenen', 'f', 'f', '300');
INSERT INTO "public"."cris_rp_no_pdef" VALUES ('301', '1', 'f', '0','em', 'em', '0', null, 'EN', '0','em',  'f', 'f', 'f', '0', 'f', 'spokenen', 'f', 'f', '301');
INSERT INTO "public"."cris_rp_no_pdef" VALUES ('302', '1', 'f','0','em', 'em', '0', null, 'ZH', '0','em',  'f', 'f', 'f', '0', 'f', 'spokenzh', 'f', 'f', '302');
INSERT INTO "public"."cris_rp_no_pdef" VALUES ('303', '3', 'f', '0','em', 'em', '0', null, 'ZH', '0','em',  'f', 'f', 'f', '0', 'f', 'writtenzh', 'f', 'f', '303');
INSERT INTO "public"."cris_rp_no_pdef" VALUES ('1301', '3', 'f', '0','em', 'em', '0', null, 'Qualification', '0','em',  'f', 'f', 'f', '0', 'f', 'qualification', 'f', 'f', '1301');
INSERT INTO "public"."cris_rp_no_pdef" VALUES ('1302', '3', 'f', '0','em', 'em', '0', null, 'Awarding Institution', '0','em',  'f', 'f', 'f', '0', 'f', 'qualificationinstitution', 'f', 'f', '1302');
INSERT INTO "public"."cris_rp_no_pdef" VALUES ('1350', '3', 'f', '0','em', 'em', '0', null, 'Date issued', '0','em',  'f', 'f', 'f', '0', 'f', 'qualificationdate', 'f', 'f', '1350');
INSERT INTO "public"."cris_rp_no_pdef" VALUES ('1401', '3', 'f', '0','em', 'em', '0', null, 'Member of', '0','em',  'f', 'f', 'f', '0', 'f', 'orgunit', 'f', 'f', '1401');
INSERT INTO "public"."cris_rp_no_pdef" VALUES ('1402', '3', 'f', '0','em', 'em', '0', null, 'Role', '0','em',  'f', 'f', 'f', '0', 'f', 'role', 'f', 'f', '1402');
INSERT INTO "public"."cris_rp_no_pdef" VALUES ('1451', '3', 'f', '0','em', 'em', '0', null, 'Start date', '0','em',  'f', 'f', 'f', '0', 'f', 'startdate', 'f', 'f', '1451');
INSERT INTO "public"."cris_rp_no_pdef" VALUES ('1450', '3', 'f', '0','em', 'em', '0', null, 'End date', '0','em',  'f', 'f', 'f', '0', 'f', 'enddate', 'f', 'f', '1450');


-- ----------------------------
-- Records of cris_rp_no_typo
-- ----------------------------
INSERT INTO "public"."cris_rp_no_tp" VALUES ('5', 'Spoken Languages', 'spoken', '1', null, 't', 't', 'f', '0', 't');
INSERT INTO "public"."cris_rp_no_tp" VALUES ('6', 'Written Languages', 'written', '3', null, 't', 'f', 'f', '10', 't');
INSERT INTO "public"."cris_rp_no_tp" VALUES ('20', 'Qualifications', 'qualifications', '3', null, 'f', 'f', 'f', '0', 't');
INSERT INTO "public"."cris_rp_no_tp" VALUES ('30', 'Affiliations', 'affiliation', '3', null, 'f', 'f', 'f', '0', 't');

-- ----------------------------
-- Records of cris_rp_no_typo2pdef
-- ----------------------------
INSERT INTO "public"."cris_rp_no_tp2pdef" VALUES ('5', '301');
INSERT INTO "public"."cris_rp_no_tp2pdef" VALUES ('5', '302');
INSERT INTO "public"."cris_rp_no_tp2pdef" VALUES ('6', '300');
INSERT INTO "public"."cris_rp_no_tp2pdef" VALUES ('6', '303');
INSERT INTO "public"."cris_rp_no_tp2pdef" VALUES ('20', '1301');
INSERT INTO "public"."cris_rp_no_tp2pdef" VALUES ('20', '1302');
INSERT INTO "public"."cris_rp_no_tp2pdef" VALUES ('20', '1350');
INSERT INTO "public"."cris_rp_no_tp2pdef" VALUES ('30', '1401');
INSERT INTO "public"."cris_rp_no_tp2pdef" VALUES ('30', '1402');
INSERT INTO "public"."cris_rp_no_tp2pdef" VALUES ('30', '1450');
INSERT INTO "public"."cris_rp_no_tp2pdef" VALUES ('30', '1451');

-- ----------------------------
-- Records of cris_rp_pdef
-- ----------------------------
INSERT INTO "public"."cris_rp_pdef" VALUES ('50', '3', 'f', '0','em', 'em', '3', null, 'Fullname', '10', 'em',  't', 'f', 'f', '900', 'f', 'fullName', 'f', 'f', '50');
INSERT INTO "public"."cris_rp_pdef" VALUES ('51', '3', 'f', '0','em', 'em', '0', null, 'Academic Name', '10', 'em',  't', 'f', 'f', '100', 'f', 'preferredName', 'f', 'f', '51');
INSERT INTO "public"."cris_rp_pdef" VALUES ('52', '3', 'f' ,'0','em', 'em', '0', null, 'Chinese Name', '10', 'em',  't', 'f', 'f', '800', 'f', 'translatedName', 'f', 'f', '52');
INSERT INTO "public"."cris_rp_pdef" VALUES ('53', '3', 'f' ,'0','em', 'em', '5', null, 'Variants', '10', 'em',  'f', 't', 'f', '50', 't', 'variants', 'f', 'f', '53');
INSERT INTO "public"."cris_rp_pdef" VALUES ('54', '3', 'f' ,'0','em', 'em', '0', null, 'Email', '10', 'em',  't', 't', 'f', '10', 'f', 'email', 'f', 'f', '54');
INSERT INTO "public"."cris_rp_pdef" VALUES ('55', '1', 'f' ,'0','em', 'em', '0', null, 'Personal Site', '10', 'em',  'f', 't', 'f', '600', 'f', 'personalsite', 'f', 'f', '55');
INSERT INTO "public"."cris_rp_pdef" VALUES ('1251', '3', 'f', '70', '%', 'em', '0', null, 'Main Affiliation', '10', 'em',  'f', 'f', 'f', '700', 'f', 'dept', 'f', 'f', '1251');
INSERT INTO "public"."cris_rp_pdef" VALUES ('1300', '3', 'f', '15','em', 'em', '0', null, null, '0', 'em',  'f', 'f', 'f', '1000', 'f', 'personalpicture', 'f', 'f', '1300');
INSERT INTO "public"."cris_rp_pdef" VALUES ('1400', '3', 'f', '0','em', 'em', '0', null, 'Working groups', '0', 'em', 'f', 'f', 'f', '0', 't', 'workgroups', 'f', 'f', '1400');
INSERT INTO "public"."cris_rp_pdef" VALUES ('1800', '3', 'f' ,'0','em', 'em', '5', null, 'Interests', '10', 'em',  'f', 't', 'f', '50', 't', 'interests', 'f', 'f', '1800');
INSERT INTO "public"."cris_rp_pdef" VALUES ('1900', '3', 'f' ,'0','em', 'em', '5', null, 'Orcid', '10', 'em',  'f', 't', 'f', '50', 't', 'orcid', 'f', 'f', '1900');

-- ----------------------------
-- Records of cris_rp_tab
-- ----------------------------
INSERT INTO "public"."cris_rp_tab" VALUES ('100', null, 'f', null, '0', 'information', 'Profile', '3');
INSERT INTO "public"."cris_rp_tab" VALUES ('250', null, 'f', null, '10', 'otherinfo', 'Other', '3');


-- ----------------------------
-- Records of cris_rp_tab2box
-- ----------------------------
INSERT INTO "public"."cris_rp_tab2box" VALUES ('100', '50');
INSERT INTO "public"."cris_rp_tab2box" VALUES ('100', '350');
INSERT INTO "public"."cris_rp_tab2box" VALUES ('100', '400');
INSERT INTO "public"."cris_rp_tab2box" VALUES ('250', '200');
INSERT INTO "public"."cris_rp_tab2box" VALUES ('250', '250');
INSERT INTO "public"."cris_rp_tab2box" VALUES ('250', '550');

-- ----------------------------
-- Records of cris_rp_wfile
-- ----------------------------
INSERT INTO "public"."cris_rp_wfile" VALUES ('1300', 'Upload photo', null, 't', '40', false);


-- ----------------------------
-- Records of jdyna_containable
-- ----------------------------

INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinition', '50', null,'50', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinition', '51', null,'51', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinition', '52', null,'52', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinition', '53', null,'53', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinition', '54', null,'54', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinition', '55', null,'55', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionproject', '100', null, null, '100', null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionproject', '101', null, null, '101', null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('typerpnestedobject', '350', null,null, null, null, null, null, null, '5', null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('typerpnestedobject', '351', null,null, null, null, null, null, null, '6', null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('pdrpnestedobject', '352', null,null, null, null, '300', null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('pdrpnestedobject', '353', null,null, null, null, '301', null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('pdrpnestedobject', '354', null,null, null, null, '302', null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('pdrpnestedobject', '355',null, null, null, null, '303', null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionproject', '1100', null,null, '950', null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionproject', '1101', null,null, '951', null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionou', '1150', null,null, null, '1000', null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionou', '1200', null,null, null, '1050', null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinition', '1401', null,'1251', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinition', '1450', null,'1300', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('typerpnestedobject', '1451', null,null, null, null, null, null, null, '20', null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('pdrpnestedobject', '1452', null,null, null, null, '1301', null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('pdrpnestedobject', '1453', null,null, null, null, '1302', null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('pdrpnestedobject', '1500', null,null, null, null, '1350', null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinition', '1550', null,'1400', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionproject', '1700', null,null, '1550', null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionproject', '1701', null,null, '1551', null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionproject', '1702', null,null, '1552', null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionproject', '1703', null,null, '1553', null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('typeprojectnestedobject', '1704', null,null, null, null, null, null, null, null, '24', null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('pdprojectnestedobject', '1705', null,null, null, null, null, '1554', null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('pdprojectnestedobject', '1706',null, null, null, null, null, '1555', null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('pdprojectnestedobject', '1707', null,null, null, null, null, '1556', null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('pdprojectnestedobject', '1708', null,null, null, null, null, '1557', null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionproject', '1750', null,null, '1600', null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionproject', '1751', null,null, '1601', null, null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionou', '1801', null,null, null, '1651', null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionou', '1802',null, null, null, '1652', null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionou', '1803',null, null, null, '1653', null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionou', '1850',null, null, null, '1700', null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitionou', '1851',null, null, null, '1701', null, null, null, null, null, null, null, null, null);
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinition', '1800', null,'1800', null, null, null, null, null, null, null, null, null, null, null);

-- ----------------------------
-- Records of jdyna_widget_date
-- ----------------------------
INSERT INTO "public"."jdyna_widget_date" VALUES ('101', null, null, 'f');
INSERT INTO "public"."jdyna_widget_date" VALUES ('1350', null, null, 'f');
INSERT INTO "public"."jdyna_widget_date" VALUES ('1450', null, null, 'f');
INSERT INTO "public"."jdyna_widget_date" VALUES ('1451', null, null, 'f');
INSERT INTO "public"."jdyna_widget_date" VALUES ('1550', null, null, 'f');
INSERT INTO "public"."jdyna_widget_date" VALUES ('1653', null, null, 'f');


-- ----------------------------
-- Records of jdyna_widget_link
-- ----------------------------
INSERT INTO "public"."jdyna_widget_link" VALUES ('55', 'Insert here the label to shown on public page (e.g. My site)', 'Url to the personal site', '40');

-- ----------------------------
-- Records of jdyna_widget_pointer
-- ----------------------------
INSERT INTO "public"."cris_rp_wpointer" VALUES ('950', '${displayObject.anagrafica4view[''preferredName''][0].value}', 'search.resourcetype:9', null, '20', 'org.dspace.app.cris.model.jdyna.value.RPPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO "public"."cris_rp_wpointer" VALUES ('951', '${displayObject.preferredName.value}', 'search.resourcetype:9', null, '20', 'org.dspace.app.cris.model.jdyna.value.RPPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO "public"."cris_rp_wpointer" VALUES ('1000', '${displayObject.preferredName.value}', 'search.resourcetype:9', null, '20', 'org.dspace.app.cris.model.jdyna.value.RPPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO "public"."cris_ou_wpointer" VALUES ('1251', '${displayObject.name}', 'search.resourcetype:11', null, '20', 'org.dspace.app.cris.model.jdyna.value.OUPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO "public"."cris_ou_wpointer" VALUES ('1400', '${displayObject.name}', 'search.resourcetype:11', null, '20', 'org.dspace.app.cris.model.jdyna.value.OUPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO "public"."cris_ou_wpointer" VALUES ('1401', '${displayObject.name}', 'search.resourcetype:11', null, '20', 'org.dspace.app.cris.model.jdyna.value.OUPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO "public"."cris_ou_wpointer" VALUES ('1557', '${displayObject.name}', 'search.resourcetype:11', null, '20', 'org.dspace.app.cris.model.jdyna.value.OUPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO "public"."cris_ou_wpointer" VALUES ('1651', '${displayObject.name}', null, null, '20', 'org.dspace.app.cris.model.jdyna.value.OUPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO "public"."cris_rp_wpointer" VALUES ('1652', '${displayObject.fullName}', null, null, '20', 'org.dspace.app.cris.model.jdyna.value.RPPointer', 'cris/uuid/${displayObject.uuid}');

-- ----------------------------
-- Records of jdyna_widget_text
-- ----------------------------
INSERT INTO "public"."jdyna_widget_text" VALUES ('50', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('51', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('52', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('53', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('54', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('100', 'f', '100', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('300', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('301', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('302', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('303', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('551', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1050', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1301', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1302', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1402', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1552', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1553', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1554', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1555', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1556', 'f', '3', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1600', 'f', '100', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1601', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1700', 'f', '100', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1800', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('1900', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('90050', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('90051', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('90052', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO "public"."jdyna_widget_text" VALUES ('90053', 'f', '30', 'em', 'em', '1', null, 'f', null);

-- ----------------------------
-- Records of cris_ou_box2con
-- ----------------------------
INSERT INTO "public"."cris_ou_box2con" VALUES ('300', '1150');
INSERT INTO "public"."cris_ou_box2con" VALUES ('300', '1200');
INSERT INTO "public"."cris_ou_box2con" VALUES ('300', '1801');
INSERT INTO "public"."cris_ou_box2con" VALUES ('300', '1802');
INSERT INTO "public"."cris_ou_box2con" VALUES ('300', '1803');
INSERT INTO "public"."cris_ou_box2con" VALUES ('300', '1851');
INSERT INTO "public"."cris_ou_box2con" VALUES ('750', '1150');
INSERT INTO "public"."cris_ou_box2con" VALUES ('750', '1200');
INSERT INTO "public"."cris_ou_box2con" VALUES ('750', '1801');
INSERT INTO "public"."cris_ou_box2con" VALUES ('750', '1803');
INSERT INTO "public"."cris_ou_box2con" VALUES ('750', '1851');
INSERT INTO "public"."cris_ou_box2con" VALUES ('800', '1850');


-- ----------------------------
-- Records of cris_rp_box2con
-- ----------------------------
INSERT INTO "public"."cris_rp_box2con" VALUES ('50', '50');
INSERT INTO "public"."cris_rp_box2con" VALUES ('50', '51');
INSERT INTO "public"."cris_rp_box2con" VALUES ('50', '52');
INSERT INTO "public"."cris_rp_box2con" VALUES ('50', '53');
INSERT INTO "public"."cris_rp_box2con" VALUES ('50', '54');
INSERT INTO "public"."cris_rp_box2con" VALUES ('50', '55');
INSERT INTO "public"."cris_rp_box2con" VALUES ('50', '1401');
INSERT INTO "public"."cris_rp_box2con" VALUES ('50', '1450');
INSERT INTO "public"."cris_rp_box2con" VALUES ('50', '1550');
INSERT INTO "public"."cris_rp_box2con" VALUES ('200', '1451');
INSERT INTO "public"."cris_rp_box2con" VALUES ('250', '350');
INSERT INTO "public"."cris_rp_box2con" VALUES ('250', '351');
INSERT INTO "public"."cris_rp_box2con" VALUES ('550', '50');
INSERT INTO "public"."cris_rp_box2con" VALUES ('550', '52');
INSERT INTO "public"."cris_rp_box2con" VALUES ('550', '55');
INSERT INTO "public"."cris_rp_box2con" VALUES ('550', '1401');
INSERT INTO "public"."cris_rp_box2con" VALUES ('550', '1450');
INSERT INTO "public"."cris_rp_box2con" VALUES ('250', '1800');


-- ----------------------------
-- Records of cris_pj_box2con
-- ----------------------------
INSERT INTO "public"."cris_pj_box2con" VALUES ('100', '1704');
INSERT INTO "public"."cris_pj_box2con" VALUES ('650', '100');
INSERT INTO "public"."cris_pj_box2con" VALUES ('650', '101');
INSERT INTO "public"."cris_pj_box2con" VALUES ('650', '1100');
INSERT INTO "public"."cris_pj_box2con" VALUES ('650', '1101');
INSERT INTO "public"."cris_pj_box2con" VALUES ('650', '1700');
INSERT INTO "public"."cris_pj_box2con" VALUES ('650', '1701');
INSERT INTO "public"."cris_pj_box2con" VALUES ('650', '1702');
INSERT INTO "public"."cris_pj_box2con" VALUES ('650', '1703');
INSERT INTO "public"."cris_pj_box2con" VALUES ('700', '100');
INSERT INTO "public"."cris_pj_box2con" VALUES ('700', '101');
INSERT INTO "public"."cris_pj_box2con" VALUES ('700', '1100');
INSERT INTO "public"."cris_pj_box2con" VALUES ('700', '1700');
INSERT INTO "public"."cris_pj_box2con" VALUES ('700', '1701');
INSERT INTO "public"."cris_pj_box2con" VALUES ('700', '1702');
INSERT INTO "public"."cris_pj_box2con" VALUES ('700', '1703');
INSERT INTO "public"."cris_pj_box2con" VALUES ('701', '1750');
INSERT INTO "public"."cris_pj_box2con" VALUES ('701', '1751');


--
-- Data for Name: cris_do_wfile; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO "public"."cris_do_wfile" (id, filedescription, labelanchor, showpreview, widgetsize, useinstatistics) VALUES (90100, 'Upload logo', NULL, true, 40, false);

--
-- Data for Name: cris_do_pdef; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO "public"."cris_do_pdef" (id, accesslevel, advancedsearch, fieldmin_col, fieldmin_col_unit, fieldmin_row_unit, fieldmin_row, help, label, labelminsize, labelminsizeunit, mandatory, newline, oncreation, priority, repeatable, shortname, showinlist, simplesearch, rendering_id) VALUES (90052, 3, false, 0, 'em', 'em', 0, NULL, 'Description', 0, 'em', false, false, false, 0, false, 'journalsdescription', false, false, 90052);
INSERT INTO "public"."cris_do_pdef" (id, accesslevel, advancedsearch, fieldmin_col, fieldmin_col_unit, fieldmin_row_unit, fieldmin_row, help, label, labelminsize, labelminsizeunit, mandatory, newline, oncreation, priority, repeatable, shortname, showinlist, simplesearch, rendering_id) VALUES (90100, 1, false, 0, 'em', 'em', 0, NULL, NULL, 0, 'em', false, false, false, 1000, false, 'journalspicture', false, false, 90100);
INSERT INTO "public"."cris_do_pdef" (id, accesslevel, advancedsearch, fieldmin_col, fieldmin_col_unit, fieldmin_row_unit, fieldmin_row, help, label, labelminsize, labelminsizeunit, mandatory, newline, oncreation, priority, repeatable, shortname, showinlist, simplesearch, rendering_id) VALUES (90050, 3, false, 0, 'em', 'em', 0, NULL, 'Name', 0, 'em', true, false, false, 900, false, 'journalsname', false, false, 90050);
INSERT INTO "public"."cris_do_pdef" (id, accesslevel, advancedsearch, fieldmin_col, fieldmin_col_unit, fieldmin_row_unit, fieldmin_row, help, label, labelminsize, labelminsizeunit, mandatory, newline, oncreation, priority, repeatable, shortname, showinlist, simplesearch, rendering_id) VALUES (90053, 3, false, 0, 'em', 'em', 0, NULL, 'Subject Classifications', 0, 'em', false, false, false, 850, true, 'journalskeywords', false, false, 90053);
INSERT INTO "public"."cris_do_pdef" (id, accesslevel, advancedsearch, fieldmin_col, fieldmin_col_unit, fieldmin_row_unit, fieldmin_row, help, label, labelminsize, labelminsizeunit, mandatory, newline, oncreation, priority, repeatable, shortname, showinlist, simplesearch, rendering_id) VALUES (90051, 3, false, 0, 'em', 'em', 0, NULL, 'ISSN', 0, 'em', false, false, false, 800, false, 'journalsissn', false, false, 90051);




--
-- Data for Name: cris_do_tp; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO "public"."cris_do_tp" (id, label, shortname) VALUES (1, 'Journals', 'journals');


--
-- Data for Name: cris_do_box; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO "public"."cris_do_box" (id, collapsed, externaljsp, priority, shortname, title, unrelevant, visibility, typedef_id) VALUES (42551, false, NULL, 0, 'journalsdescription', 'Description', false, 1, 1);
INSERT INTO "public"."cris_do_box" (id, collapsed, externaljsp, priority, shortname, title, unrelevant, visibility, typedef_id) VALUES (42600, false, 'dspaceitems', 0, 'journalspublications', 'Journal''s Article', false, 1, 1);

INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitiondynaobj', '92602', null, null, null, null, null, null, null, null, null, null, null, null, '90052');
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitiondynaobj', '92601', null, null, null, null, null, null, null, null, null, null, null, null, '90051');
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitiondynaobj', '92603', null, null, null, null, null, null, null, null, null, null, null, null, '90053');
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitiondynaobj', '92600', null, null, null, null, null, null, null, null, null, null, null, null, '90050');
INSERT INTO "public"."jdyna_containable" VALUES ('propertiesdefinitiondynaobj', '92650', null, null, null, null, null, null, null, null, null, null, null, null, '90100');

--
-- Data for Name: cris_do_box2con; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO "public"."cris_do_box2con" (cris_do_box_id, jdyna_containable_id) VALUES (42551, 92602);
INSERT INTO "public"."cris_do_box2con" (cris_do_box_id, jdyna_containable_id) VALUES (42551, 92601);
INSERT INTO "public"."cris_do_box2con" (cris_do_box_id, jdyna_containable_id) VALUES (42551, 92603);
INSERT INTO "public"."cris_do_box2con" (cris_do_box_id, jdyna_containable_id) VALUES (42551, 92600);
INSERT INTO "public"."cris_do_box2con" (cris_do_box_id, jdyna_containable_id) VALUES (42551, 92650);



--
-- Data for Name: cris_do_tab; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO "public"."cris_do_tab" (id, ext, mandatory, mime, priority, shortname, title, visibility, typedef_id) VALUES (20250, NULL, false, NULL, 0, 'journalsinformation', 'Details', 3, 1);


--
-- Data for Name: cris_do_etab; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO "public"."cris_do_etab" (id, ext, mandatory, mime, priority, shortname, title, visibility, displaytab_id, typedef_id) VALUES (20251, NULL, false, NULL, 0, 'editjournalsinformation', 'Edit Details', 1, NULL, 1);


--
-- Data for Name: cris_do_etab2box; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO "public"."cris_do_etab2box" (cris_do_etab_id, cris_do_box_id) VALUES (20251, 42551);


--
-- Data for Name: cris_do_tab2box; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO "public"."cris_do_tab2box" (cris_do_tab_id, cris_do_box_id) VALUES (20250, 42551);
INSERT INTO "public"."cris_do_tab2box" (cris_do_tab_id, cris_do_box_id) VALUES (20250, 42600);


--
-- Data for Name: cris_do_tp2pdef; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO "public"."cris_do_tp2pdef" (cris_do_tp_id, cris_do_pdef_id) VALUES (1, 90050);
INSERT INTO "public"."cris_do_tp2pdef" (cris_do_tp_id, cris_do_pdef_id) VALUES (1, 90051);
INSERT INTO "public"."cris_do_tp2pdef" (cris_do_tp_id, cris_do_pdef_id) VALUES (1, 90052);
INSERT INTO "public"."cris_do_tp2pdef" (cris_do_tp_id, cris_do_pdef_id) VALUES (1, 90053);
INSERT INTO "public"."cris_do_tp2pdef" (cris_do_tp_id, cris_do_pdef_id) VALUES (1, 90100);





-- update seq
SELECT setval('JDYNA_WIDGET_SEQ', max(id)) FROM cris_do_wfile;
SELECT setval('JDYNA_CONTAINABLE_SEQ', max(id)) FROM jdyna_containable;
SELECT setval('JDYNA_BOX_SEQ', max(id)) FROM cris_do_box;
SELECT setval('JDYNA_TAB_SEQ', max(id)) FROM cris_do_tab;
SELECT setval('JDYNA_PDEF_SEQ', max(aa.id)) FROM (
SELECT  a.id
FROM    cris_ou_pdef a
UNION
SELECT  b.id
FROM    cris_pj_pdef b
UNION
SELECT  c.id
FROM    cris_rp_pdef c
UNION
SELECT  g.id
FROM    cris_do_pdef g
UNION
SELECT  d.id
FROM    cris_ou_no_pdef d
UNION
SELECT  f.id
FROM    cris_pj_no_pdef f
UNION
SELECT  g.id
FROM    cris_rp_no_pdef g) aa;
