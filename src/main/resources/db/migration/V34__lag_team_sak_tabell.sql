CREATE TABLE SAK_STATISTIKK
(
    SEKVENSNUMMER           BIGSERIAL NOT NULL PRIMARY KEY,
    BEHANDLING_ID           BIGINT,
    BEHANDLING_UUID         UUID,
    RELATERT_BEHANDLING_ID  BIGINT,
    RELATERT_FAGSYSTEM      VARCHAR(30),
    SAK_ID                  VARCHAR(30),
    AKTOR_ID                VARCHAR(20)  NOT NULL,
    MOTTATT_TID             TIMESTAMP  NOT NULL,
    REGISTRERT_TID          TIMESTAMP,
    FERDIGBEHANDLET_TID     TIMESTAMP,
    ENDRET_TID              TIMESTAMP NOT NULL,
    TEKNISK_TID             TIMESTAMP NOT NULL,
    SAK_YTELSE              VARCHAR(50),
    BEHANDLING_TYPE         VARCHAR(30),
    BEHANDLING_STATUS       VARCHAR(30),
    BEHANDLING_RESULTAT     VARCHAR(50),
    BEHANDLING_METODE       VARCHAR(30),
    OPPRETTET_AV            VARCHAR(10),
    SAKSBEHANDLER           VARCHAR(10),
    ANSVARLIG_BESLUTTER     VARCHAR(10),
    ANSVARLIG_ENHET         VARCHAR(4),
    AVSENDER                VARCHAR(50),
    VERSJON                 VARCHAR(100)
);

