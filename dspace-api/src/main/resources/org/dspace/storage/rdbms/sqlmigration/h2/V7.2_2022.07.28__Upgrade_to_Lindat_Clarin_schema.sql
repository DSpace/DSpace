--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- HANDLE TABLE
ALTER TABLE handle ADD url varchar(2048);


-- LICENSES
--
-- Name: license_definition; Type: TABLE; Schema: public; Owner: dspace; Tablespace:
--

CREATE TABLE license_definition (
    license_id integer NOT NULL,
    name varchar(256),
    definition varchar(256),
    eperson_id integer,
    label_id integer,
    created_on timestamp,
    confirmation integer DEFAULT 0,
    required_info varchar(64)
);

--
-- Name: license_definition_license_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE license_definition_license_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- Name: license_definition_license_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--

--
-- Name: license_label; Type: TABLE; Schema: public; Owner: dspace; Tablespace:
--

CREATE TABLE license_label (
    label_id integer NOT NULL,
    label varchar(5),
    title varchar(180),
    is_extended boolean DEFAULT false
);


--
-- Name: license_label_extended_mapping; Type: TABLE; Schema: public; Owner: dspace; Tablespace:
--

CREATE TABLE license_label_extended_mapping (
    mapping_id integer NOT NULL,
    license_id integer,
    label_id integer
);

--
-- Name: license_label_extended_mapping_mapping_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE license_label_extended_mapping_mapping_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Name: license_label_extended_mapping_mapping_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--

--
-- Name: license_label_extended_mapping_mapping_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dspace
--

--
-- Name: license_label_label_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE license_label_label_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


-- Name: license_label_label_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace



--
-- Name: license_label_label_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dspace
--

--
-- Name: license_resource_mapping; Type: TABLE; Schema: public; Owner: dspace; Tablespace:
--

CREATE TABLE license_resource_mapping (
    mapping_id integer NOT NULL,
    bitstream_id integer,
    license_id integer,
    active boolean DEFAULT true
);


--
-- Name: license_resource_mapping_mapping_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE license_resource_mapping_mapping_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Name: license_resource_mapping_mapping_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--


--
-- Name: license_resource_mapping_mapping_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dspace
--

--
-- Name: license_resource_user_allowance; Type: TABLE; Schema: public; Owner: dspace; Tablespace:
--

CREATE TABLE license_resource_user_allowance (
    transaction_id integer NOT NULL,
    eperson_id integer,
    mapping_id integer,
    created_on timestamp,
    token char(32)
);

--
-- Name: license_resource_user_allowance_transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE license_resource_user_allowance_transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Name: license_resource_user_allowance_transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--


--
-- Name: license_resource_user_allowance_transaction_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dspace
--

--
-- Name: license_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE license_definition ALTER COLUMN license_id SET DEFAULT nextval('license_definition_license_id_seq');


--
-- Name: label_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE license_label ALTER COLUMN label_id SET DEFAULT nextval('license_label_label_id_seq');


--
-- Name: mapping_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE license_label_extended_mapping ALTER COLUMN mapping_id SET DEFAULT nextval('license_label_extended_mapping_mapping_id_seq');


--
-- Name: mapping_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE license_resource_mapping ALTER COLUMN mapping_id SET DEFAULT nextval('license_resource_mapping_mapping_id_seq');


--
-- Name: transaction_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE license_resource_user_allowance ALTER COLUMN transaction_id SET DEFAULT nextval('license_resource_user_allowance_transaction_id_seq');

--
-- Name: license_definition_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace:
--

ALTER TABLE license_definition
    ADD CONSTRAINT license_definition_pkey PRIMARY KEY (license_id);


--
-- Name: license_label_extended_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace:
--

ALTER TABLE license_label_extended_mapping
    ADD CONSTRAINT license_label_extended_mapping_pkey PRIMARY KEY (mapping_id);


--
-- Name: license_label_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace:
--

ALTER TABLE license_label
    ADD CONSTRAINT license_label_pkey PRIMARY KEY (label_id);


--
-- Name: license_resource_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace:
--

ALTER TABLE license_resource_mapping
    ADD CONSTRAINT license_resource_mapping_pkey PRIMARY KEY (mapping_id);


--
-- Name: license_resource_user_allowance_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace:
--

ALTER TABLE license_resource_user_allowance
    ADD CONSTRAINT license_resource_user_allowance_pkey PRIMARY KEY (transaction_id);



--
-- Name: license_definition_license_label_extended_mapping_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE license_label_extended_mapping
    ADD CONSTRAINT license_definition_license_label_extended_mapping_fk FOREIGN KEY (license_id) REFERENCES license_definition(license_id) ON DELETE CASCADE;


--
-- Name: license_definition_license_resource_mapping_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE license_resource_mapping
    ADD CONSTRAINT license_definition_license_resource_mapping_fk FOREIGN KEY (license_id) REFERENCES license_definition(license_id) ON DELETE CASCADE;


--
-- Name: license_label_license_definition_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

--ALTER TABLE license_definition
--    ADD CONSTRAINT license_label_license_definition_fk FOREIGN KEY (label_id) REFERENCES license_label(label_id);


--
-- Name: license_label_license_label_extended_mapping_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE license_label_extended_mapping
    ADD CONSTRAINT license_label_license_label_extended_mapping_fk FOREIGN KEY (label_id) REFERENCES license_label(label_id) ON DELETE CASCADE;


--
-- Name: license_resource_mapping_license_resource_user_allowance_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE license_resource_user_allowance
    ADD CONSTRAINT license_resource_mapping_license_resource_user_allowance_fk FOREIGN KEY (mapping_id) REFERENCES license_resource_mapping(mapping_id) ON UPDATE CASCADE ON DELETE CASCADE;
