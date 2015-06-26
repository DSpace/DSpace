ALTER TABLE jdyna_values ADD booleanValue number(1,0);
create table jdyna_widget_boolean (id number(10,0) not null, showAsType varchar2(255), checked number(1,0), hideWhenFalse number(1,0), primary key (id));
create table jdyna_widget_checkradio (id number(10,0) not null, option4row number(10,0), staticValues text, query varchar2(255), primary key (id));

-- Table to mantain the potential match between item and rp --
CREATE TABLE potentialmatches
(
   potentialmatches_id integer, 
   item_id integer, 
   rp VARCHAR2(20 BYTE),
   pending number(1),
    PRIMARY KEY (potentialmatches_id)
);
CREATE SEQUENCE potentialmatches_seq;
CREATE INDEX rp_idx ON potentialmatches (rp);
CREATE INDEX pending_idx ON potentialmatches (pending);
-- END potential matches --