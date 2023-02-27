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


CREATE SEQUENCE if NOT EXISTS subscription_parameter_seq;
-----------------------------------------------------------------------------------
-- ADD table subscription_parameter
-----------------------------------------------------------------------------------
CREATE TABLE if NOT EXISTS subscription_parameter
(
  subscription_parameter_id  INTEGER NOT NULL,
  name    CHARACTER VARYING(255),
  value  CHARACTER VARYING(255),
  subscription_id     INTEGER  NOT NULL,
  CONSTRAINT subscription_parameter_pkey PRIMARY KEY (subscription_parameter_id),
  CONSTRAINT subscription_parameter_subscription_fkey  FOREIGN KEY  (subscription_id) REFERENCES subscription (subscription_id)  ON DELETE CASCADE
);
--
ALTER TABLE subscription ADD COLUMN if NOT EXISTS dspace_object_id UUID;
-- --
ALTER TABLE subscription ADD COLUMN if NOT EXISTS type CHARACTER VARYING(255);
---- --
ALTER TABLE subscription DROP CONSTRAINT IF EXISTS subscription_dspaceobject_fkey;
ALTER TABLE subscription ADD CONSTRAINT subscription_dspaceobject_fkey FOREIGN KEY (dspace_object_id) REFERENCES dspaceobject (uuid);
--
UPDATE subscription SET dspace_object_id = collection_id , type = 'content';
--
ALTER TABLE subscription DROP CONSTRAINT IF EXISTS subscription_collection_id_fkey;
-- --
ALTER TABLE subscription DROP COLUMN IF EXISTS collection_id;
-- --
INSERT INTO subscription_parameter (subscription_parameter_id, name, value, subscription_id)
SELECT getnextid('subscription_parameter'), 'frequency', 'D', subscription_id from "subscription" ;

