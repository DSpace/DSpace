--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin

CREATE TABLE imp_bitstream_metadatavalue
(
  imp_bitstream_metadatavalue_id integer NOT NULL,
  imp_bitstream_id integer NOT NULL,
  imp_schema character varying(128) NOT NULL,
  imp_element character varying(128) NOT NULL,
  imp_qualifier character varying(128),
  imp_value text NOT NULL,
  imp_authority character varying(256),
  imp_confidence integer,
  imp_share integer,
  metadata_order integer NOT NULL,
  text_lang character varying(32),
  CONSTRAINT imp_bitstream_metadatavalue_id_pkey PRIMARY KEY (imp_bitstream_metadatavalue_id),
  CONSTRAINT imp_bitstream_id_mv_fkey FOREIGN KEY (imp_bitstream_id)
      REFERENCES public.imp_bitstream (imp_bitstream_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX imp_bitstream_mv_idx_impid  ON imp_bitstream_metadatavalue(imp_bitstream_id);
 CREATE SEQUENCE imp_bitstream_metadatavalue_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;
	
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
