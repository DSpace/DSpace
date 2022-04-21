-- CREATE SEQUENCE "openurltracker_seq" start with 1 increment by 1 nocache nocycle noorder;
--
-- CREATE table "OpenUrlTracker" (
--     "tracker_id"  NUMBER NOT NULL,
--     "tracker_url" VARCHAR2(255) NOT NULL,
--     "uploaddate"  DATE,
--     constraint  "OpenUrlTracker_PK" primary key ("tracker_id")
-- );
--
-- CREATE trigger "BI_OpenUrlTracker"
--   before insert on "OpenUrlTracker"
--   for each row
-- begin
--   if :NEW."tracker_id" is null then
--     select "openurltracker_seq".nextval into :NEW."tracker_id" from dual;
--   end if;
-- end;

CREATE SEQUENCE openurltracker_seq;

CREATE TABLE OpenUrlTracker
(
    tracker_id NUMBER,
    tracker_url VARCHAR2(1000),
    uploaddate DATE,
    CONSTRAINT  OpenUrlTracker_PK PRIMARY KEY (tracker_id)
);
