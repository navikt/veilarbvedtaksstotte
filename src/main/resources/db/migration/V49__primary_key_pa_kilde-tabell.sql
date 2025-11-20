UPDATE kilde SET kilde_id = gen_random_uuid() WHERE kilde_id IS NULL;

ALTER TABLE kilde ALTER COLUMN kilde_id SET NOT NULL;

ALTER TABLE kilde ADD PRIMARY KEY (kilde_id);
