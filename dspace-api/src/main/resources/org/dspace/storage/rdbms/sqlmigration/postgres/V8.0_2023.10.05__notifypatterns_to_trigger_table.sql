--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

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
