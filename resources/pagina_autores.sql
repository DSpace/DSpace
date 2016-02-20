CREATE SEQUENCE authorprofile_seq;
CREATE SEQUENCE authorprofile2bitstream_seq;


CREATE TABLE authorprofile
(
    authorprofile_id          INTEGER PRIMARY KEY
);


CREATE TABLE authorprofile2bitstream
(
    id              INTEGER PRIMARY KEY,
    authorprofile_id  INTEGER REFERENCES authorprofile(authorprofile_id),
    bitstream_id    INTEGER REFERENCES Bitstream(bitstream_id)
);

CREATE INDEX authorprofile2bitstream_author_profile_fk_idx ON authorprofile2bitstream(authorprofile_id);
CREATE INDEX authorprofile2bitstream_bitstream_fk_idx ON authorprofile2bitstream(bitstream_id);
