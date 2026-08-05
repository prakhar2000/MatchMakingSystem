package com.matchmaking.entity;

import com.matchmaking.constants.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_players")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column
    private Integer playerElo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Team team;
}
