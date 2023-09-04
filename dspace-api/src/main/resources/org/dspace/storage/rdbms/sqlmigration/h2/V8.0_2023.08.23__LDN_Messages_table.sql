--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

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
  FOREIGN KEY (object) REFERENCES dspaceobject (uuid) ON DELETE SET NULL,
  FOREIGN KEY (context) REFERENCES dspaceobject (uuid) ON DELETE SET NULL,
  FOREIGN KEY (origin) REFERENCES notifyservice (id) ON DELETE SET NULL,
  FOREIGN KEY (target) REFERENCES notifyservice (id) ON DELETE SET NULL,
  FOREIGN KEY (inReplyTo) REFERENCES ldn_message (id) ON DELETE SET NULL
);