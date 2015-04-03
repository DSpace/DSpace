--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: license_definition; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE license_definition (
    license_id integer NOT NULL,
    name character varying(256) NOT NULL,
    definition character varying(256) NOT NULL,
    eperson_id integer NOT NULL,
    label_id integer NOT NULL,
    created_on timestamp without time zone DEFAULT now() NOT NULL,
    confirmation integer DEFAULT 0 NOT NULL,
    required_info character varying(64)
);


ALTER TABLE public.license_definition OWNER TO dspace;

--
-- Name: license_definition_license_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE license_definition_license_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.license_definition_license_id_seq OWNER TO dspace;

--
-- Name: license_definition_license_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--

ALTER SEQUENCE license_definition_license_id_seq OWNED BY license_definition.license_id;


--
-- Name: license_file_download_statistic; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE license_file_download_statistic (
    transaction_id integer NOT NULL,
    eperson_id integer NOT NULL,
    bitstream_id integer NOT NULL,
    created_on timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.license_file_download_statistic OWNER TO dspace;

--
-- Name: license_file_download_statistic_transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE license_file_download_statistic_transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.license_file_download_statistic_transaction_id_seq OWNER TO dspace;

--
-- Name: license_file_download_statistic_transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--

ALTER SEQUENCE license_file_download_statistic_transaction_id_seq OWNED BY license_file_download_statistic.transaction_id;


--
-- Name: license_label; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE license_label (
    label_id integer NOT NULL,
    label character varying(5) NOT NULL,
    title character varying(180) DEFAULT NULL::character varying,
    is_extended boolean DEFAULT false
);


ALTER TABLE public.license_label OWNER TO dspace;

--
-- Name: license_label_label_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE license_label_label_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.license_label_label_id_seq OWNER TO dspace;

--
-- Name: license_label_label_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--

ALTER SEQUENCE license_label_label_id_seq OWNED BY license_label.label_id;


--
-- Name: license_resource_mapping; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE license_resource_mapping (
    mapping_id integer NOT NULL,
    bitstream_id integer NOT NULL,
    license_id integer NOT NULL,
    active boolean DEFAULT true NOT NULL
);


ALTER TABLE public.license_resource_mapping OWNER TO dspace;

--
-- Name: license_resource_mapping_mapping_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE license_resource_mapping_mapping_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.license_resource_mapping_mapping_id_seq OWNER TO dspace;

--
-- Name: license_resource_mapping_mapping_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--

ALTER SEQUENCE license_resource_mapping_mapping_id_seq OWNED BY license_resource_mapping.mapping_id;


--
-- Name: license_resource_user_allowance; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE license_resource_user_allowance (
    transaction_id integer NOT NULL,
    eperson_id integer NOT NULL,
    mapping_id integer NOT NULL,
    created_on timestamp without time zone DEFAULT now() NOT NULL,
    token character(32)
);

ALTER TABLE public.license_resource_user_allowance OWNER TO dspace;

--
-- Name: license_resource_user_allowance_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace: 
--

ALTER TABLE ONLY license_resource_user_allowance
    ADD CONSTRAINT license_resource_user_allowance_pkey PRIMARY KEY (transaction_id);


--
-- Name: license_resource_user_allowance_transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE license_resource_user_allowance_transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.license_resource_user_allowance_transaction_id_seq OWNER TO dspace;

--
-- Name: license_resource_user_allowance_transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--

ALTER SEQUENCE license_resource_user_allowance_transaction_id_seq OWNED BY license_resource_user_allowance.transaction_id;

--
-- Name: user_registration; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE user_registration (
    eperson_id integer NOT NULL,
    email character varying(256) NOT NULL,
    organization character varying(256) NOT NULL,
    confirmation boolean DEFAULT true NOT NULL
);


ALTER TABLE public.user_registration OWNER TO dspace;

--
-- Name: user_registration_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace: 
--

ALTER TABLE ONLY user_registration
    ADD CONSTRAINT user_registration_pkey PRIMARY KEY (eperson_id);




--
-- Name: verification_token; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE verification_token (
    eperson_id integer UNIQUE NOT NULL,
    token character varying(48) UNIQUE PRIMARY KEY NOT NULL,
    email character varying(64) UNIQUE NOT NULL
);


ALTER TABLE public.verification_token OWNER TO dspace;

--
-- Name: user_metadata; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE user_metadata
(
  user_metadata_id serial NOT NULL,
  eperson_id integer NOT NULL,
  metadata_key character varying(64) NOT NULL,
  metadata_value character varying(256) NOT NULL,
  transaction_id integer,
  CONSTRAINT user_metadata_pkey PRIMARY KEY (user_metadata_id),
  CONSTRAINT license_resource_user_allowance_user_metadata_fk FOREIGN KEY (transaction_id)
      REFERENCES license_resource_user_allowance (transaction_id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT user_registration_user_metadata_fk FOREIGN KEY (eperson_id)
      REFERENCES user_registration (eperson_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public.user_metadata
  OWNER TO dspace;
  
--
-- Name: license_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY license_definition ALTER COLUMN license_id SET DEFAULT nextval('license_definition_license_id_seq'::regclass);


--
-- Name: transaction_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY license_file_download_statistic ALTER COLUMN transaction_id SET DEFAULT nextval('license_file_download_statistic_transaction_id_seq'::regclass);


--
-- Name: label_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY license_label ALTER COLUMN label_id SET DEFAULT nextval('license_label_label_id_seq'::regclass);


--
-- Name: mapping_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY license_resource_mapping ALTER COLUMN mapping_id SET DEFAULT nextval('license_resource_mapping_mapping_id_seq'::regclass);


--
-- Name: transaction_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY license_resource_user_allowance ALTER COLUMN transaction_id SET DEFAULT nextval('license_resource_user_allowance_transaction_id_seq'::regclass);

--
-- Data for Name: license_definition; Type: TABLE DATA; Schema: public; Owner: dspace
--

select setval('license_definition_license_id_seq', 10);

COPY license_definition (license_id, name, definition, eperson_id, label_id, created_on, confirmation) FROM stdin;
8	Creative Commons - Attribution 3.0 Unported (CC BY 3.0)	http://creativecommons.org/licenses/by/3.0/	1	4	2011-12-14 23:05:10.936108	0
7	PDT 2.0 License	http://ufal.mff.cuni.cz/pdt2.0/doc/pdt-guide/en/html/ch07.html	1	2	2011-12-14 21:38:22.407755	2
9	Super Cool License	http://www.google.com	1	2	2012-02-13 22:14:54.370206	2
6	Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0)	http://creativecommons.org/licenses/by-sa/3.0/	1	4	2011-12-14 21:33:12.100304	0
5	Attribution-NoDerivs 3.0 Unported (CC BY-ND 3.0)	http://creativecommons.org/licenses/by-nd/3.0/	1	4	2011-12-14 21:32:39.21528	0
4	Attribution-NonCommercial-NoDerivs 3.0 Unported (CC BY-NC-ND 3.0)	http://creativecommons.org/licenses/by-nc-nd/3.0/	1	4	2011-12-14 21:32:21.586806	0
2	Attribution-NonCommercial-ShareAlike 3.0 Unported (CC BY-NC-SA 3.0)	http://creativecommons.org/licenses/by-nc-sa/3.0/	1	4	2011-12-14 21:30:29.776313	0
1	Attribution-NonCommercial 3.0 Unported (CC BY-NC 3.0)	http://creativecommons.org/licenses/by-nc/3.0/	1	4	2011-12-14 21:29:38.719044	0
\.

--
-- Data for Name: license_label; Type: TABLE DATA; Schema: public; Owner: dspace
--

COPY license_label (label_id, label, title) FROM stdin;
1	PUB	Publicly Available
4	CC	Distributed under Creative Commons
2	ACA	Academic Use
3	RES	Restricted Use
\.

--
-- Data for Name: user_registration; Type: TABLE DATA; Schema: public; Owner: dspace
--

COPY user_registration (eperson_id, email, organization, confirmation) FROM stdin;
0	anonymous	anonymous	t
1	administrator	administrator	t
\.

--
-- Name: license_definition_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace: 
--

ALTER TABLE ONLY license_definition
    ADD CONSTRAINT license_definition_pkey PRIMARY KEY (license_id);


--
-- Name: license_file_download_statistic_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace: 
--

ALTER TABLE ONLY license_file_download_statistic
    ADD CONSTRAINT license_file_download_statistic_pkey PRIMARY KEY (transaction_id);


--
-- Name: license_label_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace: 
--

ALTER TABLE ONLY license_label
    ADD CONSTRAINT license_label_pkey PRIMARY KEY (label_id);


--
-- Name: license_resource_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace: 
--

ALTER TABLE ONLY license_resource_mapping
    ADD CONSTRAINT license_resource_mapping_pkey PRIMARY KEY (mapping_id);

--
-- Name: license_definition_license_id_key; Type: INDEX; Schema: public; Owner: dspace; Tablespace: 
--

CREATE UNIQUE INDEX license_definition_license_id_key ON license_definition USING btree (name);

--
-- Name: user_registration_idx; Type: INDEX; Schema: public; Owner: dspace; Tablespace: 
--
-- Unique constraint turned off (BUG # 400)
-- CREATE UNIQUE INDEX user_registration_idx ON user_registration USING btree (email);


--
-- Name: license_definition_license_resource_mapping_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY license_resource_mapping
    ADD CONSTRAINT license_definition_license_resource_mapping_fk FOREIGN KEY (license_id) REFERENCES license_definition(license_id) ON DELETE CASCADE;


--
-- Name: license_label_license_definition_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY license_definition
    ADD CONSTRAINT license_label_license_definition_fk FOREIGN KEY (label_id) REFERENCES license_label(label_id);


--
-- Name: license_resource_mapping_license_resource_user_allowance_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY license_resource_user_allowance
    ADD CONSTRAINT license_resource_mapping_license_resource_user_allowance_fk FOREIGN KEY (mapping_id) REFERENCES license_resource_mapping(mapping_id) ON UPDATE CASCADE ON DELETE CASCADE;

--
-- Name: user_registration_license_definition_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY license_definition
    ADD CONSTRAINT user_registration_license_definition_fk FOREIGN KEY (eperson_id) REFERENCES user_registration(eperson_id);


--
-- Name: user_registration_license_file_download_statistic_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY license_file_download_statistic
    ADD CONSTRAINT user_registration_license_file_download_statistic_fk FOREIGN KEY (eperson_id) REFERENCES user_registration(eperson_id);


--
-- Name: user_registration_license_resource_user_allowance_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY license_resource_user_allowance
    ADD CONSTRAINT user_registration_license_resource_user_allowance_fk FOREIGN KEY (eperson_id) REFERENCES user_registration(eperson_id);


--
-- Name: license_label_extended_mapping; Type: TABLE; Schema: public; Owner: dspace; Tablespace:
--

CREATE TABLE license_label_extended_mapping (
    mapping_id integer NOT NULL,
    license_id integer NOT NULL,
    label_id integer NOT NULL
);


ALTER TABLE public.license_label_extended_mapping OWNER TO dspace;

--
-- Name: license_label_extended_mapping_mapping_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE license_label_extended_mapping_mapping_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.license_label_extended_mapping_mapping_id_seq OWNER TO dspace;

--
-- Name: license_label_extended_mapping_mapping_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--

ALTER SEQUENCE license_label_extended_mapping_mapping_id_seq OWNED BY license_label_extended_mapping.mapping_id;

ALTER TABLE ONLY license_label_extended_mapping ALTER COLUMN mapping_id SET DEFAULT nextval('license_label_extended_mapping_mapping_id_seq'::regclass);

ALTER TABLE ONLY license_label_extended_mapping
    ADD CONSTRAINT license_label_extended_mapping_pkey PRIMARY KEY (mapping_id);

ALTER TABLE ONLY license_label_extended_mapping
    ADD CONSTRAINT license_definition_license_label_extended_mapping_fk FOREIGN KEY (license_id) REFERENCES license_definition(license_id) ON DELETE CASCADE;

ALTER TABLE ONLY license_label_extended_mapping
    ADD CONSTRAINT license_label_license_label_extended_mapping_fk FOREIGN KEY (label_id) REFERENCES license_label(label_id) ON DELETE CASCADE;

INSERT INTO license_label values(5, 'BY', 'Attribution Required', true);
INSERT INTO license_label values(6,'SA', 'Share Alike',true);
INSERT INTO license_label values(7,'NC','Noncommercial',true);
INSERT INTO license_label values(8,'ND','No Derivative Works',true);
INSERT INTO license_label values(9,'Inf','Inform Before Use',true);
INSERT INTO license_label values(10,'ReD','Redeposit Modified',true);

select setval('license_label_label_id_seq ',11);


select setval('license_label_extended_mapping_mapping_id_seq', 25);


COPY license_label_extended_mapping (mapping_id, license_id, label_id) FROM stdin;
12	1	7
13	1	5
14	2	6
15	2	5
16	2	7
17	4	5
18	4	8
19	4	7
20	5	8
21	5	5
22	6	5
23	6	6
24	8	5
\.

--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--


-- from licenses.2014.12.17.sql
-- See #74 in lindat-repository
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('Affero General Public License 1 (AGPL-1.0)','http://www.affero.org/oagpl.html','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('Affero General Public License 3 (AGPL-3.0)','http://opensource.org/licenses/AGPL-3.0','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('Common Development and Distribution License (CDDL-1.0)','http://opensource.org/licenses/CDDL-1.0','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('Eclipse Public License 1.0 (EPL-1.0)','http://opensource.org/licenses/EPL-1.0','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('GNU General Public License 2 or later (GPL-2.0)','http://opensource.org/licenses/GPL-2.0','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('GNU Library or "Lesser" General Public License 2.1 (LGPL-2.1)','http://opensource.org/licenses/LGPL-2.1','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('GNU Library or "Lesser" General Public License 2.1 or later (LGPL-2.1)','http://opensource.org/licenses/LGPL-2.1','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('GNU Library or "Lesser" General Public License 3.0 (LGPL-3.0)','http://opensource.org/licenses/LGPL-3.0','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('Mozilla Public License 2.0','http://opensource.org/licenses/MPL-2.0','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('Open Data Commons Attribution License (ODC-By)','http://opendatacommons.org/licenses/by/summary/','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('Open Data Commons Open Database License (ODbL)','http://opendatacommons.org/licenses/odbl/summary/','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('Open Data Commons Public Domain Dedication and License (PDDL)','http://opendatacommons.org/licenses/pddl/summary/','1','1','2014-12-17 14:05:00','0');
INSERT INTO license_definition (name, definition, eperson_id, label_id, created_on, confirmation) VALUES ('Public Domain Mark (PD)','http://creativecommons.org/publicdomain/mark/1.0/','1','1','2014-12-17 14:05:00','0');
UPDATE license_definition SET name='Public Domain Dedication (CC Zero)',definition='http://creativecommons.org/publicdomain/zero/1.0/' where name ='CC0-No Rights Reserved';
