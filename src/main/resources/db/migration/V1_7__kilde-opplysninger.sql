CREATE TABLE OYBLIKKSBILDE (
  VEDTAK_ID               NUMBER NOT NULL,
  KILDE                   VARCHAR(20),
  JSON                    BLOB,
  CONSTRAINT ensure_json CHECK (JSON IS JSON)
);

CREATE TABLE OPPLYSNING (
  VEDTAK_ID               NUMBER NOT NULL,
  TEKST                   VARCHAR(200)
);
