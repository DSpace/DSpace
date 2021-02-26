--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

----------------------------------------------------------------------------------
-- DS-3277 : 'handle_id' column needs its own separate sequence, so that Handles 
-- can be minted from 'handle_seq'
----------------------------------------------------------------------------------
-- Create a new sequence for 'handle_id' column.
-- The role of this sequence is to simply provide a unique internal ID to the database.
CREATE SEQUENCE handle_id_seq;