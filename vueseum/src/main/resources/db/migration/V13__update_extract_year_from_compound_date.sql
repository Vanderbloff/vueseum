-- Helper function to extract year from a single date string
CREATE OR REPLACE FUNCTION extract_year_from_single_date(date_str text)
RETURNS integer AS $$
DECLARE
    year_match text;
    year_end text;
    century int;
    millennium int;
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

    -- SPECIFIC YEAR PATTERNS (highest priority)

    -- Look for 4-digit years first (most specific)
    IF date_str ~* '(1[0-9]{3}|20[0-2][0-9])' AND NOT is_bc THEN
        SELECT (regexp_matches(date_str, '(1[0-9]{3}|20[0-2][0-9])', 'i'))[1] INTO year_match;
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

    -- Circa CE pattern
    IF date_str ~* '(?:circa|ca\.|c\.|about|approximately)\s*(1[0-9]{3}|20[0-2][0-9])' AND NOT is_bc THEN
        SELECT (regexp_matches(date_str, '(?:circa|ca\.|c\.|about|approximately)\s*(1[0-9]{3}|20[0-2][0-9])', 'i'))[1] INTO year_match;
        RETURN cast(year_match as integer);
    END IF;

    -- Circa BC range pattern
    IF date_str ~* '(?:circa|ca\.|c\.|about|approximately)\s*(\d+)[\s\-]*\d*\s*(?:b\.c\.|bce|bc)' THEN
        SELECT (regexp_matches(date_str, '(?:circa|ca\.|c\.|about|approximately)\s*(\d+)', 'i'))[1] INTO year_match;
        RETURN -1 * cast(year_match as integer);
    END IF;

    -- CENTURY PATTERNS

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

    -- DATE RANGE PATTERNS

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

    -- MILLENNIUM PATTERNS

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

-- Main function for fixing the chronological sorting
CREATE OR REPLACE FUNCTION extract_year_from_date(date_str text)
RETURNS integer AS $$
DECLARE
    date_parts text[];
    part text;
    temp_result int;
    best_result int := NULL;
    current_priority int := 0;
    best_priority int := 0;
    chronological_year int;
BEGIN
    -- Early null check
    IF date_str IS NULL THEN
        RETURN NULL;
    END IF;

    -- Check if the string contains multiple date parts (separated by semicolons)
    IF position(';' in date_str) > 0 THEN
        -- Split the string and process each part separately
        date_parts := string_to_array(date_str, ';');

        -- Process each part and keep the most specific (non-null) date
        FOREACH part IN ARRAY date_parts
        LOOP
            -- Process each part to get its raw year
            temp_result := extract_year_from_single_date(trim(part));

            -- Skip null results
            IF temp_result IS NULL THEN
                CONTINUE;
            END IF;

            -- Determine priority of this result
            -- Higher priority for specific years (1000-2999)
            IF temp_result BETWEEN 1000 AND 2999 THEN
                current_priority := 3;
            -- Medium priority for BC years
            ELSIF temp_result < 0 THEN
                current_priority := 2;
            -- Lower priority for ancient approximations
            ELSE
                current_priority := 1;
            END IF;

            -- Update our best result if this one has higher priority
            IF best_result IS NULL OR current_priority > best_priority OR
               (current_priority = best_priority AND ABS(temp_result) < ABS(best_result)) THEN
                best_result := temp_result;
                best_priority := current_priority;
            END IF;
        END LOOP;

        -- Transform the best result for chronological sorting
        IF best_result IS NOT NULL THEN
            IF best_result < 0 THEN
                -- Already BC/BCE, leave it negative
                RETURN best_result;
            ELSIF best_result > 3000 THEN
                -- Ancient date (millennium approximation) - make negative for sorting
                RETURN -1 * best_result;
            ELSE
                -- Modern date - leave as is
                RETURN best_result;
            END IF;
        ELSE
            RETURN NULL;
        END IF;
    ELSE
        -- For a single date part
        temp_result := extract_year_from_single_date(date_str);

        -- Transform for chronological sorting
        IF temp_result IS NOT NULL THEN
            IF temp_result < 0 THEN
                -- Already BC/BCE, leave it negative
                RETURN temp_result;
            ELSIF temp_result > 3000 THEN
                -- Ancient date (millennium approximation) - make negative for sorting
                RETURN -1 * temp_result;
            ELSE
                -- Modern date - leave as is
                RETURN temp_result;
            END IF;
        ELSE
            RETURN NULL;
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;