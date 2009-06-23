ALTER TABLE item ADD COLUMN views INTEGER;
ALTER TABLE bitstream ADD COLUMN views INTEGER;

UPDATE bitstream SET views=0;
UPDATE item SET views=0;
