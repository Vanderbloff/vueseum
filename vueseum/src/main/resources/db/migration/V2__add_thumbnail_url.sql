ALTER TABLE artworks
ADD COLUMN thumbnail_image_url VARCHAR(500);

CREATE INDEX idx_artwork_thumbnail_url ON artworks(thumbnail_image_url);