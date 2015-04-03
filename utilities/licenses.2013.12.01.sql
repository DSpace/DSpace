--
-- Name: user_metadata; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE user_metadata (
    eperson_id_key character varying(256) UNIQUE PRIMARY KEY NOT NULL,
    eperson_id integer NOT NULL,
    metadata_key character varying(64) NOT NULL,
    metadata_value character varying(256) NOT NULL
);


ALTER TABLE public.user_metadata OWNER TO dspace;
