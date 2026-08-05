# ML-Based Match Quality Scoring

## Overview

The matchmaking system uses a hybrid approach: deterministic algorithms generate valid candidate matches, and ML models rank them by predicted quality. This ensures fast, explainable matching while improving match quality over time.

## Architecture

```
Players Join Queue
        │
        ▼
Generate Candidate Matches (Deterministic)
        │
        ▼
Feature Extraction (Mathematical)
        │
        ▼
Match Quality Scorer (Rule-Based → ML)
        │
        ▼
Choose Best Match
        │
        ▼
Create Match
```

## Key Design Principles

1. **ELO First**: ML doesn't replace ELO constraints—it ranks valid ELO-compliant candidates
2. **Deterministic Base**: System works with rule-based scoring before ML is deployed
3. **Data-Driven Evolution**: ML models trained on actual match data
4. **Explainable**: Feature extraction is transparent and mathematical
5. **Fallback**: Rule-based scorer always available as backup

## Package Structure

```
com.matchmaking.ml
├── feature/
│   ├── MatchFeatures.java          # Feature vector representation
│   └── MatchFeatureExtractor.java  # Extracts features from player lists
├── model/
│   └── MatchCandidate.java          # Represents a potential match
├── scorer/
│   ├── MatchQualityScorer.java     # Scorer interface
│   ├── RuleBasedScorer.java        # Rule-based implementation
│   └── MLMatchQualityScorer.java   # ML-based implementation (future)
└── training/                       # Python training scripts (future)
```

## Feature Engineering

### Current Features

#### Elo-Related Features
- `averageElo`: Mean ELO of all players in match
- `eloStdDeviation`: Standard deviation of ELOs
- `maxEloDifference`: Max ELO - Min ELO
- `minElo`: Minimum ELO in match
- `maxElo`: Maximum ELO in match

#### Player Statistics
- `averageWinRate`: Mean win rate across players
- `averageLossRate`: Mean loss rate across players
- `averageSessionLength`: Mean session duration (placeholder)
- `averageQuitRate`: Mean quit rate (placeholder)

#### Queue and Timing
- `averageQueueWaitTime`: Mean time players spent in queue
- `timeOfDay`: Normalized time of day (0-1)
- `region`: Geographic region

#### Match Characteristics
- `playerCount`: Number of players in match
- `teamBalanceScore`: Balance of team average ELOs

### Feature Normalization

All features are normalized to 0-1 range for consistent ML model input:

```java
// Elo balance: lower std deviation is better
eloBalanceScore = 1.0 - (eloStdDeviation / 300.0)

// Time of day: normalized to 0-1
timeOfDay = minutesFromMidnight / (24 * 60)

// Team balance: already normalized
teamBalanceScore = 1.0 - (eloDifference / 200.0)
```

## Scoring Approaches

### Rule-Based Scorer

Current implementation using weighted formula:

```java
qualityScore =
    0.4 * eloBalanceScore +
    0.3 * teamBalanceScore +
    0.2 * winRateScore +
    0.1 * quitRateScore
```

**Weights:**
- Elo Balance: 40% (most important)
- Team Balance: 30%
- Win Rate Balance: 20%
- Quit Rate: 10%

### ML-Based Scorer (Future)

Planned implementation using Python-trained models:

**Model Options:**
- Random Forest
- XGBoost
- Neural Networks

**Export Format:**
- ONNX (cross-platform)
- PMML (legacy support)

**Integration:**
- Python: scikit-learn for training
- Java: ONNX Runtime for inference

## Candidate Generation

### Sliding Window Approach

Instead of single match, generate multiple candidates:

```
Queue ELOs: [1450, 1460, 1475, 1480, 1490, 1500, 1510, 1520]

Candidates:
- Candidate 1: [1450, 1460, 1475, 1480]
- Candidate 2: [1460, 1475, 1480, 1490]
- Candidate 3: [1475, 1480, 1490, 1500]
- Candidate 4: [1480, 1490, 1500, 1510]
```

Each candidate is scored, and the highest-scoring match is created.

## Data Collection Pipeline

### Training Data Generation

Every completed match produces a training record:

```java
MatchFeatures features = featureExtractor.extractFeatures(matchPlayers);
double qualityScore = calculateSyntheticQualityScore(match);

// Store for training
trainingDataRepository.save(features, qualityScore);
```

### Synthetic Quality Score

Before ML exists, use rule-based scoring:

```java
qualityScore =
    0.4 * eloBalance +
    0.3 * matchDurationScore +
    0.3 * quitPenalty
```

Where:
- `eloBalance = 1 - (eloStdDeviation / 300)`
- `quitPenalty = 1.0 (no quits), 0.5 (one quit), 0.0 (many quits)`

## Training Pipeline (Future)

### Phase 1: Data Collection
- Log all completed matches
- Extract features
- Calculate synthetic quality scores
- Store in training database

### Phase 2: Model Training
```python
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error

# Load training data
df = pd.read_csv('match_training_data.csv')

# Split features and target
X = df[feature_columns]
y = df['quality_score']

# Train model
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)
model = RandomForestRegressor(n_estimators=100, max_depth=10)
model.fit(X_train, y_train)

# Evaluate
y_pred = model.predict(X_test)
mse = mean_squared_error(y_test, y_pred)

# Export to ONNX
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

initial_type = [('float_input', FloatTensorType([None, X.shape[1]]))]
onnx_model = convert_sklearn(model, initial_types=initial_type)

with open('match_quality_model.onnx', 'wb') as f:
    f.write(onnx_model.SerializeToString())
```

### Phase 3: Production Integration
```java
// Load ONNX model
OrtEnvironment env = OrtEnvironment.getEnvironment();
OrtSession.SessionOptions options = new OrtSession.SessionOptions();
OrtSession session = env.createSession("match_quality_model.onnx", options);

// Run inference
OnnxTensor inputTensor = OnnxTensor.createTensor(env, featureArray);
OrtSession.Result results = session.run(Collections.singletonList("float_input"),
    Collections.singletonMap("output", inputTensor));

double qualityScore = ((float[][]) results.get(0).getValue())[0][0];
```

## Performance Considerations

### Feature Extraction Cost
- Current: ~1-2ms per candidate
- Optimized: Can be cached for repeated candidates

### Scoring Cost
- Rule-based: <1ms per candidate
- ML-based: ~5-10ms per candidate (ONNX)

### Candidate Generation
- Sliding window: O(n) where n = queue size
- Optimized: Limit to top 50 players by ELO

## Monitoring and Metrics

### Key Metrics
- Average match quality score
- Feature distribution over time
- Scorer performance (rule vs ML)
- Candidate generation rate

### Alerts
- Quality score drops below threshold
- Feature extraction errors
- Model drift detection

## Configuration

### Application Properties

```yaml
matchmaking:
  ml:
    enabled: false  # Disable ML by default
    scorer-type: RULE_BASED  # RULE_BASED or ML
    candidate-generation:
      enabled: true
      max-candidates: 10
      min-players-per-candidate: 2
    feature-extraction:
      cache-enabled: true
      cache-ttl-seconds: 300
```

## Testing

### Unit Tests
- Test feature extraction with known inputs
- Test rule-based scoring edge cases
- Test candidate generation logic

### Integration Tests
- Test end-to-end candidate generation
- Test scorer selection
- Test fallback to rule-based scorer

### Performance Tests
- Benchmark feature extraction
- Benchmark scoring throughput
- Test with large queue sizes

## Future Enhancements

### Advanced Features
- Player compatibility scores
- Historical match outcomes
- Player feedback integration
- Time-based skill decay

### Model Improvements
- Ensemble methods
- Online learning
- Transfer learning from other games
- Reinforcement learning for optimization

### Explainability
- SHAP values for model interpretation
- Feature importance tracking
- Decision visualization

## References

- scikit-learn: https://scikit-learn.org/
- ONNX Runtime: https://onnxruntime.ai/
- Matchmaking Research: https://www.gamasutra.com/blogs/JoshuaMenke/20171004/30311/Improving_Rate_of_Convergence_in_Matchmaking.php
