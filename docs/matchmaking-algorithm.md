# Matchmaking Algorithm

## Overview

The matchmaking algorithm is the core component that pairs players based on their skill level (Elo rating), region, and other factors to create balanced and enjoyable matches.

## Current Implementation (Sprint 2)

### Basic Elo-Based Matching

The initial implementation uses a simple Elo-based matching algorithm:

1. **Queue Storage**: Players are stored in Redis sorted sets, keyed by their Elo rating
2. **Region Separation**: Each region maintains its own queue
3. **Matching Logic**: Find players with similar Elo ratings within a threshold

#### Algorithm Steps

```
1. Every second, the matchmaking worker runs
2. For each region:
   a. Retrieve players from the queue (sorted by Elo)
   b. Group players with Elo difference < threshold (e.g., 100)
   c. Create matches when enough players are found
   d. Remove matched players from queue
   e. Persist match to PostgreSQL
   f. Notify players via WebSocket
```

#### Redis Data Structure

```
queue:{region}:{gameMode}
Sorted Set by Elo

Example: queue:US_EAST:RANKED
Score (Elo) → Player ID
1500 → player_123
1510 → player_456
1520 → player_789
1540 → player_321
```

#### Match Creation Rules

- Minimum players per match: 2 (configurable)
- Maximum Elo difference: 100 (configurable)
- Same region only
- Same game mode only

---

## Future Enhancements (Sprint 3+)

### 1. Dynamic Elo Threshold

Instead of a fixed threshold, adjust based on queue size:

```java
if (queueSize < 10) {
    eloThreshold = 50;   // Stricter matching when few players
} else if (queueSize < 50) {
    eloThreshold = 100;  // Standard matching
} else {
    eloThreshold = 200;  // Relaxed matching to reduce wait times
}
```

### 2. Wait Time Priority

Give priority to players who have been waiting longer:

```java
effectiveElo = player.elo - (waitTimeSeconds / 10);
```

This gradually lowers a player's effective Elo, making them matchable with a wider range of opponents over time.

### 3. Team Balancing

For team-based games, ensure teams have similar average Elo:

```
Team A Average Elo: 1450
Team B Average Elo: 1460
Difference: 10 (acceptable)
```

### 4. Player Preferences

Allow players to specify preferences:
- Preferred game modes
- Maximum wait time
- Preferred map types
- Avoid specific players (block list)

### 5. AI-Powered Match Quality

Use machine learning to predict match quality:

**Features:**
- Elo difference
- Historical win rates between players
- Player retention after matches
- Reported match satisfaction

**Model Output:**
- Predicted match quality score (0-100)
- Expected match duration
- Likelihood of player enjoyment

**Training Data:**
- Historical match results
- Player feedback
- Post-match surveys

### 6. Fairness Metrics

Track and optimize for:
- **Match Balance**: How close are team skill levels?
- **Wait Time**: Average time in queue
- **Player Retention**: Do players return after matches?
- **Skill Progression**: Are players improving at appropriate rates?

---

## Match Quality Scoring

### Current Score (Simple)

```
Quality Score = 100 - (Elo Difference / 2)
```

Example:
- Elo difference = 0 → Score = 100
- Elo difference = 50 → Score = 75
- Elo difference = 100 → Score = 50
- Elo difference = 200 → Score = 0

### Enhanced Score (Planned)

```
Quality Score = 
  (Elo Balance Weight × Elo Score) +
  (Wait Time Weight × Wait Score) +
  (Region Weight × Region Score) +
  (History Weight × History Score)
```

Where each component is normalized to 0-100.

---

## Performance Considerations

### Current Performance
- Matching interval: 1 second
- Expected queue size: Hundreds to thousands
- Redis operations: O(log N) for sorted sets

### Optimization Strategies

1. **Batch Processing**: Process multiple regions in parallel
2. **Caching**: Cache player data to reduce database queries
3. **Pre-computation**: Pre-compute match candidates
4. **Load Shedding**: Skip matching if system is overloaded

---

## Monitoring and Metrics

### Key Metrics to Track

- Average queue wait time
- Average Elo difference in matches
- Match creation rate
- Player satisfaction scores
- Queue size by region
- Failed match attempts

### Alerting

- Alert if average wait time > threshold
- Alert if match quality score drops below threshold
- Alert if queue size exceeds capacity

---

## Configuration

### Application Properties

```yaml
matchmaking:
  enabled: true
  interval-seconds: 1
  elo-threshold: 100
  min-players-per-match: 2
  max-players-per-match: 10
  max-wait-time-seconds: 300
  priority-decay-rate: 0.1
```

### Region-Specific Config

```yaml
matchmaking:
  regions:
    US_EAST:
      elo-threshold: 100
      min-players: 2
    ASIA:
      elo-threshold: 150
      min-players: 4
```

---

## Testing

### Unit Tests
- Test matching logic with various Elo distributions
- Test edge cases (empty queue, single player)
- Test threshold calculations

### Integration Tests
- Test end-to-end flow with Redis and PostgreSQL
- Test concurrent queue operations
- Test WebSocket notifications

### Load Tests
- Simulate high queue volumes
- Test performance under stress
- Validate scalability limits

---

## References

- Elo Rating System: https://en.wikipedia.org/wiki/Elo_rating_system
- Matchmaking in Games: https://www.gamasutra.com/blogs/JoshuaMenke/20171004/30311/Improving_Rate_of_Convergence_in_Matchmaking.php
- TrueSkill: Microsoft's Bayesian ranking system
