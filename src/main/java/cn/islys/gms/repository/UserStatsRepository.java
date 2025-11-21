package cn.islys.gms.repository;

import cn.islys.gms.entity.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
    Optional<UserStats> findByUserId(Long userId);

    @Query("SELECT us FROM UserStats us WHERE us.userId IN (SELECT u.id FROM User u WHERE u.status = 1) ORDER BY us.rankPoints DESC LIMIT 30")
    List<UserStats> findTop30ByRankPoints();

    long countByRankPointsGreaterThan(Integer rankPoints);
    long count();
}