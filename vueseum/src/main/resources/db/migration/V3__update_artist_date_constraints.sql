ALTER TABLE artists
    DROP CONSTRAINT birth_date_format,
    DROP CONSTRAINT death_date_format,
    DROP CONSTRAINT valid_lifespan;

-- Allow either empty string or 4 digits
ALTER TABLE artists
    ADD CONSTRAINT birth_date_format
        CHECK (birth_date IS NULL OR birth_date = '' OR birth_date ~ '^[0-9]{4}$'),
    ADD CONSTRAINT death_date_format
        CHECK (death_date IS NULL OR death_date = '' OR death_date ~ '^[0-9]{4}$'),
    ADD CONSTRAINT valid_lifespan
        CHECK (
            birth_date IS NULL OR birth_date = '' OR
            death_date IS NULL OR death_date = '' OR
            (
                birth_date ~ '^[0-9]{4}$' AND
                death_date ~ '^[0-9]{4}$' AND
                CAST(death_date AS INTEGER) - CAST(birth_date AS INTEGER) BETWEEN 1 AND 120
            )
        );