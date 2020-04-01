
INSERT INTO VEDTAK (AKTOR_ID, VEILEDER_IDENT, OPPFOLGINGSENHET_ID, SIST_OPPDATERT, STATUS) VALUES ('123', 'Z1234', '1234', CURRENT_TIMESTAMP, 'UTKAST');

INSERT INTO BESLUTTEROVERSIKT_BRUKER (
VEDTAK_ID, BRUKER_FORNAVN, BRUKER_ETTERNAVN, BRUKER_OPPFOLGINGSENHET_NAVN,
BRUKER_OPPFOLGINGSENHET_ID, BRUKER_FNR, VEDTAK_STARTET, STATUS, BESLUTTER_NAVN, VEILEDER_NAVN
) VALUES (1, 'Ola', 'Nordmann', 'NAV Testheim', '1234', '12345678900', CURRENT_TIMESTAMP , 'HAR_BESLUTTER', 'Beslutter Besluttersen', 'Veileder veiledersen');

INSERT INTO BESLUTTEROVERSIKT_BRUKER (
VEDTAK_ID, BRUKER_FORNAVN, BRUKER_ETTERNAVN, BRUKER_OPPFOLGINGSENHET_NAVN,
BRUKER_OPPFOLGINGSENHET_ID, BRUKER_FNR, VEDTAK_STARTET, STATUS, BESLUTTER_NAVN, VEILEDER_NAVN
) VALUES (1, 'Kari', 'Karlsen', 'NAV Aremark', '6755', '8967498237', CURRENT_TIMESTAMP , 'TRENGER_BESLUTTER', null, 'Vegar Veileder');

INSERT INTO BESLUTTEROVERSIKT_BRUKER (
VEDTAK_ID, BRUKER_FORNAVN, BRUKER_ETTERNAVN, BRUKER_OPPFOLGINGSENHET_NAVN,
BRUKER_OPPFOLGINGSENHET_ID, BRUKER_FNR, VEDTAK_STARTET, STATUS, BESLUTTER_NAVN, VEILEDER_NAVN
) VALUES (1, 'Test', 'Testersen', 'NAV Mars', '6123', '6456457623', CURRENT_TIMESTAMP , 'GODKJENT_AV_BESLUTTER', 'Bob Beslutter', 'Viktor Veileder');