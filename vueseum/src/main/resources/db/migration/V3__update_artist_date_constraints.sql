ALTER TABLE artists DROP CONSTRAINT birth_date_format;
ALTER TABLE artists DROP CONSTRAINT death_date_format;

-- Allow either empty string or 4 digits
ALTER TABLE artists ADD CONSTRAINT birth_date_format
    CHECK (birth_date IS NULL OR birth_date = '' OR birth_date ~ '^[0-9]{4}$');
ALTER TABLE artists ADD CONSTRAINT death_date_format
    CHECK (death_date IS NULL OR death_date = '' OR death_date ~ '^[0-9]{4}$');