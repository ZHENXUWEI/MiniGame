package cn.islys.gms.repository;

import cn.islys.gms.entity.RankingBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RankingBotRepository extends JpaRepository<RankingBot, Long> {
    @Query("SELECT rb FROM RankingBot rb ORDER BY rb.rankPoints DESC LIMIT :limit")
    List<RankingBot> findTopRankingBots(@Param("limit") int limit);

    @Query("SELECT rb FROM RankingBot rb WHERE rb.rankPoints BETWEEN :minPoints AND :maxPoints")
    List<RankingBot> findByRankPointsBetween(@Param("minPoints") Integer minPoints, @Param("maxPoints") Integer maxPoints);

    @Query("SELECT COUNT(rb) FROM RankingBot rb WHERE rb.rankPoints >= :minPoints AND rb.rankPoints <= :maxPoints")
    long countByRankPointsBetween(@Param("minPoints") Integer minPoints, @Param("maxPoints") Integer maxPoints);

    long count();

    boolean existsByName(String name);

    // 如果需要，添加这个方法
    List<RankingBot> findAll();
}