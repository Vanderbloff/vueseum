-- V7__update_extract_year_function.sql
CREATE OR REPLACE FUNCTION extract_year_from_date(date_str text)
RETURNS integer AS $$
DECLARE
    year_match text;
BEGIN
    -- Handle A.D. format (case insensitive)
    IF date_str ~* 'a\.d\.\s+(\d+)' THEN
        year_match := substring(date_str from 'a\.d\.\s+(\d+)');
        RETURN cast(year_match as integer);
    END IF;

    -- Handle explicit B.C. format
    IF date_str ~* '(\d+)\s*b\.c\.' THEN
        year_match := substring(date_str from '(\d+)');
        RETURN -cast(year_match as integer);
    END IF;

    -- Handle CE format
    IF date_str ~* '(\d+)\s*ce' THEN
        year_match := substring(date_str from '(\d+)');
        RETURN cast(year_match as integer);
    END IF;

    -- Handle A.D. range formats
    IF date_str ~* 'a\.d\.\s+(\d+)-(\d+|present)' THEN
        year_match := substring(date_str from 'a\.d\.\s+(\d+)');
        RETURN cast(year_match as integer);
    END IF;

    -- Keep the existing logic
    -- Extract first occurrence of a 4-digit number
    IF date_str ~ '\d{4}' THEN
        year_match := substring(date_str from '\d{4}');
        RETURN cast(year_match as integer);
    END IF;

    -- Handle BCE/BC dates
    IF date_str ~* '(\d+)\s*(?:bce|bc)' THEN
        year_match := substring(date_str from '(\d+)');
        RETURN -cast(year_match as integer);
    END IF;

    -- Handle century notation
    IF date_str ~* '(\d+)(?:st|nd|rd|th)\s+century' THEN
        year_match := substring(date_str from '(\d+)');
        RETURN cast(year_match as integer) * 100 - 50;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;