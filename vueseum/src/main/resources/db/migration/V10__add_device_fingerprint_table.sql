CREATE TABLE device_fingerprints (
    token VARCHAR(255) PRIMARY KEY,
    fingerprint VARCHAR(255) NOT NULL,
    user_agent TEXT,
    screen_resolution VARCHAR(50),
    timezone VARCHAR(100),
    languages TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_device_fingerprint ON device_fingerprints(fingerprint);