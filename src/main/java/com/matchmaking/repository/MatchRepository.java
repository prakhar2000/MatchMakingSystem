package com.matchmaking.repository;

import com.matchmaking.entity.Match;
import com.matchmaking.constants.Region;
import com.matchmaking.constants.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByRegionAndStatus(Region region, MatchStatus status);

    Optional<Match> findByIdAndStatus(Long id, MatchStatus status);
}
