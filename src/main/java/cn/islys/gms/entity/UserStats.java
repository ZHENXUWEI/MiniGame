package cn.islys.gms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "user_stats")
public class UserStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "experience", nullable = false)
    private Integer experience = 0;

    @Column(name = "rank_points", nullable = false)
    private Integer rankPoints = 0;

    @Column(name = "performance", nullable = false)
    private Integer performance = 50;

    @Column(name = "wins", nullable = false)
    private Integer wins = 0;

    @Column(name = "losses", nullable = false)
    private Integer losses = 0;

    @Column(name = "total_kills", nullable = false)
    private Integer totalKills = 0;

    @Column(name = "total_deaths", nullable = false)
    private Integer totalDeaths = 0;

    @Column(name = "updated_time")
    private java.time.LocalDateTime updatedTime;

    @PreUpdate
    @PrePersist
    public void updateTime() {
        this.updatedTime = java.time.LocalDateTime.now();
    }

    // 获取段位等级
    public String getRankTier() {
        if (rankPoints >= 15000) return "传说";
        if (rankPoints >= 10000) return "神话";
        if (rankPoints >= 8000) return "大师";
        if (rankPoints >= 6500) return "钻石";
        if (rankPoints >= 5000) return "铂金";
        if (rankPoints >= 3500) return "黄金";
        if (rankPoints >= 2000) return "白银";
        return "青铜";
    }

    // 获取小段位
    public Integer getRankSubTier() {
        if (rankPoints >= 15000) return null; // 传说没有小段位
        if (rankPoints >= 10000) return 1;    // 神话只有1段
        if (rankPoints >= 8000) return 1;     // 大师只有1段

        int basePoints;
        if (rankPoints >= 6500) basePoints = 6500;
        else if (rankPoints >= 5000) basePoints = 5000;
        else if (rankPoints >= 3500) basePoints = 3500;
        else if (rankPoints >= 2000) basePoints = 2000;
        else basePoints = 0;

        int pointsInTier = rankPoints - basePoints;
        int pointsPerSubTier = 500; // 每个小段位500分
        return Math.min((pointsInTier / pointsPerSubTier) + 1, 3); // 最多3个小段位
    }

    // 获取表现等级
    public String getPerformanceGrade() {
        if (performance >= 400) return "S";
        if (performance >= 300) return "A";
        if (performance >= 200) return "B";
        if (performance >= 100) return "C";
        return "D";
    }

    // 检查升级
    public boolean checkLevelUp() {
        int requiredExp = level * 1000;
        if (experience >= requiredExp) {
            level++;
            experience -= requiredExp;
            return true;
        }
        return false;
    }
}