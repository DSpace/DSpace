--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- CREATE notifyservice table
-----------------------------------------------------------------------------------


CREATE SEQUENCE if NOT EXISTS notifyservice_id_seq;

CREATE TABLE notifyservice (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    url VARCHAR(255),
    ldn_url VARCHAR(255),
    enabled BOOLEAN NOT NULL,
    score NUMERIC(6, 5),
    lower_ip VARCHAR(45),
    upper_ip VARCHAR(45),
    CONSTRAINT ldn_url_unique UNIQUE (ldn_url)
);

-----------------------------------------------------------------------------------
-- CREATE notifyservice_inbound_pattern_id_seq table
-----------------------------------------------------------------------------------

CREATE SEQUENCE if NOT EXISTS notifyservice_inbound_pattern_id_seq;

CREATE TABLE notifyservice_inbound_pattern (
    id INTEGER PRIMARY KEY,
    service_id INTEGER REFERENCES notifyservice(id) ON DELETE CASCADE,
    pattern VARCHAR(255),
    constraint_name VARCHAR(255),
    automatic BOOLEAN
);

CREATE INDEX notifyservice_inbound_idx ON notifyservice_inbound_pattern (service_id);


-------------------------------------------------------------------------------
-- Table to store LDN messages
-------------------------------------------------------------------------------

CREATE TABLE ldn_message
(
  id VARCHAR(255) PRIMARY KEY,
  object uuid,
  message TEXT,
  type VARCHAR(255),
  origin INTEGER,
  target INTEGER,
  inReplyTo VARCHAR(255),
  context uuid,
  activity_stream_type VARCHAR(255),
  coar_notify_type VARCHAR(255),
  queue_status INTEGER DEFAULT NULL,
  queue_attempts INTEGER DEFAULT 0,
  queue_last_start_time TIMESTAMP,
  queue_timeout TIMESTAMP,
  source_ip VARCHAR(45),
  FOREIGN KEY (object) REFERENCES dspaceobject (uuid) ON DELETE SET NULL,
  FOREIGN KEY (context) REFERENCES dspaceobject (uuid) ON DELETE SET NULL,
  FOREIGN KEY (origin) REFERENCES notifyservice (id) ON DELETE SET NULL,
  FOREIGN KEY (target) REFERENCES notifyservice (id) ON DELETE SET NULL,
  FOREIGN KEY (inReplyTo) REFERENCES ldn_message (id) ON DELETE SET NULL
);


-------------------------------------------------------------------------------
-- Table to store notify patterns that will be triggered
-------------------------------------------------------------------------------

CREATE SEQUENCE if NOT EXISTS notifypatterns_to_trigger_id_seq;

CREATE TABLE notifypatterns_to_trigger
(
  id INTEGER PRIMARY KEY,
  item_id UUID REFERENCES Item(uuid) ON DELETE CASCADE,
  service_id INTEGER REFERENCES notifyservice(id) ON DELETE CASCADE,
  pattern VARCHAR(255)
);

CREATE INDEX notifypatterns_to_trigger_item_idx ON notifypatterns_to_trigger (item_id);
CREATE INDEX notifypatterns_to_trigger_service_idx ON notifypatterns_to_trigger (service_id);
