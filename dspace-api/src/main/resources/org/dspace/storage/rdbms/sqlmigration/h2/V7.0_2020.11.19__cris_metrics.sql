--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create table for CrisMetrics
-----------------------------------------------------------------------------------

CREATE SEQUENCE cris_metrics_seq;

CREATE TABLE cris_metrics
(
    id INTEGER NOT NULL,
    metricType CHARACTER VARYING(255),
    metricCount FLOAT,
    acquisitionDate TIMESTAMP,
    startDate TIMESTAMP,
    endDate TIMESTAMP,
    resource_id UUID NOT NULL,
    last BOOLEAN,
    remark TEXT,
    CONSTRAINT cris_metrics_pkey PRIMARY KEY (id),
    CONSTRAINT cris_metrics_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES item (uuid)
);

CREATE INDEX metrics_last_idx
ON public.cris_metrics (last);

CREATE INDEX metrics_uuid_idx
ON public.cris_metrics (resource_id);
  
CREATE INDEX metric_bid_idx
ON public.cris_metrics (resource_id, metricType);