-- Table and sequence to store rating for recommender system
CREATE SEQUENCE ratings_seq;

CREATE TABLE ratings
 (
  rating_id           	INTEGER NOT NULL,
  eperson_id		INTEGER,
  dspace_object_id 	INTEGER,
  rating      		INTEGER,
  CONSTRAINT ratings_pkey PRIMARY KEY (rating_id)
);
