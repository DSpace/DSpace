ALTER TABLE eperson ALTER COLUMN netid TYPE varchar(256);
ALTER TABLE eperson ADD welcome_info varchar(30);
ALTER TABLE eperson ADD last_login varchar(30);
ALTER TABLE eperson ADD can_edit_submission_metadata BOOL;

ALTER TABLE metadatafieldregistry ALTER COLUMN element TYPE VARCHAR(128);

ALTER TABLE handle ADD url varchar(2048);
