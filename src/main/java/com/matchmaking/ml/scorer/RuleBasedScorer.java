package com.matchmaking.ml.scorer;

import com.matchmaking.ml.feature.MatchFeatures;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RuleBasedScorer implements MatchQualityScorer {

    private static final double ELO_BALANCE_WEIGHT = 0.4;
    private static final double TEAM_BALANCE_WEIGHT = 0.3;
    private static final double WIN_RATE_WEIGHT = 0.2;
    private static final double QUIT_RATE_WEIGHT = 0.1;

    @Override
    public double score(MatchFeatures features) {
        // Normalize Elo balance (lower std deviation is better)
        double eloBalanceScore = 1.0 - Math.min(1.0, features.getEloStdDeviation() / 300.0);

        // Team balance is already normalized 0-1
        double teamBalanceScore = features.getTeamBalanceScore();

        // Win rate balance (closer to 0.5 is better for competitive matches)
        double winRateScore = 1.0 - Math.abs(features.getAverageWinRate() - 0.5) * 2.0;

        // Quit rate penalty (lower is better)
        double quitRateScore = 1.0 - features.getAverageQuitRate();

        // Calculate weighted score
        double qualityScore =
                ELO_BALANCE_WEIGHT * eloBalanceScore +
                TEAM_BALANCE_WEIGHT * teamBalanceScore +
                WIN_RATE_WEIGHT * winRateScore +
                QUIT_RATE_WEIGHT * quitRateScore;

        log.debug("Rule-based score calculation: eloBalance={}, teamBalance={}, winRate={}, quitRate={}, finalScore={}",
                eloBalanceScore, teamBalanceScore, winRateScore, quitRateScore, qualityScore);

        return Math.max(0.0, Math.min(1.0, qualityScore));
    }

    @Override
    public String getScorerName() {
        return "RuleBasedScorer";
    }
}
