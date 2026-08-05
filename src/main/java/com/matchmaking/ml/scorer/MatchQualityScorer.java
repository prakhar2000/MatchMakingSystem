package com.matchmaking.ml.scorer;

import com.matchmaking.ml.feature.MatchFeatures;

public interface MatchQualityScorer {

    double score(MatchFeatures features);

    String getScorerName();
}
