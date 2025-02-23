CREATE OR REPLACE FUNCTION extract_year_from_date(date_str text)
RETURNS integer AS $$
DECLARE
    year_match text;
BEGIN
    -- Extract first occurrence of a 4-digit number
    IF date_str ~ '\\d{4}' THEN
        year_match := substring(date_str from '\\d{4}');
        RETURN cast(year_match as integer);
    END IF;

    -- Handle BCE/BC dates
    IF date_str ~ '(\\d+)\\s*(?:BCE|BC)' THEN
        year_match := substring(date_str from '(\\d+)');
        RETURN -cast(year_match as integer);
    END IF;

    -- Handle century notation
    IF date_str ~ '(\\d+)(?:st|nd|rd|th)\\s+century' THEN
        year_match := substring(date_str from '(\\d+)');
        RETURN cast(year_match as integer) * 100 - 50;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;