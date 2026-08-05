CREATE TABLE players (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    elo INTEGER DEFAULT 1000,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    region VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_players_region ON players(region);
CREATE INDEX idx_players_status ON players(status);
CREATE INDEX idx_players_elo ON players(elo);
