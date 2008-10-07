ALTER TABLE bitstreamformatregistry ADD COLUMN x varchar(128);

UPDATE bitstreamformatregistry SET x = mimetype;

ALTER TABLE bitstreamformatregistry DROP COLUMN mimetype;

ALTER TABLE bitstreamformatregistry RENAME COLUMN x TO mimetype;
