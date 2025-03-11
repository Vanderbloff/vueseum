ALTER TABLE artworks ADD COLUMN chronological_sort_value INTEGER;

-- Update existing records with appropriate values
UPDATE artworks a SET chronological_sort_value =
  CASE
    -- BC/BCE dates with different patterns
    WHEN a.creation_date LIKE '%B.C.%' OR a.creation_date LIKE '%BC%' OR
         a.creation_date LIKE '%BCE%' OR a.creation_date LIKE '%century BCE%' OR
         a.creation_date LIKE '%millennium BCE%'
    THEN -10000 + COALESCE(extract_year_from_date(a.creation_date), 0)
         -- The extracted year is already negative for BCE dates, so this formula
         -- makes it even more negative, e.g., -2300 becomes -12300
         -- This ensures ancient dates sort correctly

    -- Modern dates
    ELSE COALESCE(extract_year_from_date(a.creation_date), 0)
  END;

CREATE INDEX idx_artwork_chronological_sort ON artworks(chronological_sort_value);