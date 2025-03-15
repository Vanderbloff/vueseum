-- Create standardized_terms table
CREATE TABLE standardized_terms (
    -- Base entity fields inherited from BaseEntity
    id BIGINT PRIMARY KEY DEFAULT nextval('id_sequence'),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,

    -- Standardized term-specific fields
    raw_term VARCHAR(1000) NOT NULL,
    standardized_term VARCHAR(1000) NOT NULL,
    category VARCHAR(50) NOT NULL,
    last_accessed TIMESTAMP,
    access_count INTEGER DEFAULT 1,

    -- Add constraints
    CONSTRAINT unique_raw_term_category UNIQUE (raw_term, category)
);

-- Create indices for optimized queries
CREATE INDEX idx_standardized_raw_term ON standardized_terms(raw_term);
CREATE INDEX idx_standardized_term ON standardized_terms(standardized_term);
CREATE INDEX idx_standardized_category ON standardized_terms(category);
CREATE INDEX idx_standardized_deleted ON standardized_terms(is_deleted);

-- Create trigger for updated_at
CREATE TRIGGER update_standardized_terms_updated_at
    BEFORE UPDATE ON standardized_terms
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Create soft delete trigger
CREATE TRIGGER standardized_terms_soft_delete_trigger
    BEFORE UPDATE OF is_deleted ON standardized_terms
    FOR EACH ROW
    WHEN (OLD.is_deleted = false AND NEW.is_deleted = true)
EXECUTE FUNCTION soft_delete();