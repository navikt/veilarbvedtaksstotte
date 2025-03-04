-- 2025-02-27
-- Denne tabellen representerer en kobling mellom ulike identer til en gitt person.
-- Tabellen/mappingen er basert på PDL sin "Referanseimplementasjon - bruk av identer".
-- Tabellen er i skrivende stund identisk med BRUKER_IDENTER-tabellen i veilarbportefolje.
CREATE TABLE BRUKER_IDENTER
(
    PERSON    VARCHAR(25) NOT NULL,
    IDENT     VARCHAR(30) PRIMARY KEY,
    HISTORISK BOOLEAN     NOT NULL,
    GRUPPE    VARCHAR(30) NOT NULL
);

CREATE SEQUENCE BRUKER_IDENTER_PERSON_SEQ AS BIGINT START 1;
CREATE INDEX BRUKER_IDENTER_PERSON_IDX ON BRUKER_IDENTER(PERSON);