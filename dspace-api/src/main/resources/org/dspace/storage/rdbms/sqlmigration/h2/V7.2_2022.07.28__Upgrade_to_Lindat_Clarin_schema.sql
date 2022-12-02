--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- HANDLE TABLE
ALTER TABLE handle ADD url varchar(2048);
ALTER TABLE handle ADD dead BOOL;
ALTER TABLE handle ADD dead_since TIMESTAMP;


-- LICENSES
--
-- Name: license_definition; Type: TABLE; Schema: public; Owner: dspace; Tablespace:
--

CREATE TABLE license_definition (
    license_id integer NOT NULL,
    name varchar(256),
    definition varchar(256),
    user_registration_id integer,
    label_id integer,
    created_on timestamp,
    confirmation integer DEFAULT 0,
    required_info varchar(256)
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
    is_extended boolean DEFAULT false,
    icon bytea
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
    bitstream_uuid uuid,
    license_id integer
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
    user_registration_id integer,
    mapping_id integer,
    created_on timestamp,
    token varchar(256)
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
-- Name: user_registration; Type: TABLE; Schema: public; Owner: dspace; Tablespace:
--

CREATE TABLE user_registration (
    user_registration_id integer NOT NULL,
    eperson_id UUID,
    email varchar(256),
    organization varchar(256),
    confirmation boolean DEFAULT true
);

CREATE SEQUENCE user_registration_user_registration_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- Name: user_metadata; Type: TABLE; Schema: public; Owner: dspace; Tablespace:
--

CREATE TABLE user_metadata (
    user_metadata_id integer NOT NULL,
    user_registration_id integer,
    metadata_key character varying(64),
    metadata_value character varying(256),
    transaction_id integer
);

--
-- Name: user_metadata_user_metadata_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE user_metadata_user_metadata_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

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
-- Name: user_metadata_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE user_metadata ALTER COLUMN user_metadata_id SET DEFAULT nextval('user_metadata_user_metadata_id_seq');

--
-- Name: user_registration_id; Type: DEFAULT; Schema: public; Owner: dspace
--

--ALTER TABLE user_registration ALTER COLUMN eperson_id SET DEFAULT nextval('user_registration_eperson_id_seq');

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
-- Name: user_registration_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace:
--

ALTER TABLE user_registration
    ADD CONSTRAINT user_registration_pkey PRIMARY KEY (user_registration_id);

--
-- Name: user_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace:
--

ALTER TABLE user_metadata
    ADD CONSTRAINT user_metadata_pkey PRIMARY KEY (user_metadata_id);

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


ALTER TABLE license_resource_mapping
    ADD CONSTRAINT bitstream_license_resource_mapping_fk FOREIGN KEY (bitstream_uuid) REFERENCES bitstream(uuid) ON DELETE CASCADE;

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

--
-- Name: user_registration_license_definition_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE license_definition
    ADD CONSTRAINT user_registration_license_definition_fk FOREIGN KEY (user_registration_id) REFERENCES user_registration(user_registration_id);

--
-- Name: user_registration_license_resource_user_allowance_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE license_resource_user_allowance
    ADD CONSTRAINT user_registration_license_resource_user_allowance_fk FOREIGN KEY (user_registration_id) REFERENCES user_registration(user_registration_id);

--
-- Name: license_resource_user_allowance_user_metadata_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE user_metadata
    ADD CONSTRAINT license_resource_user_allowance_user_metadata_fk FOREIGN KEY (transaction_id) REFERENCES license_resource_user_allowance(transaction_id) ON UPDATE CASCADE ON DELETE CASCADE;

--
-- Name: user_registration_user_metadata_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE user_metadata
    ADD CONSTRAINT user_registration_user_metadata_fk FOREIGN KEY (user_registration_id) REFERENCES user_registration(user_registration_id);
