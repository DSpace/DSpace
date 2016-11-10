--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
truncate table cris_do_box2policygroup;
truncate table cris_do_box2policysingle;
truncate table cris_do_etab2policygroup;
truncate table cris_do_etab2policysingle; 
truncate table cris_do_tab2policygroup;
truncate table cris_do_tab2policysingle;
truncate table cris_ou_box2policygroup;
truncate table cris_ou_box2policysingle;
truncate table cris_ou_etab2policygroup;
truncate table cris_ou_etab2policysingle; 
truncate table cris_ou_tab2policygroup;
truncate table cris_ou_tab2policysingle;
truncate table cris_pj_box2policygroup;
truncate table cris_pj_box2policysingle;
truncate table cris_pj_etab2policygroup;
truncate table cris_pj_etab2policysingle; 
truncate table cris_pj_tab2policygroup;
truncate table cris_pj_tab2policysingle;
truncate table cris_rp_box2policygroup;
truncate table cris_rp_box2policysingle;
truncate table cris_rp_etab2policygroup;
truncate table cris_rp_etab2policysingle; 
truncate table cris_rp_tab2policygroup;
truncate table cris_rp_tab2policysingle;
alter table cris_do_box2policygroup drop column authorizedGroup;
alter table cris_do_box2policysingle drop column authorizedSingle;
alter table cris_do_etab2policygroup drop column authorizedGroup;
alter table cris_do_etab2policysingle drop column authorizedSingle;
alter table cris_do_tab2policygroup drop column authorizedGroup;
alter table cris_do_tab2policysingle drop column authorizedSingle;
alter table cris_ou_box2policygroup drop column authorizedGroup;
alter table cris_ou_box2policysingle drop column authorizedSingle;
alter table cris_ou_etab2policygroup drop column authorizedGroup;
alter table cris_ou_etab2policysingle drop column authorizedSingle;
alter table cris_ou_tab2policygroup drop column authorizedGroup;
alter table cris_ou_tab2policysingle drop column authorizedSingle;
alter table cris_pj_box2policygroup drop column authorizedGroup;
alter table cris_pj_box2policysingle drop column authorizedSingle;
alter table cris_pj_etab2policygroup drop column authorizedGroup;
alter table cris_pj_etab2policysingle drop column authorizedSingle;
alter table cris_pj_tab2policygroup drop column authorizedGroup;
alter table cris_pj_tab2policysingle drop column authorizedSingle;
alter table cris_rp_box2policygroup drop column authorizedGroup;
alter table cris_rp_box2policysingle drop column authorizedSingle;
alter table cris_rp_etab2policygroup drop column authorizedGroup;
alter table cris_rp_etab2policysingle drop column authorizedSingle;
alter table cris_rp_tab2policygroup drop column authorizedGroup;
alter table cris_rp_tab2policysingle drop column authorizedSingle;
alter table cris_do_box2policygroup add column authorizedGroup_id int4 not null;
alter table cris_do_box2policysingle add column authorizedSingle_id int4 not null;
alter table cris_do_etab2policygroup add column authorizedGroup_id int4 not null;
alter table cris_do_etab2policysingle add column authorizedSingle_id int4 not null;
alter table cris_do_tab2policygroup add column authorizedGroup_id int4 not null;
alter table cris_do_tab2policysingle add column authorizedSingle_id int4 not null;
alter table cris_ou_box2policygroup add column authorizedGroup_id int4 not null;
alter table cris_ou_box2policysingle add column authorizedSingle_id int4 not null;
alter table cris_ou_etab2policygroup add column authorizedGroup_id int4 not null;
alter table cris_ou_etab2policysingle add column authorizedSingle_id int4 not null;
alter table cris_ou_tab2policygroup add column authorizedGroup_id int4 not null;
alter table cris_ou_tab2policysingle add column authorizedSingle_id int4 not null;
alter table cris_pj_box2policygroup add column authorizedGroup_id int4 not null;
alter table cris_pj_box2policysingle add column authorizedSingle_id int4 not null;
alter table cris_pj_etab2policygroup add column authorizedGroup_id int4 not null;
alter table cris_pj_etab2policysingle add column authorizedSingle_id int4 not null;
alter table cris_pj_tab2policygroup add column authorizedGroup_id int4 not null;
alter table cris_pj_tab2policysingle add column authorizedSingle_id int4 not null;
alter table cris_rp_box2policygroup add column authorizedGroup_id int4 not null;
alter table cris_rp_box2policysingle add column authorizedSingle_id int4 not null;
alter table cris_rp_etab2policygroup add column authorizedGroup_id int4 not null;
alter table cris_rp_etab2policysingle add column authorizedSingle_id int4 not null;
alter table cris_rp_tab2policygroup add column authorizedGroup_id int4 not null;
alter table cris_rp_tab2policysingle add column authorizedSingle_id int4 not null;
alter table cris_do_box2policygroup add constraint FK_ncnj9wab3w3dtuttmnqo7wmkg foreign key (authorizedGroup_id) references cris_do_pdef;
alter table cris_do_box2policysingle add constraint FK_a6bmprm8xwlvj8bqmdptt25s8 foreign key (authorizedSingle_id) references cris_do_pdef;
alter table cris_do_etab2policygroup add constraint FK_jt6tpe3s3do23qq5uv3tqm6om foreign key (authorizedGroup_id) references cris_do_pdef;
alter table cris_do_etab2policysingle add constraint FK_1yyg2way754sn9exp6v2c2giu foreign key (authorizedSingle_id) references cris_do_pdef;
alter table cris_do_tab2policygroup add constraint FK_kbekx9xwdejdvh44ajdt27mji foreign key (authorizedGroup_id) references cris_do_pdef;
alter table cris_do_tab2policysingle add constraint FK_h8v99o1641du8hrkjfevb16kv foreign key (authorizedSingle_id) references cris_do_pdef;
alter table cris_ou_box2policygroup add constraint FK_6v9y4kkw2xll05dr4cetu0ar foreign key (authorizedGroup_id) references cris_ou_pdef;
alter table cris_ou_box2policysingle add constraint FK_kksu0lkl54lfyo7avfw9q2e3m foreign key (authorizedSingle_id) references cris_ou_pdef;
alter table cris_ou_etab2policygroup add constraint FK_n2g69plgby2vu4a10c67ebave foreign key (authorizedGroup_id) references cris_ou_pdef;
alter table cris_ou_etab2policysingle add constraint FK_dv5r0xlg2v6fomsyn31t3l9fe foreign key (authorizedSingle_id) references cris_ou_pdef;
alter table cris_ou_tab2policygroup add constraint FK_d67u4q8t4p5ld2o5urki3no77 foreign key (authorizedGroup_id) references cris_ou_pdef;
alter table cris_ou_tab2policysingle add constraint FK_iwvt61g5jik8b9btgu4rwg8a5 foreign key (authorizedSingle_id) references cris_ou_pdef;
alter table cris_pj_box2policygroup add constraint FK_fci7x9pqfchr3h3tx8v20rmy8 foreign key (authorizedGroup_id) references cris_pj_pdef;
alter table cris_pj_box2policysingle add constraint FK_536r7nnuifyq6mklu7qrmeorf foreign key (authorizedSingle_id) references cris_pj_pdef;
alter table cris_pj_etab2policygroup add constraint FK_r6h4nfpuwnb3hbmeyf7oam05u foreign key (authorizedGroup_id) references cris_pj_pdef;
alter table cris_pj_etab2policysingle add constraint FK_qprkwu1841jovxuaek7d33s1k foreign key (authorizedSingle_id) references cris_pj_pdef;
alter table cris_pj_tab2policygroup add constraint FK_qr0t60b77rk0ie9ovkakf1gm2 foreign key (authorizedGroup_id) references cris_pj_pdef;
alter table cris_pj_tab2policysingle add constraint FK_5mf8gw7mvind2ujkxs115kttp foreign key (authorizedSingle_id) references cris_pj_pdef;
alter table cris_rp_box2policygroup add constraint FK_ep9w7f4u1ntkqgsy1r95l2wuy foreign key (authorizedGroup_id) references cris_rp_pdef;
alter table cris_rp_box2policysingle add constraint FK_lesynsrx0csubq5a0sx31tv5w foreign key (authorizedSingle_id) references cris_rp_pdef;
alter table cris_rp_etab2policygroup add constraint FK_461h8h92wrvyxxxywunkopdol foreign key (authorizedGroup_id) references cris_rp_pdef;
alter table cris_rp_etab2policysingle add constraint FK_fkbp74vg34qxh8k788jey45sd foreign key (authorizedSingle_id) references cris_rp_pdef;
alter table cris_rp_tab2policygroup add constraint FK_jtscxfde3kfpxuniwnox0rvy6 foreign key (authorizedGroup_id) references cris_rp_pdef;
alter table cris_rp_tab2policysingle add constraint FK_jvw45mrhe2due2meew4jneqfc foreign key (authorizedSingle_id) references cris_rp_pdef;
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';