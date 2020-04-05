CREATE TYPE MELDING_UNDER_TYPE AS enum (
    'UTKAST_OPPRETTET',
    'BESLUTTER_PROSESS_STARTET',
    'BLI_BESLUTTER',
    'GODSKJENT_AV_BESLUTTER',
    'TA_OVER_SOM_BESLUTTER',
    'TA_OVER_SOM_VEILEDER'
);

CREATE TYPE MELDING_TYPE AS enum (
    'MANUELL',
    'SYSTEM'
);

ALTER TABLE DIALOG_MELDING ADD COLUMN MELDING_UNDER_TYPE MELDING_UNDER_TYPE;

ALTER TABLE DIALOG_MELDING ADD COLUMN MELDING_TYPE MELDING_TYPE NOT NULL;

