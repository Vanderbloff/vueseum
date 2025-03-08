CREATE OR REPLACE FUNCTION extract_year_from_date(date_str text)
RETURNS integer AS $$
DECLARE
    year_match text;
    year_end text;
    century int;
    millennium int;
    result int;
    is_bc boolean := false;
BEGIN
    -- Early null check
    IF date_str IS NULL THEN
        RETURN NULL;
    END IF;

    -- Normalize the input (lowercase, trim excess whitespace)
    date_str := lower(trim(regexp_replace(date_str, '\s+', ' ', 'g')));

    -- Check if it's B.C./BCE
    is_bc := date_str ~* 'b\.c\.|bce|bc';

    -- HANDLE MILLENNIUM PATTERNS

    -- Early millennium range pattern
    IF date_str ~* 'early\s+(\d+)(?:st|nd|rd|th)?[\s\-]+\d+(?:st|nd|rd|th)?\s+millennium' THEN
        SELECT (regexp_matches(date_str, 'early\s+(\d+)', 'i'))[1] INTO year_match;
        millennium := year_match::int;

        IF is_bc THEN
            RETURN -1 * (millennium * 1000 - 300);
        ELSE
            RETURN (millennium - 1) * 1000 + 300;
        END IF;
    END IF;

    -- Late millennium pattern
    IF date_str ~* 'late\s+(\d+)(?:st|nd|rd|th)?\s+millennium' THEN
        SELECT (regexp_matches(date_str, 'late\s+(\d+)', 'i'))[1] INTO year_match;
        millennium := year_match::int;

        IF is_bc THEN
            RETURN -1 * (millennium * 1000 - 700);
        ELSE
            RETURN (millennium - 1) * 1000 + 700;
        END IF;
    END IF;

    -- Mid/middle millennium pattern
    IF date_str ~* '(?:mid|middle of)\s+(?:the\s+)?(\d+)(?:st|nd|rd|th)?\s+millennium' THEN
        SELECT (regexp_matches(date_str, '(?:mid|middle of)\s+(?:the\s+)?(\d+)', 'i'))[1] INTO year_match;
        millennium := year_match::int;

        IF is_bc THEN
            RETURN -1 * (millennium * 1000 - 500);
        ELSE
            RETURN (millennium - 1) * 1000 + 500;
        END IF;
    END IF;

    -- First/second half millennium patterns
    IF date_str ~* '(?:first half|beginning) of(?: the)? (\d+)(?:st|nd|rd|th)?\s+millennium' THEN
        SELECT (regexp_matches(date_str, '(?:first half|beginning) of(?: the)? (\d+)', 'i'))[1] INTO year_match;
        millennium := year_match::int;

        IF is_bc THEN
            RETURN -1 * (millennium * 1000 - 250);
        ELSE
            RETURN (millennium - 1) * 1000 + 250;
        END IF;
    END IF;

    IF date_str ~* '(?:second half|end) of(?: the)? (\d+)(?:st|nd|rd|th)?\s+millennium' THEN
        SELECT (regexp_matches(date_str, '(?:second half|end) of(?: the)? (\d+)', 'i'))[1] INTO year_match;
        millennium := year_match::int;

        IF is_bc THEN
            RETURN -1 * (millennium * 1000 - 750);
        ELSE
            RETURN (millennium - 1) * 1000 + 750;
        END IF;
    END IF;

    -- Millennium range pattern
    IF date_str ~* '(\d+)(?:st|nd|rd|th)?[\s\-]+\d+(?:st|nd|rd|th)?\s+millennium' THEN
        SELECT (regexp_matches(date_str, '(\d+)(?:st|nd|rd|th)?', 'i'))[1] INTO year_match;
        millennium := year_match::int;

        IF is_bc THEN
            RETURN -1 * (millennium * 1000 - 500);
        ELSE
            RETURN (millennium - 1) * 1000 + 500;
        END IF;
    END IF;

    -- Standard millennium (no qualifiers)
    IF date_str ~* '(\d+)(?:st|nd|rd|th)?\s+millennium' THEN
        SELECT (regexp_matches(date_str, '(\d+)(?:st|nd|rd|th)?', 'i'))[1] INTO year_match;
        millennium := year_match::int;

        IF is_bc THEN
            RETURN -1 * (millennium * 1000 - 500);
        ELSE
            RETURN (millennium - 1) * 1000 + 500;
        END IF;
    END IF;

    -- HANDLE CENTURY PATTERNS

    -- Century range pattern
    IF date_str ~* '(\d+)(?:st|nd|rd|th)[\s\-]+\d+(?:st|nd|rd|th)\s+century' THEN
        SELECT (regexp_matches(date_str, '(\d+)(?:st|nd|rd|th)', 'i'))[1] INTO year_match;
        century := year_match::int;

        IF is_bc THEN
            RETURN -1 * ((century - 1) * 100 + 50);
        ELSE
            RETURN (century - 1) * 100 + 50;
        END IF;
    END IF;

    -- Early century pattern
    IF date_str ~* 'early\s+(\d+)(?:st|nd|rd|th)\s+century' THEN
        SELECT (regexp_matches(date_str, 'early\s+(\d+)', 'i'))[1] INTO year_match;
        century := year_match::int;

        IF is_bc THEN
            RETURN -1 * ((century - 1) * 100 + 25);
        ELSE
            RETURN (century - 1) * 100 + 25;
        END IF;
    END IF;

    -- Late century pattern
    IF date_str ~* 'late\s+(\d+)(?:st|nd|rd|th)\s+century' THEN
        SELECT (regexp_matches(date_str, 'late\s+(\d+)', 'i'))[1] INTO year_match;
        century := year_match::int;

        IF is_bc THEN
            RETURN -1 * ((century - 1) * 100 + 75);
        ELSE
            RETURN (century - 1) * 100 + 75;
        END IF;
    END IF;

    -- Standard century pattern
    IF date_str ~* '(\d+)(?:st|nd|rd|th)\s+century' THEN
        SELECT (regexp_matches(date_str, '(\d+)', 'i'))[1] INTO year_match;
        century := year_match::int;

        IF is_bc THEN
            RETURN -1 * ((century - 1) * 100 + 50);
        ELSE
            RETURN (century - 1) * 100 + 50;
        END IF;
    END IF;

    -- HANDLE DATE RANGES

    -- Cross-era range like "30 B.C.–A.D. 364" - return the BC date
    IF date_str ~* '(\d+)\s*(?:b\.c\.|bce|bc)[\s\-]+a\.d\.\s*\d+' THEN
        SELECT (regexp_matches(date_str, '(\d+)\s*(?:b\.c\.|bce|bc)', 'i'))[1] INTO year_match;
        RETURN -1 * cast(year_match as integer);
    END IF;

    -- Abbreviated range like "1910-15"
    IF date_str ~* '(1[0-9]{3}|20[0-2][0-9])[\s\-]+(\d{1,2})' AND NOT is_bc THEN
        SELECT (regexp_matches(date_str, '(1[0-9]{3}|20[0-2][0-9])', 'i'))[1] INTO year_match;
        RETURN cast(year_match as integer);
    END IF;

    -- Circa BC range pattern
    IF date_str ~* '(?:circa|ca\.|c\.|about|approximately)\s*(\d+)[\s\-]*\d*\s*(?:b\.c\.|bce|bc)' THEN
        SELECT (regexp_matches(date_str, '(?:circa|ca\.|c\.|about|approximately)\s*(\d+)', 'i'))[1] INTO year_match;
        RETURN -1 * cast(year_match as integer);
    END IF;

    -- BC range pattern
    IF date_str ~* '(\d+)[\s\-]+\d+\s*(?:b\.c\.|bce|bc)' THEN
        SELECT (regexp_matches(date_str, '(\d+)', 'i'))[1] INTO year_match;
        RETURN -1 * cast(year_match as integer);
    END IF;

    -- A.D. range pattern
    IF date_str ~* 'a\.d\.\s+(\d+)[\s\-]+(\d+|present)' THEN
        SELECT (regexp_matches(date_str, 'a\.d\.\s+(\d+)', 'i'))[1] INTO year_match;
        RETURN cast(year_match as integer);
    END IF;

    -- General year range for CE dates
    IF date_str ~* '(\d+)[\s\-]+(\d+)' AND NOT is_bc THEN
        SELECT (regexp_matches(date_str, '(\d+)', 'i'))[1] INTO year_match;
        SELECT (regexp_matches(date_str, '[\s\-]+(\d+)', 'i'))[1] INTO year_end;

        -- Check if it's an abbreviated range like "1910-15"
        IF length(year_end) <= 2 AND length(year_match) = 4 THEN
            -- Combine the century from first year with the abbreviated second year
            RETURN cast(year_match as integer);
        END IF;

        RETURN cast(year_match as integer);
    END IF;

    -- HANDLE SINGLE DATES

    -- Circa CE pattern
    IF date_str ~* '(?:circa|ca\.|c\.|about|approximately)\s*(1[0-9]{3}|20[0-2][0-9])' AND NOT is_bc THEN
        SELECT (regexp_matches(date_str, '(?:circa|ca\.|c\.|about|approximately)\s*(1[0-9]{3}|20[0-2][0-9])', 'i'))[1] INTO year_match;
        RETURN cast(year_match as integer);
    END IF;

    -- B.C./BCE pattern with special spacing
    IF date_str ~* '(\d+)\s*b\.\s*c\.' THEN
        SELECT (regexp_matches(date_str, '(\d+)', 'i'))[1] INTO year_match;
        RETURN -1 * cast(year_match as integer);
    END IF;

    -- Standard B.C./BCE pattern
    IF date_str ~* '(\d+)\s*(?:b\.c\.|bce|bc)' THEN
        SELECT (regexp_matches(date_str, '(\d+)', 'i'))[1] INTO year_match;
        RETURN -1 * cast(year_match as integer);
    END IF;

    -- A.D. pattern
    IF date_str ~* 'a\.d\.\s+(\d+)' THEN
        SELECT (regexp_matches(date_str, 'a\.d\.\s+(\d+)', 'i'))[1] INTO year_match;
        RETURN cast(year_match as integer);
    END IF;

    -- CE pattern
    IF date_str ~* '(\d+)\s*ce' THEN
        SELECT (regexp_matches(date_str, '(\d+)', 'i'))[1] INTO year_match;
        RETURN cast(year_match as integer);
    END IF;

    -- Extract 4-digit number as fallback
    IF date_str ~* '\d{4}' THEN
        SELECT (regexp_matches(date_str, '(\d{4})', 'i'))[1] INTO year_match;

        IF is_bc THEN
            RETURN -1 * cast(year_match as integer);
        ELSE
            RETURN cast(year_match as integer);
        END IF;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;