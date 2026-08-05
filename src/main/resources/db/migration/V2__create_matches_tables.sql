CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    average_elo INTEGER,
    region VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE match_players (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    player_elo INTEGER,
    team VARCHAR(20) NOT NULL,
    CONSTRAINT fk_match_players_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE INDEX idx_matches_region ON matches(region);
CREATE INDEX idx_matches_status ON matches(status);
CREATE INDEX idx_match_players_match_id ON match_players(match_id);
CREATE INDEX idx_match_players_player_id ON match_players(player_id);
