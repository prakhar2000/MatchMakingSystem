package com.matchmaking.ml.feature;

import com.matchmaking.constants.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchFeatures {

    // Elo-related features
    private double averageElo;
    private double eloStdDeviation;
    private double maxEloDifference;
    private double minElo;
    private double maxElo;

    // Player statistics
    private double averageWinRate;
    private double averageLossRate;
    private double averageSessionLength;
    private double averageQuitRate;

    // Queue and timing
    private double averageQueueWaitTime;
    private double timeOfDay; // Normalized 0-1 (0 = midnight, 1 = 23:59)
    private Region region;

    // Match characteristics
    private int playerCount;
    private double teamBalanceScore;
}
