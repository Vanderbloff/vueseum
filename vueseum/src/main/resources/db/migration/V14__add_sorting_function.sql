CREATE OR REPLACE FUNCTION sort_by_chronology(date_str text)
RETURNS int AS $$
DECLARE
    raw_year int;
BEGIN
    -- Get the raw year from the existing function
    raw_year := extract_year_from_date(date_str);

    -- For sorting purposes only:
    -- Invert the sign of positive years > 3000 (ancient approximations)
    -- Keep modern dates (1000-2999) as is
    -- Keep BC dates (negative) as is
    IF raw_year IS NULL THEN
        RETURN NULL;
    ELSIF raw_year > 3000 THEN
        RETURN -1 * raw_year; -- Ancient date
    ELSE
        RETURN raw_year; -- Modern or BC date
    END IF;
END;
$$ LANGUAGE plpgsql;