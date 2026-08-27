--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

CREATE SEQUENCE IF NOT EXISTS proxies_seq;
create table IF NOT EXISTS proxies (proxies_id integer NOT NULL DEFAULT nextval('proxies_seq'), depositor_id uuid, proxy_id uuid, handle varchar(100));

CREATE SEQUENCE IF NOT EXISTS umrestricted_seq;
create table IF NOT EXISTS umrestricted (id integer NOT NULL DEFAULT nextval('umrestricted_seq'), item_id varchar(250), release_date varchar(250));

create sequence IF NOT EXISTS consolidatedstatstable_seq;
create table IF NOT EXISTS consolidatedstatstable (consolidatedstatstable_id integer NOT NULL DEFAULT nextval('consolidatedstatstable_seq'), colldt varchar(250), collid integer, collname text, handle varchar(250), title text, publisher text, inside_count integer, out_count integer,  bit_count integer,  bitum integer, bitnonum integer, itemuminside integer, itemnonuminside integer, itemumoutside integer, itemnonumoutside integer, bitreferer text, itemoutsidereferer text, collid_uuid character varying(250));

create table IF NOT EXISTS individual_stats (id SERIAL PRIMARY KEY, email varchar(200), enabled varchar(10));

create table IF NOT EXISTS admin_stats ( collemail varchar (200) primary key );

create table IF NOT EXISTS crawlerip ( date varchar(100), ip text, site text, ipcount integer);

create table IF NOT EXISTS all_ips ( ip varchar(30), hostname varchar(250), stat_date varchar(250));
CREATE UNIQUE INDEX IF NOT EXISTS ip_idx ON all_ips (ip);

create table IF NOT EXISTS crawlers_dspace ( ip text);

create table IF NOT EXISTS rawstats ( colldt varchar (250), total_item_view integer, total_bit_view integer);

create table IF NOT EXISTS statsdata (item_id integer, handle varchar(250), authors text, title text, dateadded text,
 numofbits       integer,
 display_authors text, 
 display_title   text, 
 publisher       text,
 collid          integer, 
 collid_uuid     varchar(250),
 item_uuid       varchar(250));


CREATE UNIQUE INDEX IF NOT EXISTS stats_date ON statsdata (dateadded);
CREATE UNIQUE INDEX IF NOT EXISTS stats_date_handle ON statsdata (handle, dateadded);
CREATE UNIQUE INDEX IF NOT EXISTS stats_handle_idx ON statsdata (handle);

create table IF NOT EXISTS bitstreamipstatsdata
(date     text,
 collid   varchar(250),
 item_id  varchar(250),
 handle   varchar(250),
 isumip   integer,
 referer  text,
 filename text);
CREATE UNIQUE INDEX IF NOT EXISTS bitip_idx ON bitstreamipstatsdata (date, collid, handle, isumip);

create table IF NOT EXISTS itemipstatsdata 
(date            text,
 collid          varchar(250),
 item_id         varchar(250),
 handle          varchar(250),
 insideindicator integer,
 isumip          integer,
 referer         text);

CREATE UNIQUE INDEX IF NOT EXISTS itemip_idx ON itemipstatsdata (date, collid, handle, isumip, insideindicator);

create table IF NOT EXISTS bitspecificdata (
 collid      integer,
 colldt      varchar(250),
 handle      varchar(250),
 filename    text,
 bitum       integer,
 bitnonum    integer,
 total       integer,
 bitreferer  text,
 collid_uuid varchar(250));

CREATE UNIQUE INDEX IF NOT EXISTS bitspecificdata_colldt_idx ON bitspecificdata (colldt);
CREATE UNIQUE INDEX IF NOT EXISTS bitspecificdata_handle_filename_idx ON bitspecificdata (handle, filename);
CREATE UNIQUE INDEX IF NOT EXISTS bitspecificdata_handle_idx ON bitspecificdata (handle);

create table IF NOT EXISTS statsidanddate (
 collid      integer, 
 colldt      varchar(250), 
 collid_uuid varchar(250));

CREATE UNIQUE INDEX IF NOT EXISTS stats_1 ON statsidanddate (collid_uuid);
CREATE UNIQUE INDEX IF NOT EXISTS stats_2 ON statsidanddate (colldt);
CREATE UNIQUE INDEX IF NOT EXISTS stats_3 ON statsidanddate (collid_uuid, colldt);

ALTER TABLE proxies ADD COLUMN uuid VARCHAR(100);






