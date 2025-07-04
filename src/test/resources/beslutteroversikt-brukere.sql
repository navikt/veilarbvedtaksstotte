
INSERT INTO VEDTAK (AKTOR_ID, VEILEDER_IDENT, OPPFOLGINGSENHET_ID, UTKAST_SIST_OPPDATERT, STATUS) VALUES ('123', 'Z1234', '1234', CURRENT_TIMESTAMP, 'UTKAST');

INSERT INTO BESLUTTEROVERSIKT_BRUKER (
VEDTAK_ID, BRUKER_FORNAVN, BRUKER_ETTERNAVN, BRUKER_OPPFOLGINGSENHET_NAVN,
BRUKER_OPPFOLGINGSENHET_ID, BRUKER_FNR, VEDTAK_STARTET, STATUS, BESLUTTER_NAVN, BESLUTTER_IDENT, VEILEDER_NAVN
) VALUES (currval('vedtak_id_seq'), 'Kari', 'Karlsen', 'Nav Aremark', '6755', '12345678900', CURRENT_TIMESTAMP , 'TRENGER_BESLUTTER', null, null, 'Vegar Veileder');

INSERT INTO BESLUTTEROVERSIKT_BRUKER (
VEDTAK_ID, BRUKER_FORNAVN, BRUKER_ETTERNAVN, BRUKER_OPPFOLGINGSENHET_NAVN,
BRUKER_OPPFOLGINGSENHET_ID, BRUKER_FNR, VEDTAK_STARTET, STATUS, BESLUTTER_NAVN, BESLUTTER_IDENT, VEILEDER_NAVN
) VALUES (currval('vedtak_id_seq'), 'Ola', 'Nordmann', 'Nav Testheim', '1234', '011111111111', CURRENT_TIMESTAMP , 'KLAR_TIL_BESLUTTER', 'Beslutter Besluttersen', 'Z748932', 'Veileder veiledersen');


INSERT INTO BESLUTTEROVERSIKT_BRUKER (
VEDTAK_ID, BRUKER_FORNAVN, BRUKER_ETTERNAVN, BRUKER_OPPFOLGINGSENHET_NAVN,
BRUKER_OPPFOLGINGSENHET_ID, BRUKER_FNR, VEDTAK_STARTET, STATUS, BESLUTTER_NAVN, BESLUTTER_IDENT, VEILEDER_NAVN
) VALUES (currval('vedtak_id_seq'), 'Test', 'Testersen', 'Nav Mars', '6123', '9999999999', CURRENT_TIMESTAMP , 'GODKJENT_AV_BESLUTTER', 'Bob Beslutter', 'Z748932', 'Viktor Veileder');
