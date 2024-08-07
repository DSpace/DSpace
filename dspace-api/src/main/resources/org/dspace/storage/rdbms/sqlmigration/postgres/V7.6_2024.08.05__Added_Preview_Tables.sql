--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--
-- Name: previewcontent; Type: TABLE; Schema: public; Owner: dspace; Tablespace:
--

CREATE TABLE previewcontent (
   previewcontent_id integer NOT NULL,
   bitstream_id uuid NOT NULL,
   name varchar(2000),
   content varchar(2000),
   isDirectory boolean DEFAULT false,
   size varchar(256)
);

ALTER TABLE public.previewcontent OWNER TO dspace;

--
-- Name: previewcontent_previewcontent_id_seq; Type: SEQUENCE; Schema: public; Owner: dspace
--

CREATE SEQUENCE previewcontent_previewcontent_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

ALTER TABLE public.previewcontent_previewcontent_id_seq OWNER TO dspace;

--
-- Name: previewcontent_previewcontent_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dspace
--

ALTER SEQUENCE previewcontent_previewcontent_id_seq OWNED BY previewcontent.previewcontent_id;

--
-- Name: previewcontent_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace:
--

ALTER TABLE ONLY previewcontent
    ADD CONSTRAINT previewcontent_pkey PRIMARY KEY (previewcontent_id);

--
-- Name: previewcontent_bitstream_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE ONLY previewcontent
    ADD CONSTRAINT previewcontent_bitstream_fk FOREIGN KEY (bitstream_id) REFERENCES bitstream(uuid) ON DELETE CASCADE;

--
-- Name: preview2preview; Type: TABLE; Schema: public; Owner: dspace; Tablespace:
--

CREATE TABLE preview2preview (
    parent_id integer NOT NULL,
    child_id integer NOT NULL,
    name varchar(2000)
);

ALTER TABLE public.preview2preview OWNER TO dspace;

--
-- Name: preview2preview_pkey; Type: CONSTRAINT; Schema: public; Owner: dspace; Tablespace:
--

ALTER TABLE preview2preview
    ADD CONSTRAINT preview2preview_pkey PRIMARY KEY (parent_id, child_id);

--
-- Name: preview2preview_parent_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE preview2preview
    ADD CONSTRAINT preview2preview_parent_fk FOREIGN KEY (parent_id) REFERENCES previewcontent(previewcontent_id) ON DELETE CASCADE;

--
-- Name: preview2preview_child_fk; Type: FK CONSTRAINT; Schema: public; Owner: dspace
--

ALTER TABLE preview2preview
    ADD CONSTRAINT preview2preview_child_fk FOREIGN KEY (child_id) REFERENCES previewcontent(previewcontent_id) ON DELETE CASCADE;
