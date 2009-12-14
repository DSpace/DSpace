ALTER TABLE unit ADD COLUMN faculty_only BOOL;

UPDATE unit SET faculty_only=true;

