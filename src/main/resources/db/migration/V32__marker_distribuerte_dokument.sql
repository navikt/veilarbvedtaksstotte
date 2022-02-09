UPDATE vedtak
SET dokument_bestilling_id = '-'
WHERE dokument_bestilling_id IS NULL
  AND vedtak_fattet IS NOT NULL
  AND journalpost_id IS NOT NULL
  AND dokument_id IS NOT NULL;
