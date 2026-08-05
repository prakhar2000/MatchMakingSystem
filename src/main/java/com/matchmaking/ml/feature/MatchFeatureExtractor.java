package com.matchmaking.ml.feature;

import com.matchmaking.constants.Region;
import com.matchmaking.constants.Team;
import com.matchmaking.entity.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.DoubleSummaryStatistics;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchFeatureExtractor {

    public MatchFeatures extractFeatures(List<Player> players) {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("Players list cannot be null or empty");
        }

        // Elo statistics
        DoubleSummaryStatistics eloStats = players.stream()
                .mapToDouble(Player::getElo)
                .summaryStatistics();

        double averageElo = eloStats.getAverage();
        double eloStdDeviation = calculateStdDeviation(players, averageElo);
        double maxEloDifference = eloStats.getMax() - eloStats.getMin();

        // Player statistics
        double averageWinRate = calculateAverageWinRate(players);
        double averageLossRate = calculateAverageLossRate(players);
        double averageSessionLength = 30.0; // Placeholder - would come from player history
        double averageQuitRate = 0.05; // Placeholder - would come from player history

        // Timing features
        double timeOfDay = normalizeTimeOfDay(LocalDateTime.now());
        Region region = players.get(0).getRegion();

        // Team balance (for 2v2 or larger matches)
        double teamBalanceScore = calculateTeamBalance(players);

        return MatchFeatures.builder()
                .averageElo(averageElo)
                .eloStdDeviation(eloStdDeviation)
                .maxEloDifference(maxEloDifference)
                .minElo(eloStats.getMin())
                .maxElo(eloStats.getMax())
                .averageWinRate(averageWinRate)
                .averageLossRate(averageLossRate)
                .averageSessionLength(averageSessionLength)
                .averageQuitRate(averageQuitRate)
                .averageQueueWaitTime(0.0) // Would be passed in from queue data
                .timeOfDay(timeOfDay)
                .region(region)
                .playerCount(players.size())
                .teamBalanceScore(teamBalanceScore)
                .build();
    }

    private double calculateStdDeviation(List<Player> players, double mean) {
        double variance = players.stream()
                .mapToDouble(player -> Math.pow(player.getElo() - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private double calculateAverageWinRate(List<Player> players) {
        return players.stream()
                .mapToDouble(player -> {
                    int totalGames = player.getWins() + player.getLosses();
                    return totalGames > 0 ? (double) player.getWins() / totalGames : 0.5;
                })
                .average()
                .orElse(0.5);
    }

    private double calculateAverageLossRate(List<Player> players) {
        return players.stream()
                .mapToDouble(player -> {
                    int totalGames = player.getWins() + player.getLosses();
                    return totalGames > 0 ? (double) player.getLosses() / totalGames : 0.5;
                })
                .average()
                .orElse(0.5);
    }

    private double normalizeTimeOfDay(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        int minutesFromMidnight = time.getHour() * 60 + time.getMinute();
        return (double) minutesFromMidnight / (24 * 60); // Normalize to 0-1
    }

    private double calculateTeamBalance(List<Player> players) {
        // For 1v1 matches, balance is perfect
        if (players.size() == 2) {
            return 1.0;
        }

        // For larger matches, calculate team balance
        // This is a simplified version - real implementation would use actual team assignments
        int half = players.size() / 2;
        List<Player> teamA = players.subList(0, half);
        List<Player> teamB = players.subList(half, players.size());

        double teamAAvgElo = teamA.stream().mapToInt(Player::getElo).average().orElse(0);
        double teamBAvgElo = teamB.stream().mapToInt(Player::getElo).average().orElse(0);

        double eloDifference = Math.abs(teamAAvgElo - teamBAvgElo);
        double balanceScore = 1.0 - (eloDifference / 200.0); // Normalize, assuming 200 Elo diff is worst
        return Math.max(0.0, Math.min(1.0, balanceScore));
    }
}
