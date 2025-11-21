package cn.islys.gms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "battle_records")
public class BattleRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "mode", nullable = false)
    private String mode; // QUICK, RANKED, TOURNAMENT

    @Column(name = "result", nullable = false)
    private String result; // WIN, LOSE

    @Column(name = "kills", nullable = false)
    private Integer kills;

    @Column(name = "experience_gained", nullable = false)
    private Integer experienceGained;

    @Column(name = "money_gained", nullable = false)
    private Integer moneyGained;

    @Column(name = "performance_gained", nullable = false)
    private Integer performanceGained;

    @Column(name = "rank_points_gained")
    private Integer rankPointsGained;

    @Column(name = "battle_time", nullable = false)
    private LocalDateTime battleTime;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "team_data", columnDefinition = "TEXT")
    private String teamData; // JSON格式存储队伍数据

    @Column(name = "enemy_data", columnDefinition = "TEXT")
    private String enemyData; // JSON格式存储敌人数据

    public BattleRecord() {
        this.battleTime = LocalDateTime.now();
    }
}