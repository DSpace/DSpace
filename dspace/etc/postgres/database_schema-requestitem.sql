CREATE SEQUENCE requestitem_seq;

-------------------------------------------------------
-- RequestItem table
-------------------------------------------------------
CREATE TABLE requestitem
(
  requestitem_id int4 NOT NULL,
  token varchar(48),
  item_id int4,
  bitstream_id int4,
  allfiles bool,
  request_email varchar(64),
  request_name varchar(64),
  request_date timestamp,
  accept_request bool,
  decision_date timestamp,
  expires timestamp,
  CONSTRAINT requestitem_pkey PRIMARY KEY (requestitem_id),
  CONSTRAINT requestitem_token_key UNIQUE (token)
)  WITH OIDS;