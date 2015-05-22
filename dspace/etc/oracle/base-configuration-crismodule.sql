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

-- ----------------------------
-- Records of cris_ou_box
-- ----------------------------
INSERT INTO cris_ou_box VALUES ('300', '0', null, '1000',  'details', 'Details', '0', '3');
INSERT INTO cris_ou_box VALUES ('451', '0', 'dspaceitems', '0', 'publication', 'OrgUnit''s Publications', '0', '3');
INSERT INTO cris_ou_box VALUES ('452', '0', 'dspaceitems', '0', 'rppublication', 'OrgUnit''s Researchers publications', '0', '3');
INSERT INTO cris_ou_box VALUES ('453', '0', 'persons', '0', 'rp', 'OrgUnit''s Researchers', '0', '3');
INSERT INTO cris_ou_box VALUES ('454', '0', 'projects', '0', 'projects', 'OrgUnit funded Projects', '0', '3');
INSERT INTO cris_ou_box VALUES ('750', '0', null, '10000', 'orgcard', 'Card', '1', '3');
INSERT INTO cris_ou_box VALUES ('800', '1', null, '0', 'description', 'Description', '0', '3');
INSERT INTO cris_ou_box VALUES ('801', '0', 'organizations', '0', 'organizations', 'SubOrgUnits', '0', '3');
INSERT INTO cris_ou_box VALUES ('850', '0', 'projects', '0', 'rpprojects', 'OrgUnit''s Researchers projects', '0', '3');



-- ----------------------------
-- Records of cris_ou_edittab
-- ----------------------------
INSERT INTO cris_ou_etab VALUES ('451', null, '0', null, '0', 'editinformation', 'OrgUnit''s Metadata', '1', null);


-- ----------------------------
-- Records of cris_ou_edittab2box
-- ----------------------------
INSERT INTO cris_ou_etab2box VALUES ('451', '300');
INSERT INTO cris_ou_etab2box VALUES ('451', '800');

-- ----------------------------
-- Records of cris_ou_pdef
-- ----------------------------
INSERT INTO cris_ou_pdef VALUES ('1000', '1', '0', '70', 'em', 'em',  '0', null, 'Director', '0', 'em', '1', '0', '0', '800', '0', 'director', '0', '0', '1000');
INSERT INTO cris_ou_pdef VALUES ('1050', '3', '0', '70', 'em', 'em',  '3', null, 'Organization name', '0', 'em', '1', '0', '0', '999', '0', 'name', '0', '0', '1050');
INSERT INTO cris_ou_pdef VALUES ('1651', '1', '0', '70', 'em', 'em',  '0', null, 'Parent OrgUnit', '0', 'em', '0', '0', '0', '700', '0', 'parentorgunit', '0', '0', '1651');
INSERT INTO cris_ou_pdef VALUES ('1652', '1', '0', '0', 'em', 'em',  '0', null, 'Scientifics Board', '0', 'em', '0', '0', '0', '10', '1', 'boards', '0', '0', '1652');
INSERT INTO cris_ou_pdef VALUES ('1653', '1', '0', '0', 'em', 'em',  '3', null, 'Established', '0', 'em', '0', '1', '0', '40', '0', 'date', '0', '0', '1653');
INSERT INTO cris_ou_pdef VALUES ('1700', '1', '0', '0', 'em', 'em', '0', null, null, '0', 'em', '0', '0', '0', '0', '0', 'description', '0', '0', '1700');
INSERT INTO cris_ou_pdef VALUES ('1701', '1', '0', '15', 'em', 'em',  '13', null, null, '0', 'em', '0', '0', '0', '1000', '0', 'logo', '0', '0', '1701');

-- ----------------------------
-- Records of cris_ou_tab
-- ----------------------------
INSERT INTO cris_ou_tab VALUES ('304', null, '0', null, '0', 'informations', 'Informations', '3');
INSERT INTO cris_ou_tab VALUES ('400', null, '0', null, '0', 'publications', 'Publications', '3');
INSERT INTO cris_ou_tab VALUES ('402', null, '0', null, '0', 'projects', 'Projects', '3');
INSERT INTO cris_ou_tab VALUES ('404', null, '0', null, '0', 'people', 'Peoples', '3');

-- ----------------------------
-- Records of cris_ou_tab2box
-- ----------------------------
INSERT INTO cris_ou_tab2box VALUES ('304', '300');
INSERT INTO cris_ou_tab2box VALUES ('304', '800');
INSERT INTO cris_ou_tab2box VALUES ('304', '801');
INSERT INTO cris_ou_tab2box VALUES ('400', '451');
INSERT INTO cris_ou_tab2box VALUES ('400', '452');
INSERT INTO cris_ou_tab2box VALUES ('400', '750');
INSERT INTO cris_ou_tab2box VALUES ('402', '454');
INSERT INTO cris_ou_tab2box VALUES ('402', '750');
INSERT INTO cris_ou_tab2box VALUES ('402', '850');
INSERT INTO cris_ou_tab2box VALUES ('404', '453');
INSERT INTO cris_ou_tab2box VALUES ('404', '750');

-- ----------------------------
-- Records of cris_ou_wfile
-- ----------------------------
INSERT INTO cris_ou_wfile VALUES ('1701', 'Upload logo', null, '1', '40', '0');

-- ----------------------------
-- Records of cris_pj_box
-- ----------------------------
INSERT INTO cris_pj_box VALUES ('100', '0', null, '999', 'grants', 'Grants', '0', '3');
INSERT INTO cris_pj_box VALUES ('450', '0', 'dspaceitems', '10', 'dspaceitems', 'Publications', '0', '3');
INSERT INTO cris_pj_box VALUES ('650', '0', null, '1000', 'primarydata', 'Primary Data', '0', '3');
INSERT INTO cris_pj_box VALUES ('700', '0', null, '1001', 'projectcard', 'Project Card', '0', '3');
INSERT INTO cris_pj_box VALUES ('701', '0', null, '10', 'description', 'Description', '0', '3');


-- ----------------------------
-- Records of cris_pj_edittab
-- ----------------------------
INSERT INTO cris_pj_etab VALUES ('103', null, '0', null, '0', 'editinformation', 'Information', '1', null);
INSERT INTO cris_pj_etab VALUES ('352', null, '0', null, '10', 'editgrants', 'Grants', '1', null);


-- ----------------------------
-- Records of cris_pj_edittab2box
-- ----------------------------
INSERT INTO cris_pj_etab2box VALUES ('103', '650');
INSERT INTO cris_pj_etab2box VALUES ('103', '701');
INSERT INTO cris_pj_etab2box VALUES ('352', '100');

-- ----------------------------
-- Records of cris_pj_no_pdef
-- ----------------------------
INSERT INTO cris_pj_no_pdef VALUES ('1554', '1', '0', '0', 'em', 'em',  '0', null, 'Grant type', '0', 'em', '0', '0', '0', '100', '0', 'granttype', '0', '0', '1554');
INSERT INTO cris_pj_no_pdef VALUES ('1555', '1', '0', '0', 'em', 'em',  '0', null, 'Amount', '0', 'em', '0', '0', '0', '0', '0', 'grantamount', '0', '0', '1555');
INSERT INTO cris_pj_no_pdef VALUES ('1556', '1', '0', '0', 'em', 'em',  '0', null, 'Duration', '0', 'em', '0', '0', '0', '0', '0', 'grantduration', '0', '0', '1556');
INSERT INTO cris_pj_no_pdef VALUES ('1557', '1', '0', '0', 'em', 'em',  '0', null, 'Agencies', '0', 'em', '0', '0', '0', '0', '0', 'agencies', '0', '0', '1557');

-- ----------------------------
-- Records of cris_pj_no_typo
-- ----------------------------
INSERT INTO cris_pj_no_tp VALUES ('24', 'Grant', 'grant', '1', null, '0', '0', '1', '0', '1');

-- ----------------------------
-- Records of cris_pj_no_typo2pdef
-- ----------------------------
INSERT INTO cris_pj_no_tp2pdef VALUES ('24', '1554');
INSERT INTO cris_pj_no_tp2pdef VALUES ('24', '1555');
INSERT INTO cris_pj_no_tp2pdef VALUES ('24', '1556');
INSERT INTO cris_pj_no_tp2pdef VALUES ('24', '1557');

-- ----------------------------
-- Records of cris_pj_pdef
-- ----------------------------
INSERT INTO cris_pj_pdef VALUES ('100', '1', '0', '70', 'em', 'em', '0', null, 'Project title', '11','em',  '1', '0', '0', '999', '0', 'title', '0', '0', '100');
INSERT INTO cris_pj_pdef VALUES ('101', '1', '0', '0', 'em', 'em', '0', null, 'Expected Completion', '0','em',  '0', '1', '0', '5', '0', 'expdate', '0', '0', '101');
INSERT INTO cris_pj_pdef VALUES ('950', '1', '0', '70', 'em', 'em', '3', null, 'Project Coordinator', '0','em',  '1', '0', '0', '800', '0', 'principalinvestigator', '0', '0', '950');
INSERT INTO cris_pj_pdef VALUES ('951', '1', '0', '0', 'em', 'em', '0', null, 'Co-Investigator(s)', '0','em',  '0', '0', '0', '0', '1', 'coinvestigators', '0', '0', '951');
INSERT INTO cris_pj_pdef VALUES ('1550', '1', '0', '1', 'em', 'em', '0', null, 'Start date', '0','em',  '0', '0', '0', '10', '0', 'startdate', '0', '0', '1550');
INSERT INTO cris_pj_pdef VALUES ('1551', '1', '0', '15', 'em', 'em', '13', null, null, '0','em',  '0', '0', '0', '1000', '0', 'logo', '0', '0', '1551');
INSERT INTO cris_pj_pdef VALUES ('1552', '1', '0', '0', 'em', 'em', '3', null, 'Status', '10','em',  '0', '0', '0', '20', '0', 'status', '0', '0', '1552');
INSERT INTO cris_pj_pdef VALUES ('1553', '1', '0', '70', 'em', 'em', '0', null, 'Code', '11','em',  '0', '0', '0', '900', '0', 'code', '0', '0', '1553');
INSERT INTO cris_pj_pdef VALUES ('1600', '1', '0', '0', 'em', 'em', '0', null, 'Abstract', '0','em',  '0', '1', '0', '0', '0', 'abstract', '0', '0', '1600');
INSERT INTO cris_pj_pdef VALUES ('1601', '1', '0', '0', 'em', 'em', '0', null, 'Keyword(s)', '0','em',  '0', '0', '0', '0', '1', 'keywords', '0', '0', '1601');

-- ----------------------------
-- Records of cris_pj_tab
-- ----------------------------
INSERT INTO cris_pj_tab VALUES ('102', null, '0', null, '10', 'grants', 'Grants', '3');
INSERT INTO cris_pj_tab VALUES ('350', null, '0', null, '0', 'informations', 'Informations', '3');

-- ----------------------------
-- Records of cris_pj_tab2box
-- ----------------------------
INSERT INTO cris_pj_tab2box VALUES ('102', '100');
INSERT INTO cris_pj_tab2box VALUES ('102', '450');
INSERT INTO cris_pj_tab2box VALUES ('102', '700');
INSERT INTO cris_pj_tab2box VALUES ('350', '650');
INSERT INTO cris_pj_tab2box VALUES ('350', '701');

-- ----------------------------
-- Records of cris_pj_wfile
-- ----------------------------
INSERT INTO cris_pj_wfile VALUES ('1551', 'Insert logo', null, '1', '40', 0);
-- ----------------------------
-- Records of cris_rp_box
-- ----------------------------
INSERT INTO cris_rp_box VALUES ('50', '0', null, '1000', 'researcherprofile', 'Profile', '0', '3');
INSERT INTO cris_rp_box VALUES ('200', '0', null, '10', 'qualifications', 'Professional Qualifications', '0', '3');
INSERT INTO cris_rp_box VALUES ('250', '0', null, '10', 'media', 'Media Contact Directory', '0', '3');
INSERT INTO cris_rp_box VALUES ('350', '0', 'dspaceitems', '0', 'dspaceitems', 'Publications', '0', '3');
INSERT INTO cris_rp_box VALUES ('400', '0', 'projects', '0', 'projects', 'Projects', '0', '3');
INSERT INTO cris_rp_box VALUES ('550', '0', null, '1000', 'namecard', 'Name Card', '1', '3');


-- ----------------------------
-- Records of cris_rp_edittab
-- ----------------------------
INSERT INTO cris_rp_etab VALUES ('101', null, '0', null, '0', 'editinformation', 'Edit Personal Information', '2', null);
INSERT INTO cris_rp_etab VALUES ('251', null, '0', null, '0', 'editotherinfo', 'Edit Other', '1', null);


-- ----------------------------
-- Records of cris_rp_edittab2box
-- ----------------------------
INSERT INTO cris_rp_etab2box VALUES ('101', '50');
INSERT INTO cris_rp_etab2box VALUES ('251', '200');
INSERT INTO cris_rp_etab2box VALUES ('251', '250');



-- ----------------------------
-- Records of cris_rp_no_pdef
-- ----------------------------
INSERT INTO cris_rp_no_pdef VALUES ('300', '3', '0', '0','em', 'em', '0', null, 'EN', '0','em',  '1', '0', '0', '0', '0', 'writtenen', '0', '0', '300');
INSERT INTO cris_rp_no_pdef VALUES ('301', '1', '0', '0','em', 'em', '0', null, 'EN', '0','em',  '0', '0', '0', '0', '0', 'spokenen', '0', '0', '301');
INSERT INTO cris_rp_no_pdef VALUES ('302', '1', '0','0','em', 'em', '0', null, 'ZH', '0','em',  '0', '0', '0', '0', '0', 'spokenzh', '0', '0', '302');
INSERT INTO cris_rp_no_pdef VALUES ('303', '3', '0', '0','em', 'em', '0', null, 'ZH', '0','em',  '0', '0', '0', '0', '0', 'writtenzh', '0', '0', '303');
INSERT INTO cris_rp_no_pdef VALUES ('1301', '3', '0', '0','em', 'em', '0', null, 'Qualification', '0','em',  '0', '0', '0', '0', '0', 'qualification', '0', '0', '1301');
INSERT INTO cris_rp_no_pdef VALUES ('1302', '3', '0', '0','em', 'em', '0', null, 'Awarding Institution', '0','em',  '0', '0', '0', '0', '0', 'qualificationinstitution', '0', '0', '1302');
INSERT INTO cris_rp_no_pdef VALUES ('1350', '3', '0', '0','em', 'em', '0', null, 'Date issued', '0','em',  '0', '0', '0', '0', '0', 'qualificationdate', '0', '0', '1350');
INSERT INTO cris_rp_no_pdef VALUES ('1401', '3', 'f', '0','em', 'em', '0', null, 'Member of', '0','em',  'f', 'f', 'f', '0', 'f', 'orgunit', 'f', 'f', '1401');
INSERT INTO cris_rp_no_pdef VALUES ('1402', '3', 'f', '0','em', 'em', '0', null, 'Role', '0','em',  'f', 'f', 'f', '0', 'f', 'role', 'f', 'f', '1402');
INSERT INTO cris_rp_no_pdef VALUES ('1451', '3', 'f', '0','em', 'em', '0', null, 'Start date', '0','em',  'f', 'f', 'f', '0', 'f', 'startdate', 'f', 'f', '1451');
INSERT INTO cris_rp_no_pdef VALUES ('1450', '3', 'f', '0','em', 'em', '0', null, 'End date', '0','em',  'f', 'f', 'f', '0', 'f', 'enddate', 'f', 'f', '1450');

-- ----------------------------
-- Records of cris_rp_no_typo
-- ----------------------------
INSERT INTO cris_rp_no_tp VALUES ('5', 'Spoken Languages', 'spoken', '1', null, '1', '1', '0', '0', '1');
INSERT INTO cris_rp_no_tp VALUES ('6', 'Written Languages', 'written', '3', null, '1', '0', '0', '10', '1');
INSERT INTO cris_rp_no_tp VALUES ('20', 'Qualifications', 'qualifications', '3', null, '0', '0', '0', '0', '1');
INSERT INTO cris_rp_no_tp VALUES ('30', 'Affiliations', 'affiliation', '3', null, '0', '0', '0', '0', '1');

-- ----------------------------
-- Records of cris_rp_no_typo2pdef
-- ----------------------------
INSERT INTO cris_rp_no_tp2pdef VALUES ('5', '301');
INSERT INTO cris_rp_no_tp2pdef VALUES ('5', '302');
INSERT INTO cris_rp_no_tp2pdef VALUES ('6', '300');
INSERT INTO cris_rp_no_tp2pdef VALUES ('6', '303');
INSERT INTO cris_rp_no_tp2pdef VALUES ('20', '1301');
INSERT INTO cris_rp_no_tp2pdef VALUES ('20', '1302');
INSERT INTO cris_rp_no_tp2pdef VALUES ('20', '1350');
INSERT INTO cris_rp_no_tp2pdef VALUES ('30', '1401');
INSERT INTO cris_rp_no_tp2pdef VALUES ('30', '1402');
INSERT INTO cris_rp_no_tp2pdef VALUES ('30', '1450');
INSERT INTO cris_rp_no_tp2pdef VALUES ('30', '1451');
-- ----------------------------
-- Records of cris_rp_pdef
-- ----------------------------
INSERT INTO cris_rp_pdef VALUES ('50', '3', '0', '0','em', 'em', '3', null, 'Fullname', '10', 'em',  '1', '0', '0', '900', '0', 'fullName', '0', '0', '50');
INSERT INTO cris_rp_pdef VALUES ('51', '3', '0', '0','em', 'em', '0', null, 'Academic Name', '10', 'em',  '1', '0', '0', '100', '0', 'preferredName', '0', '0', '51');
INSERT INTO cris_rp_pdef VALUES ('52', '3', '0' ,'0','em', 'em', '0', null, 'Chinese Name', '10', 'em',  '1', '0', '0', '800', '0', 'translatedName', '0', '0', '52');
INSERT INTO cris_rp_pdef VALUES ('53', '3', '0' ,'0','em', 'em', '5', null, 'Variants', '10', 'em',  '0', '1', '0', '50', '1', 'variants', '0', '0', '53');
INSERT INTO cris_rp_pdef VALUES ('54', '3', '0' ,'0','em', 'em', '0', null, 'Email', '10', 'em',  '1', '1', '0', '10', '0', 'email', '0', '0', '54');
INSERT INTO cris_rp_pdef VALUES ('55', '1', '0' ,'0','em', 'em', '0', null, 'Personal Site', '10', 'em',  '0', '1', '0', '600', '0', 'personalsite', '0', '0', '55');
INSERT INTO cris_rp_pdef VALUES ('1251', '3', '0', '70', '%', 'em', '0', null, 'Department', '10', 'em',  '0', '0', '0', '700', '0', 'dept', '0', '0', '1251');
INSERT INTO cris_rp_pdef VALUES ('1300', '3', '0', '15','em', 'em', '0', null, null, '0', 'em',  '0', '0', '0', '1000', '0', 'personalpicture', '0', '0', '1300');
INSERT INTO cris_rp_pdef VALUES ('1400', '3', '0', '0','em', 'em', '0', null, 'Working groups', '0', 'em', '0', '0', '0', '0', '1', 'workgroups', '0', '0', '1400');
INSERT INTO cris_rp_pdef VALUES ('1800', '3', '0' ,'0','em', 'em', '5', null, 'Interests', '10', 'em',  '0', '1', '0', '50', '1', 'interests', '0', '0', '1800');
INSERT INTO cris_rp_pdef VALUES ('1900', '3', '0' ,'0','em', 'em', '5', null, 'Orcid', '10', 'em',  '0', '1', '0', '50', '1', 'orcid', '0', '0', '1900');

-- ----------------------------
-- Records of cris_rp_tab
-- ----------------------------
INSERT INTO cris_rp_tab VALUES ('100', null, '0', null, '0', 'information', 'Profile', '3');
INSERT INTO cris_rp_tab VALUES ('250', null, '0', null, '10', 'otherinfo', 'Other', '3');


-- ----------------------------
-- Records of cris_rp_tab2box
-- ----------------------------
INSERT INTO cris_rp_tab2box VALUES ('100', '50');
INSERT INTO cris_rp_tab2box VALUES ('100', '350');
INSERT INTO cris_rp_tab2box VALUES ('100', '400');
INSERT INTO cris_rp_tab2box VALUES ('250', '200');
INSERT INTO cris_rp_tab2box VALUES ('250', '250');
INSERT INTO cris_rp_tab2box VALUES ('250', '550');

-- ----------------------------
-- Records of cris_rp_wfile
-- ----------------------------
INSERT INTO cris_rp_wfile VALUES ('1300', 'Upload photo', null, '1', '40', 0);


-- ----------------------------
-- Records of jdyna_containable
-- ----------------------------

INSERT INTO jdyna_containable VALUES ('propertiesdefinition', '50', null,'50', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinition', '51', null,'51', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinition', '52', null,'52', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinition', '53', null,'53', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinition', '54', null,'54', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinition', '55', null,'55', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionproject', '100', null, null, '100', null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionproject', '101', null, null, '101', null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('typerpnestedobject', '350', null,null, null, null, null, null, null, '5', null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('typerpnestedobject', '351', null,null, null, null, null, null, null, '6', null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('pdrpnestedobject', '352', null,null, null, null, '300', null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('pdrpnestedobject', '353', null,null, null, null, '301', null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('pdrpnestedobject', '354', null,null, null, null, '302', null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('pdrpnestedobject', '355',null, null, null, null, '303', null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionproject', '1100', null,null, '950', null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionproject', '1101', null,null, '951', null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionou', '1150', null,null, null, '1000', null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionou', '1200', null,null, null, '1050', null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinition', '1401', null,'1251', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinition', '1450', null,'1300', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('typerpnestedobject', '1451', null,null, null, null, null, null, null, '20', null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('pdrpnestedobject', '1452', null,null, null, null, '1301', null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('pdrpnestedobject', '1453', null,null, null, null, '1302', null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('pdrpnestedobject', '1500', null,null, null, null, '1350', null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinition', '1550', null,'1400', null, null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionproject', '1700', null,null, '1550', null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionproject', '1701', null,null, '1551', null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionproject', '1702', null,null, '1552', null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionproject', '1703', null,null, '1553', null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('typeprojectnestedobject', '1704', null,null, null, null, null, null, null, null, '24', null, null, null, null);
INSERT INTO jdyna_containable VALUES ('pdprojectnestedobject', '1705', null,null, null, null, null, '1554', null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('pdprojectnestedobject', '1706',null, null, null, null, null, '1555', null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('pdprojectnestedobject', '1707', null,null, null, null, null, '1556', null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('pdprojectnestedobject', '1708', null,null, null, null, null, '1557', null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionproject', '1750', null,null, '1600', null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionproject', '1751', null,null, '1601', null, null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionou', '1801', null,null, null, '1651', null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionou', '1802',null, null, null, '1652', null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionou', '1803',null, null, null, '1653', null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionou', '1850',null, null, null, '1700', null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinitionou', '1851',null, null, null, '1701', null, null, null, null, null, null, null, null, null);
INSERT INTO jdyna_containable VALUES ('propertiesdefinition', '1800', null,'1800', null, null, null, null, null, null, null, null, null, null, null);

-- ----------------------------
-- Records of jdyna_widget_date
-- ----------------------------
INSERT INTO jdyna_widget_date VALUES ('101', null, null, '0');
INSERT INTO jdyna_widget_date VALUES ('1350', null, null, '0');
INSERT INTO jdyna_widget_date VALUES ('1450', null, null, 'f');
INSERT INTO jdyna_widget_date VALUES ('1451', null, null, 'f');
INSERT INTO jdyna_widget_date VALUES ('1550', null, null, '0');
INSERT INTO jdyna_widget_date VALUES ('1653', null, null, '0');


-- ----------------------------
-- Records of jdyna_widget_link
-- ----------------------------
INSERT INTO jdyna_widget_link VALUES ('55', 'Insert here the label to shown on public page (e.g. My site)', 'Url to the personal site', '40');

-- ----------------------------
-- Records of jdyna_widget_pointer
-- ----------------------------
-- No se han podido grabar estos registros
--
INSERT INTO cris_rp_wpointer VALUES ('950', '${displayObject.anagrafica4view[''preferredName''][0].value}', 'search.resourcetype:9', null, '20', 'org.dspace.app.cris.model.jdyna.value.RPPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO cris_rp_wpointer VALUES ('951', '${displayObject.preferredName.value}', 'search.resourcetype:9', null, '20', 'org.dspace.app.cris.model.jdyna.value.RPPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO cris_rp_wpointer VALUES ('1000', '${displayObject.preferredName.value}', 'search.resourcetype:9', null, '20', 'org.dspace.app.cris.model.jdyna.value.RPPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO cris_ou_wpointer VALUES ('1251', '${displayObject.name}', 'search.resourcetype:11', null, '20', 'org.dspace.app.cris.model.jdyna.value.OUPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO cris_ou_wpointer VALUES ('1400', '${displayObject.name}', 'search.resourcetype:11', null, '20', 'org.dspace.app.cris.model.jdyna.value.OUPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO cris_ou_wpointer VALUES ('1401', '${displayObject.name}', 'search.resourcetype:11', null, '20', 'org.dspace.app.cris.model.jdyna.value.OUPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO cris_ou_wpointer VALUES ('1557', '${displayObject.name}', 'search.resourcetype:11', null, '20', 'org.dspace.app.cris.model.jdyna.value.OUPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO cris_ou_wpointer VALUES ('1651', '${displayObject.name}', null, null, '20', 'org.dspace.app.cris.model.jdyna.value.OUPointer', 'cris/uuid/${displayObject.uuid}');
INSERT INTO cris_rp_wpointer VALUES ('1652', '${displayObject.fullName}', null, null, '20', 'org.dspace.app.cris.model.jdyna.value.RPPointer', 'cris/uuid/${displayObject.uuid}');
-- No concuerda el número de campos con los de las tablas
--

-- ----------------------------
-- ----------------------------
-- Records of jdyna_widget_text
-- ----------------------------
INSERT INTO jdyna_widget_text VALUES ('50', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('51', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('52', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('53', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('54', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('100', '0', '100', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('300', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('301', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('302', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('303', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('551', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1050', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1301', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1302', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1402', 'f', '30', 'em', 'em', '1', null, 'f', null);
INSERT INTO jdyna_widget_text VALUES ('1552', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1553', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1554', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1555', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1556', '0', '3', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1600', '0', '100', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1601', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1700', '0', '100', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1800', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('1900', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('90050', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('90051', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('90052', '0', '30', 'em', 'em', '1', null, '0', null);
INSERT INTO jdyna_widget_text VALUES ('90053', '0', '30', 'em', 'em', '1', null, '0', null);

-- ----------------------------
-- Records of cris_ou_box2con
-- ----------------------------
INSERT INTO cris_ou_box2con VALUES ('300', '1150');
INSERT INTO cris_ou_box2con VALUES ('300', '1200');
INSERT INTO cris_ou_box2con VALUES ('300', '1801');
INSERT INTO cris_ou_box2con VALUES ('300', '1802');
INSERT INTO cris_ou_box2con VALUES ('300', '1803');
INSERT INTO cris_ou_box2con VALUES ('300', '1851');
INSERT INTO cris_ou_box2con VALUES ('750', '1150');
INSERT INTO cris_ou_box2con VALUES ('750', '1200');
INSERT INTO cris_ou_box2con VALUES ('750', '1801');
INSERT INTO cris_ou_box2con VALUES ('750', '1803');
INSERT INTO cris_ou_box2con VALUES ('750', '1851');
INSERT INTO cris_ou_box2con VALUES ('800', '1850');


-- ----------------------------
-- Records of cris_rp_box2con
-- ----------------------------
INSERT INTO cris_rp_box2con VALUES ('50', '50');
INSERT INTO cris_rp_box2con VALUES ('50', '51');
INSERT INTO cris_rp_box2con VALUES ('50', '52');
INSERT INTO cris_rp_box2con VALUES ('50', '53');
INSERT INTO cris_rp_box2con VALUES ('50', '54');
INSERT INTO cris_rp_box2con VALUES ('50', '55');
INSERT INTO cris_rp_box2con VALUES ('50', '1401');
INSERT INTO cris_rp_box2con VALUES ('50', '1450');
INSERT INTO cris_rp_box2con VALUES ('50', '1550');
INSERT INTO cris_rp_box2con VALUES ('200', '1451');
INSERT INTO cris_rp_box2con VALUES ('250', '350');
INSERT INTO cris_rp_box2con VALUES ('250', '351');
INSERT INTO cris_rp_box2con VALUES ('550', '50');
INSERT INTO cris_rp_box2con VALUES ('550', '52');
INSERT INTO cris_rp_box2con VALUES ('550', '55');
INSERT INTO cris_rp_box2con VALUES ('550', '1401');
INSERT INTO cris_rp_box2con VALUES ('550', '1450');
INSERT INTO cris_rp_box2con VALUES ('250', '1800');


-- ----------------------------
-- Records of cris_pj_box2con
-- ----------------------------
INSERT INTO cris_pj_box2con VALUES ('100', '1704');
INSERT INTO cris_pj_box2con VALUES ('650', '100');
INSERT INTO cris_pj_box2con VALUES ('650', '101');
INSERT INTO cris_pj_box2con VALUES ('650', '1100');
INSERT INTO cris_pj_box2con VALUES ('650', '1101');
INSERT INTO cris_pj_box2con VALUES ('650', '1700');
INSERT INTO cris_pj_box2con VALUES ('650', '1701');
INSERT INTO cris_pj_box2con VALUES ('650', '1702');
INSERT INTO cris_pj_box2con VALUES ('650', '1703');
INSERT INTO cris_pj_box2con VALUES ('700', '100');
INSERT INTO cris_pj_box2con VALUES ('700', '101');
INSERT INTO cris_pj_box2con VALUES ('700', '1100');
INSERT INTO cris_pj_box2con VALUES ('700', '1700');
INSERT INTO cris_pj_box2con VALUES ('700', '1701');
INSERT INTO cris_pj_box2con VALUES ('700', '1702');
INSERT INTO cris_pj_box2con VALUES ('700', '1703');
INSERT INTO cris_pj_box2con VALUES ('701', '1750');
INSERT INTO cris_pj_box2con VALUES ('701', '1751');


--
-- Data for Name: cris_do_wfile; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO cris_do_wfile (id, filedescription, labelanchor, showpreview, widgetsize, useinstatistics) VALUES (90100, 'Upload logo', NULL, 1, 40, 0);

--
-- Data for Name: cris_do_pdef; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO cris_do_pdef (id, accesslevel, advancedsearch, fieldmin_col, fieldmin_col_unit, fieldmin_row_unit, fieldmin_row, help, label, labelminsize, labelminsizeunit, mandatory, newline, oncreation, priority, repeatable, shortname, showinlist, simplesearch, rendering_id) VALUES (90052, 3, 0, 0, 'em', 'em', 0, NULL, 'Description', 0, 'em', 0, 0, 0, 0, 0, 'journalsdescription', 0, 0, 90052);
INSERT INTO cris_do_pdef (id, accesslevel, advancedsearch, fieldmin_col, fieldmin_col_unit, fieldmin_row_unit, fieldmin_row, help, label, labelminsize, labelminsizeunit, mandatory, newline, oncreation, priority, repeatable, shortname, showinlist, simplesearch, rendering_id) VALUES (90100, 1, 0, 0, 'em', 'em', 0, NULL, NULL, 0, 'em', 0, 0, 0, 1000, 0, 'journalspicture', 0, 0, 90100);
INSERT INTO cris_do_pdef (id, accesslevel, advancedsearch, fieldmin_col, fieldmin_col_unit, fieldmin_row_unit, fieldmin_row, help, label, labelminsize, labelminsizeunit, mandatory, newline, oncreation, priority, repeatable, shortname, showinlist, simplesearch, rendering_id) VALUES (90050, 3, 0, 0, 'em', 'em', 0, NULL, 'Name', 0, 'em', 1, 0, 0, 900, 0, 'journalsname', 0, 0, 90050);
INSERT INTO cris_do_pdef (id, accesslevel, advancedsearch, fieldmin_col, fieldmin_col_unit, fieldmin_row_unit, fieldmin_row, help, label, labelminsize, labelminsizeunit, mandatory, newline, oncreation, priority, repeatable, shortname, showinlist, simplesearch, rendering_id) VALUES (90053, 3, 0, 0, 'em', 'em', 0, NULL, 'Subject Classifications', 0, 'em', 0, 0, 0, 850, 1, 'journalskeywords', 0, 0, 90053);
INSERT INTO cris_do_pdef (id, accesslevel, advancedsearch, fieldmin_col, fieldmin_col_unit, fieldmin_row_unit, fieldmin_row, help, label, labelminsize, labelminsizeunit, mandatory, newline, oncreation, priority, repeatable, shortname, showinlist, simplesearch, rendering_id) VALUES (90051, 3, 0, 0, 'em', 'em', 0, NULL, 'ISSN', 0, 'em', 0, 0, 0, 800, 0, 'journalsissn', 0, 0, 90051);




--
-- Data for Name: cris_do_tp; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO cris_do_tp (id, label, shortname) VALUES (1, 'Journals', 'journals');


--
-- Data for Name: cris_do_box; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO cris_do_box (id, collapsed, externaljsp, priority, shortname, title, unrelevant, visibility, typedef_id) VALUES (42551, 0, NULL, 0, 'journalsdescription', 'Description', 0, 1, 1);
INSERT INTO cris_do_box (id, collapsed, externaljsp, priority, shortname, title, unrelevant, visibility, typedef_id) VALUES (42600, 0, 'dspaceitems', 0, 'journalspublications', 'Journal''s Article', 0, 1, 1);

INSERT INTO jdyna_containable VALUES ('propertiesdefinitiondynaobj', '92602', null, null, null, null, null, null, null, null, null, null, null, null, '90052');
INSERT INTO jdyna_containable VALUES ('propertiesdefinitiondynaobj', '92601', null, null, null, null, null, null, null, null, null, null, null, null, '90051');
INSERT INTO jdyna_containable VALUES ('propertiesdefinitiondynaobj', '92603', null, null, null, null, null, null, null, null, null, null, null, null, '90053');
INSERT INTO jdyna_containable VALUES ('propertiesdefinitiondynaobj', '92600', null, null, null, null, null, null, null, null, null, null, null, null, '90050');
INSERT INTO jdyna_containable VALUES ('propertiesdefinitiondynaobj', '92650', null, null, null, null, null, null, null, null, null, null, null, null, '90100');

--
-- Data for Name: cris_do_box2con; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO cris_do_box2con (cris_do_box_id, jdyna_containable_id) VALUES (42551, 92602);
INSERT INTO cris_do_box2con (cris_do_box_id, jdyna_containable_id) VALUES (42551, 92601);
INSERT INTO cris_do_box2con (cris_do_box_id, jdyna_containable_id) VALUES (42551, 92603);
INSERT INTO cris_do_box2con (cris_do_box_id, jdyna_containable_id) VALUES (42551, 92600);
INSERT INTO cris_do_box2con (cris_do_box_id, jdyna_containable_id) VALUES (42551, 92650);



--
-- Data for Name: cris_do_tab; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO cris_do_tab (id, ext, mandatory, mime, priority, shortname, title, visibility, typedef_id) VALUES (20250, NULL, 0, NULL, 0, 'journalsinformation', 'Details', 3, 1);


--
-- Data for Name: cris_do_etab; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO cris_do_etab (id, ext, mandatory, mime, priority, shortname, title, visibility, displaytab_id, typedef_id) VALUES (20251, NULL, 0, NULL, 0, 'editjournalsinformation', 'Edit Details', 1, NULL, 1);


--
-- Data for Name: cris_do_etab2box; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO cris_do_etab2box (cris_do_etab_id, cris_do_box_id) VALUES (20251, 42551);


--
-- Data for Name: cris_do_tab2box; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO cris_do_tab2box (cris_do_tab_id, cris_do_box_id) VALUES (20250, 42551);
INSERT INTO cris_do_tab2box (cris_do_tab_id, cris_do_box_id) VALUES (20250, 42600);


--
-- Data for Name: cris_do_tp2pdef; Type: TABLE DATA; Schema: public; Owner: dspace
--

INSERT INTO cris_do_tp2pdef (cris_do_tp_id, cris_do_pdef_id) VALUES (1, 90050);
INSERT INTO cris_do_tp2pdef (cris_do_tp_id, cris_do_pdef_id) VALUES (1, 90051);
INSERT INTO cris_do_tp2pdef (cris_do_tp_id, cris_do_pdef_id) VALUES (1, 90052);
INSERT INTO cris_do_tp2pdef (cris_do_tp_id, cris_do_pdef_id) VALUES (1, 90053);
INSERT INTO cris_do_tp2pdef (cris_do_tp_id, cris_do_pdef_id) VALUES (1, 90100);




-- No modificado, hecho manualmente.
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
