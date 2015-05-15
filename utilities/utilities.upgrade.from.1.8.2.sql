--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'SQL_ASCII';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: piwik_report; Type: TABLE; Schema: public; Owner: dspace; Tablespace: 
--

CREATE TABLE piwik_report (
    report_id integer NOT NULL,
    eperson_id integer NOT NULL,
    item_id integer NOT NULL
);


ALTER TABLE public.piwik_report OWNER TO dspace;

--
-- Name: piwik_report_report_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE piwik_report_report_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.piwik_report_report_id_seq OWNER TO dspace;

--
-- Name: piwik_report_report_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--

ALTER SEQUENCE piwik_report_report_id_seq OWNED BY piwik_report.report_id;


--
-- Name: piwik_report_report_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dspace
--

SELECT pg_catalog.setval('piwik_report_report_id_seq', 18, true);


--
-- Name: report_id; Type: DEFAULT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY piwik_report ALTER COLUMN report_id SET DEFAULT nextval('piwik_report_report_id_seq'::regclass);


--
-- Name: piwik_report_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace: 
--

ALTER TABLE ONLY piwik_report
    ADD CONSTRAINT piwik_report_pkey PRIMARY KEY (report_id);


--
-- Name: piwik_report_eperson_id_item_id_key; Type: INDEX; Schema: public; Owner: dspace; Tablespace: 
--

CREATE UNIQUE INDEX piwik_report_eperson_id_item_id_key ON piwik_report USING btree (eperson_id, item_id);

