CREATE SEQUENCE openurltracker_seq;

CREATE TABLE OpenUrlTracker
(
  tracker_id  INTEGER PRIMARY KEY,
  tracker_url VARCHAR(1000),
  uploaddate  DATE
);
