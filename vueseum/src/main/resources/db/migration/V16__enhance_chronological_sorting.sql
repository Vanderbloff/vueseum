-- Update all artworks with more precise chronological values
UPDATE artworks a SET chronological_sort_value =
    CASE
        -- Extract value using new function for all dates
        WHEN a.creation_date IS NOT NULL THEN extract_chronological_year(a.creation_date)
        -- Default value for NULL dates
        ELSE 0
    END;

-- Add index if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'idx_artwork_chronological_sort'
    ) THEN
        CREATE INDEX idx_artwork_chronological_sort ON artworks(chronological_sort_value);
    END IF;
END $$;