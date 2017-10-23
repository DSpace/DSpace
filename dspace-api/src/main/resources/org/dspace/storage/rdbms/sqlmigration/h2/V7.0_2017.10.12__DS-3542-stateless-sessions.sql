-- This adds an extra column to the eperson table where we save a salt for stateless authentication
ALTER TABLE eperson ADD session_salt varchar(16);