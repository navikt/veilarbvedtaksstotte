ALTER TABLE VEDTAK
    RENAME COLUMN SIST_OPPDATERT TO UTKAST_SIST_OPPDATERT;

ALTER TABLE VEDTAK
    ADD COLUMN VEDTAK_FATTET TIMESTAMP;

UPDATE VEDTAK
    SET VEDTAK_FATTET = UTKAST_SIST_OPPDATERT
    WHERE STATUS = 'SENDT';