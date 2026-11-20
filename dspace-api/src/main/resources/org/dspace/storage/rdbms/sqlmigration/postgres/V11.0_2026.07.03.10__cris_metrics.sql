--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- CRIS Metrics - squashed idempotent migration
-- Consolidates: V7.0_2020.11.19 (create), V7.0_2020.12.01 (delta/rank columns),
--   V7.0_2020.12.09 (fk performance fix), V7.0_2021.09.03 (fk to dspaceobject)
--
-- The table name is unchanged between legacy CRIS and this port, so no rename or
-- data copy is required. CREATE ... IF NOT EXISTS keeps the migration safe on a
-- CRIS-base database (table already present in final shape) and on a fresh
-- install. The final foreign key targets dspaceobject(uuid) without ON DELETE
-- CASCADE, matching the cumulative effect of the 7.x CRIS migrations.
-------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS cris_metrics_seq;

CREATE TABLE IF NOT EXISTS cris_metrics
(
    id              INTEGER NOT NULL,
    metricType      CHARACTER VARYING(255),
    metricCount     FLOAT,
    acquisitionDate TIMESTAMP,
    startDate       TIMESTAMP,
    endDate         TIMESTAMP,
    resource_id     UUID NOT NULL,
    last            BOOLEAN,
    remark          TEXT,
    deltaPeriod1    FLOAT,
    deltaPeriod2    FLOAT,
    rank            FLOAT,
    CONSTRAINT cris_metrics_pkey PRIMARY KEY (id),
    CONSTRAINT cris_metrics_resource_id_fkey FOREIGN KEY (resource_id)
        REFERENCES dspaceobject (uuid)
);

CREATE INDEX IF NOT EXISTS metrics_last_idx
    ON cris_metrics USING btree (last);

CREATE INDEX IF NOT EXISTS metrics_uuid_idx
    ON cris_metrics USING btree (resource_id);

CREATE INDEX IF NOT EXISTS metric_bid_idx
    ON cris_metrics USING btree (resource_id, metricType COLLATE pg_catalog."default");
