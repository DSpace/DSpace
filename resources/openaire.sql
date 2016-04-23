CREATE TABLE revision_token
(
  revision_token_id integer NOT NULL,
  tipo character varying(1) NOT NULL,
  email character varying(200) NOT NULL,
  token character varying(100) NOT NULL,
  handle_revisado character varying(21),
  workspace_id character varying(10),
  revision_id character varying(10),
  CONSTRAINT revision_token_id_pk PRIMARY KEY (revision_token_id),
  CONSTRAINT token_email_unique UNIQUE (email,token),
  CONSTRAINT token_unique UNIQUE (token)
);

CREATE UNIQUE INDEX token_idx
  ON revision_token   USING btree
  (revision_token_id);
  
CREATE SEQUENCE revision_token_seq;

