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
        if (rankPoints >= 10000) return "LEGEND";
        if (rankPoints >= 8000) return "MYTHIC";
        if (rankPoints >= 6500) return "MASTER";
        if (rankPoints >= 5000) return "DIAMOND";
        if (rankPoints >= 3500) return "PLATINUM";
        if (rankPoints >= 2000) return "GOLD";
        if (rankPoints >= 1500) return "SILVER";
        return "BRONZE";
    }

    // 获取小段位
    public Integer getRankSubTier() {
        if (rankPoints >= 10000) return null; // 传说没有小段位

        int basePoints;
        if (rankPoints >= 8000) basePoints = 8000;
        else if (rankPoints >= 6500) basePoints = 6500;
        else if (rankPoints >= 5000) basePoints = 5000;
        else if (rankPoints >= 3500) basePoints = 3500;
        else if (rankPoints >= 2000) basePoints = 2000;
        else if (rankPoints >= 1500) basePoints = 1500;
        else basePoints = 0;

        int pointsInTier = rankPoints - basePoints;
        return (pointsInTier / 500) + 1;
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