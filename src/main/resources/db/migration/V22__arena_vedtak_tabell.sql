CREATE TABLE ARENA_VEDTAK
(
    FNR           VARCHAR(20) UNIQUE NOT NULL,
    HOVEDMAL      VARCHAR(30),
    INNSATSGRUPPE VARCHAR(40)        NOT NULL,
    FRA_DATO      TIMESTAMP          NOT NULL,
    MOD_USER      VARCHAR(256)
);
