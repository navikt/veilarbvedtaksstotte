CREATE TABLE ARENA_VEDTAK
(
    FNR                     VARCHAR(20)     UNIQUE  NOT NULL,
    HOVEDMAL                VARCHAR(30),
    INNSATSGRUPPE           VARCHAR(40)             NOT NULL,
    FRA_DATO                TIMESTAMP               NOT NULL,
    REG_USER                VARCHAR(256)            NOT NULL,
    OPERATION_TIMESTAMP     TIMESTAMP               NOT NULL,
    HENDELSE_ID             BIGSERIAL               NOT NULL,
    VEDTAK_ID               BIGSERIAL               NOT NULL
);
