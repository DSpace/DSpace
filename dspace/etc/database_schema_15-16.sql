CREATE TABLE uuid (
    uuid VARCHAR(36) not null,
    resource_type integer,
    resource_id integer
);

CREATE INDEX uuid_idx ON uuid (uuid);