ALTER TABLE eperson ALTER COLUMN netid TYPE varchar(256);
ALTER TABLE eperson ADD welcome_info varchar(30);
ALTER TABLE eperson ADD last_login varchar(30);
ALTER TABLE eperson ADD can_edit_submission_metadata BOOL;

ALTER TABLE metadatafieldregistry ALTER COLUMN element TYPE VARCHAR(128);

ALTER TABLE handle ADD url varchar(2048);

--
-- Name: license_definition; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE license_definition (
    license_id integer NOT NULL,
    name varchar(256) NOT NULL,
    definition varchar(256) NOT NULL,
    eperson_id integer NOT NULL,
    label_id integer NOT NULL,
    created_on timestamp NOT NULL,
    confirmation integer DEFAULT 0 NOT NULL,
    required_info varchar(64)
);


--
-- Name: license_file_download_statistic; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE license_file_download_statistic (
    transaction_id integer NOT NULL,
    eperson_id integer NOT NULL,
    bitstream_id integer NOT NULL,
    created_on timestamp NOT NULL
);


--
-- Name: license_label; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE license_label (
    label_id integer NOT NULL,
    label varchar(5) NOT NULL,
    title varchar(180),
    is_extended boolean DEFAULT false
);


-- ALTER TABLE public.license_label OWNER TO dspace;

--
-- Name: license_label_extended_mapping; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE license_label_extended_mapping (
    mapping_id integer NOT NULL,
    license_id integer NOT NULL,
    label_id integer NOT NULL
);


--
-- Name: license_resource_mapping; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE license_resource_mapping (
    mapping_id integer NOT NULL,
    bitstream_id integer NOT NULL,
    license_id integer NOT NULL,
    active boolean DEFAULT true NOT NULL
);


--
-- Name: license_resource_user_allowance; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE license_resource_user_allowance (
    transaction_id integer NOT NULL,
    eperson_id integer NOT NULL,
    mapping_id integer NOT NULL,
    created_on timestamp NOT NULL,
    token char(32)
);


--
-- Name: piwik_report; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE piwik_report (
    report_id integer NOT NULL,
    eperson_id integer NOT NULL,
    item_id integer NOT NULL
);


--
-- Name: user_metadata; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE user_metadata (
    user_metadata_id integer NOT NULL,
    eperson_id integer NOT NULL,
    metadata_key character varying(64) NOT NULL,
    metadata_value character varying(256) NOT NULL,
    transaction_id integer
);


--
-- Name: user_registration; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE user_registration (
    eperson_id integer NOT NULL,
    email varchar(256) NOT NULL,
    organization varchar(256) NOT NULL,
    confirmation boolean DEFAULT true NOT NULL
);


-- ALTER TABLE public.user_registration OWNER TO dspace;

--
-- Name: verification_token; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE verification_token (
    eperson_id integer NOT NULL,
    token varchar(48) NOT NULL,
    email varchar(64) NOT NULL
);

