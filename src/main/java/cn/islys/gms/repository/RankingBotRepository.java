package cn.islys.gms.repository;

import cn.islys.gms.entity.RankingBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RankingBotRepository extends JpaRepository<RankingBot, Long> {
    @Query("SELECT rb FROM RankingBot rb ORDER BY rb.rankPoints DESC LIMIT 30")
    List<RankingBot> findTop30ByRankPoints();

    List<RankingBot> findByRankPointsBetween(Integer minPoints, Integer maxPoints);

    // 添加缺失的方法
    long count();
}