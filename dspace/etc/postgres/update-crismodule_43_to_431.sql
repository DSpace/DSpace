ALTER TABLE jdyna_values ADD COLUMN booleanValue bool;
create table jdyna_widget_boolean (id int4 not null, showAsType varchar(255), checked bool, hideWhenFalse bool, primary key (id));
create table jdyna_widget_checkradio (id int4 not null, option4row integer, staticValues text, query varchar(255), primary key (id));
