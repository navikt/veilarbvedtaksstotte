-- Fremmednøkler kan bare referere til unike verdier, men journalpost_id har ikke tidligere vært definert som unik til tross for at den er det
ALTER TABLE vedtak
    ADD CONSTRAINT journalpost_id_unique UNIQUE (journalpost_id);

CREATE TABLE RETRY_VEDTAKDISTRIBUSJON
(
    JOURNALPOST_ID      VARCHAR(20) PRIMARY KEY,
    DISTRIBUSJONSFORSOK INT NOT NULL DEFAULT 1, -- Settes til 1 siden ingenting settes inn i tabellen uten å ha feilet først
    FOREIGN KEY (JOURNALPOST_ID) REFERENCES vedtak (journalpost_id) ON DELETE CASCADE -- Hvis vedtak slettes, slett tilhørende retry-rad
);
