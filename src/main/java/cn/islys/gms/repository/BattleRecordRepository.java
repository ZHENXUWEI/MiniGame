package cn.islys.gms.repository;

import cn.islys.gms.entity.BattleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BattleRecordRepository extends JpaRepository<BattleRecord, Long> {
    List<BattleRecord> findByUserIdOrderByBattleTimeDesc(Long userId);
    List<BattleRecord> findTop10ByUserIdOrderByBattleTimeDesc(Long userId);
}