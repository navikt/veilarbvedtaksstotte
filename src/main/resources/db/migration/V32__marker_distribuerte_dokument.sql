UPDATE vedtak
SET dokument_bestilling_id = '-'
WHERE vedtak_fattet IS NOT NULL
  AND status = 'SENDT'
  AND dokument_bestilling_id IS NULL
  AND journalpost_id IS NOT NULL
  AND dokument_id IS NOT NULL
  AND vedtak_fattet < '2022-03-04';
