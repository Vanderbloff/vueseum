-- Increase sizes of potentially problematic VARCHAR fields in artworks table
ALTER TABLE artworks ALTER COLUMN title TYPE VARCHAR(1000);
ALTER TABLE artworks ALTER COLUMN medium TYPE VARCHAR(1000);
ALTER TABLE artworks ALTER COLUMN culture TYPE VARCHAR(1000);
ALTER TABLE artworks ALTER COLUMN country TYPE VARCHAR(1000);
ALTER TABLE artworks ALTER COLUMN region TYPE VARCHAR(1000);
ALTER TABLE artworks ALTER COLUMN sub_region TYPE VARCHAR(1000);
ALTER TABLE artworks ALTER COLUMN artist_prefix TYPE VARCHAR(500);
ALTER TABLE artworks ALTER COLUMN artist_role TYPE VARCHAR(500);
ALTER TABLE artworks ALTER COLUMN classification TYPE VARCHAR(1000);
ALTER TABLE artworks ALTER COLUMN copyright_status TYPE VARCHAR(1000);
ALTER TABLE artworks ALTER COLUMN department TYPE VARCHAR(500);
ALTER TABLE artworks ALTER COLUMN geography_type TYPE VARCHAR(500);

-- Increase sizes of potentially problematic VARCHAR fields in artists table
ALTER TABLE artists ALTER COLUMN artist_name TYPE VARCHAR(1000);
ALTER TABLE artists ALTER COLUMN nationality TYPE VARCHAR(500);