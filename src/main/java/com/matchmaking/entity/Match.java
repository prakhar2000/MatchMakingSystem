package com.matchmaking.entity;

import com.matchmaking.constants.MatchStatus;
import com.matchmaking.constants.Region;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "matches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column
    private Integer averageElo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.PENDING;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<MatchPlayer> matchPlayers = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
