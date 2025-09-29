--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- =======================================================================
--  DSpace Database Migration
--  Version: 8.0
--  Date: 2025-09-29
--  Description: Create table to persist curation task queue entries migrated
--               from previous file-based implementation (DBTaskQueue).
--               Each row represents a TaskQueueEntry.
--
--  Format corresponds to: epersonId|submitTime|tasks|objectId
-- =======================================================================

CREATE TABLE IF NOT EXISTS curation_task_queue (
    id SERIAL PRIMARY KEY,
    queue_name TEXT NOT NULL,
    eperson TEXT,
    submit_time BIGINT NOT NULL,
    tasks TEXT NOT NULL,
    object_id TEXT,
    UNIQUE (queue_name, tasks, object_id)
);
-- New table to manage locks on curation queues
CREATE TABLE IF NOT EXISTS curation_queue_lock (
    id SERIAL PRIMARY KEY,
    queue_name TEXT NOT NULL,
    ticket BIGINT NOT NULL,
    owner_id TEXT,
    lock_date TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_cql_queue_name ON curation_queue_lock(queue_name);
CREATE INDEX IF NOT EXISTS idx_ctq_queue ON curation_task_queue(queue_name);
