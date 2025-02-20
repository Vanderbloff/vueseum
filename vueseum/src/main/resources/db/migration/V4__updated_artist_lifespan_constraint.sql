ALTER TABLE artists DROP CONSTRAINT valid_lifespan;

ALTER TABLE artists ADD CONSTRAINT valid_lifespan
    CHECK (
        birth_date IS NULL OR birth_date = '' OR
        death_date IS NULL OR death_date = '' OR
        (
            birth_date ~ '^[0-9]{4}$' AND
            death_date ~ '^[0-9]{4}$' AND
            CAST(death_date AS INTEGER) - CAST(birth_date AS INTEGER) BETWEEN 1 AND 120
        )
    );