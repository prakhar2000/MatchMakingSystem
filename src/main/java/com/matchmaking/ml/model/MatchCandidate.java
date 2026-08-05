package com.matchmaking.ml.model;

import com.matchmaking.entity.Player;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchCandidate {

    private List<Player> players;
    private double qualityScore;
    private double averageElo;
    private double maxEloDifference;
    private double eloStdDeviation;
}
