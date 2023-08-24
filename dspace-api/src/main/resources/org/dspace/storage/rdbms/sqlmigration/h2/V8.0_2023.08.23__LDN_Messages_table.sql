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

CREATE TABLE ldn_messages
(
  id VARCHAR(255) PRIMARY KEY,
  object uuid,
  message TEXT,
  type VARCHAR(255),
  origin INTEGER,
  target INTEGER,
  inReplyTo VARCHAR(255),
  context uuid,
  FOREIGN KEY (object) REFERENCES dspaceobject (uuid) ON DELETE SET NULL,
  FOREIGN KEY (context) REFERENCES dspaceobject (uuid) ON DELETE SET NULL,
  FOREIGN KEY (origin) REFERENCES notifyservices (id) ON DELETE SET NULL,
  FOREIGN KEY (target) REFERENCES notifyservices (id) ON DELETE SET NULL,
  FOREIGN KEY (inReplyTo) REFERENCES ldn_messages (id) ON DELETE SET NULL
);