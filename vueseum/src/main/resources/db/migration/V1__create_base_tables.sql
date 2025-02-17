-- Create sequence for ID generation
CREATE SEQUENCE id_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create museums table
CREATE TABLE museums (
    -- Base entity fields
    id BIGINT PRIMARY KEY DEFAULT nextval('id_sequence'),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    -- Museum-specific fields
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    website_url VARCHAR(255),
    museum_hours JSONB DEFAULT '{}',
    additional_metadata JSONB DEFAULT '{}',
    CONSTRAINT museum_name_location_unique UNIQUE (name, location)
);

-- Create artists table
CREATE TABLE artists (
    -- Base entity fields
    id BIGINT PRIMARY KEY DEFAULT nextval('id_sequence'),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    -- Artist-specific fields
    artist_name VARCHAR(255) NOT NULL,
    nationality VARCHAR(100),
    birth_date VARCHAR(4),
    death_date VARCHAR(4),
    additional_metadata JSONB DEFAULT '{}',
    CONSTRAINT artist_name_unique UNIQUE (artist_name),
    CONSTRAINT birth_date_format CHECK (birth_date ~ '^[0-9]{4}$'),
    CONSTRAINT death_date_format CHECK (death_date ~ '^[0-9]{4}$'),
    CONSTRAINT valid_lifespan CHECK (
        birth_date IS NULL OR
        death_date IS NULL OR
        (CAST(death_date AS INTEGER) - CAST(birth_date AS INTEGER)) BETWEEN 1 AND 120
    )
);

-- Create artworks table
CREATE TABLE artworks (
    -- Base entity fields
    id BIGINT PRIMARY KEY DEFAULT nextval('id_sequence'),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    -- Artwork-specific fields
    title VARCHAR(255) NOT NULL,
    external_id VARCHAR(100) NOT NULL,
    artist_id BIGINT REFERENCES artists(id),
    museum_id BIGINT NOT NULL REFERENCES museums(id),
    medium VARCHAR(255),
    culture VARCHAR(255),
    country VARCHAR(255),
    region VARCHAR(255),
    sub_region VARCHAR(255),
    geography_type VARCHAR(50),
    gallery_number VARCHAR(50),
    image_url TEXT,
    description TEXT,
    department VARCHAR(255),
    creation_date VARCHAR(100),
    processing_status VARCHAR(50) DEFAULT 'PENDING',
    last_sync_attempt TIMESTAMP,
    last_sync_error TEXT,
    artist_prefix VARCHAR(100),
    artist_role VARCHAR(100),
    classification VARCHAR(255),
    copyright_status VARCHAR(255),
    additional_metadata JSONB DEFAULT '{}',
    CONSTRAINT external_id_museum_unique UNIQUE (external_id, museum_id)
);

-- Create tours table
CREATE TABLE tours (
    -- Base entity fields
    id BIGINT PRIMARY KEY DEFAULT nextval('id_sequence'),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    -- Tour-specific fields
    device_fingerprint VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    metadata JSONB DEFAULT '{}',
    museum_id BIGINT NOT NULL REFERENCES museums(id),
    generation_prompt TEXT,
    tour_theme VARCHAR(50),
    last_validated TIMESTAMP
);

-- Create tour_stops table
CREATE TABLE tour_stops (
    -- Base entity fields
    id BIGINT PRIMARY KEY DEFAULT nextval('id_sequence'),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    -- Tour stop-specific fields
    tour_id BIGINT NOT NULL REFERENCES tours(id),
    artwork_id BIGINT NOT NULL REFERENCES artworks(id),
    sequence_number INTEGER NOT NULL,
    standard_description TEXT,
    tour_context_description TEXT,
    is_required BOOLEAN DEFAULT false,
    CONSTRAINT unique_tour_sequence UNIQUE (tour_id, sequence_number)
);

-- Create indices for optimized queries
CREATE INDEX idx_artwork_museum ON artworks(museum_id);
CREATE INDEX idx_artwork_artist ON artworks(artist_id);
CREATE INDEX idx_artwork_status ON artworks(processing_status);
CREATE INDEX idx_tour_museum ON tours(museum_id);
CREATE INDEX idx_tour_theme ON tours(tour_theme);
CREATE INDEX idx_tour_stop_sequence ON tour_stops(tour_id, sequence_number);
CREATE INDEX idx_artwork_creation_date ON artworks(creation_date NULLS LAST);
CREATE INDEX idx_artwork_title ON artworks(title NULLS LAST);
CREATE INDEX idx_artwork_culture_country ON artworks(culture, country);
CREATE INDEX idx_artwork_geography ON artworks(country, region, sub_region);
CREATE INDEX idx_tour_device_fingerprint ON tours(device_fingerprint);

-- Create indices for soft delete queries
CREATE INDEX idx_museums_deleted ON museums(is_deleted);
CREATE INDEX idx_artists_deleted ON artists(is_deleted);
CREATE INDEX idx_artworks_deleted ON artworks(is_deleted);
CREATE INDEX idx_tours_deleted ON tours(is_deleted);
CREATE INDEX idx_tour_stops_deleted ON tour_stops(is_deleted);

-- Create trigger function for updating the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for each table
CREATE TRIGGER update_museums_updated_at
    BEFORE UPDATE ON museums
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_artists_updated_at
    BEFORE UPDATE ON artists
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_artworks_updated_at
    BEFORE UPDATE ON artworks
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tours_updated_at
    BEFORE UPDATE ON tours
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tour_stops_updated_at
    BEFORE UPDATE ON tour_stops
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Create function for soft delete
CREATE OR REPLACE FUNCTION soft_delete()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.is_deleted = true;
    NEW.deleted_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create soft delete triggers for each table
CREATE TRIGGER museums_soft_delete_trigger
    BEFORE UPDATE OF is_deleted ON museums
    FOR EACH ROW
    WHEN (OLD.is_deleted = false AND NEW.is_deleted = true)
EXECUTE FUNCTION soft_delete();

CREATE TRIGGER artists_soft_delete_trigger
    BEFORE UPDATE OF is_deleted ON artists
    FOR EACH ROW
    WHEN (OLD.is_deleted = false AND NEW.is_deleted = true)
EXECUTE FUNCTION soft_delete();

CREATE TRIGGER artworks_soft_delete_trigger
    BEFORE UPDATE OF is_deleted ON artworks
    FOR EACH ROW
    WHEN (OLD.is_deleted = false AND NEW.is_deleted = true)
EXECUTE FUNCTION soft_delete();

CREATE TRIGGER tours_soft_delete_trigger
    BEFORE UPDATE OF is_deleted ON tours
    FOR EACH ROW
    WHEN (OLD.is_deleted = false AND NEW.is_deleted = true)
EXECUTE FUNCTION soft_delete();

CREATE TRIGGER tour_stops_soft_delete_trigger
    BEFORE UPDATE OF is_deleted ON tour_stops
    FOR EACH ROW
    WHEN (OLD.is_deleted = false AND NEW.is_deleted = true)
EXECUTE FUNCTION soft_delete();