package cn.islys.gms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "ranking_bots")
public class RankingBot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "rank_points", nullable = false)
    private Integer rankPoints = 0;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

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

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        updatedTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }

    // 计算属性
    public Double getWinRate() {
        int total = wins + losses;
        return total > 0 ? Math.round((wins * 100.0 / total) * 100.0) / 100.0 : 0.0;
    }

    public Double getKDRatio() {
        return totalDeaths > 0 ? Math.round((totalKills * 1.0 / totalDeaths) * 100.0) / 100.0 :
                (totalKills > 0 ? Math.round(totalKills * 100.0) / 100.0 : 0.0);
    }

    public String getPerformanceGrade() {
        if (performance >= 400) return "S";
        if (performance >= 300) return "A";
        if (performance >= 200) return "B";
        if (performance >= 100) return "C";
        return "D";
    }

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
        return (pointsInTier / 500) + 1;
    }
}