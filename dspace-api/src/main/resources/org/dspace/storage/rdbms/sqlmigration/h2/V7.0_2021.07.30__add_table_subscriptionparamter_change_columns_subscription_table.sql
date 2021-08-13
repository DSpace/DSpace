--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- ADD table subscription_parameter
-----------------------------------------------------------------------------------


CREATE SEQUENCE subscription_parameter_seq;
-------------------------------------------------------
-- Create the subscription_parameter table
-------------------------------------------------------

CREATE TABLE  subscription_parameter
(
  subscription_parameter_id  INTEGER NOT NULL,
  name    CHARACTER VARYING(255),
  value  CHARACTER VARYING(255),
  subscription_id     INTEGER  NOT NULL,
  CONSTRAINT subscription_parameter_pkey PRIMARY KEY (subscription_parameter_id),
  CONSTRAINT subscription_parameter_subscription_fkey  FOREIGN KEY  (subscription_id) REFERENCES subscription (subscription_id) ON DELETE CASCADE
);
-- --
ALTER TABLE subscription DROP CONSTRAINT Subscription_collection_id_fk;
--
ALTER TABLE subscription DROP COLUMN collection_id;
--
ALTER TABLE subscription ADD COLUMN dspace_object_id UUID;
--
ALTER TABLE subscription ADD COLUMN type CHARACTER VARYING(255);
--
ALTER TABLE subscription ADD CONSTRAINT subscription_dspaceobject_fkey FOREIGN KEY (dspace_object_id) REFERENCES dspaceobject (uuid);




