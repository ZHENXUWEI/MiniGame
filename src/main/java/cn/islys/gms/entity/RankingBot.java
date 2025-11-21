package cn.islys.gms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "ranking_bots")
public class RankingBot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "rank_points", nullable = false)
    private Integer rankPoints;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "performance", nullable = false)
    private Integer performance;

    @Column(name = "wins", nullable = false)
    private Integer wins;

    @Column(name = "losses", nullable = false)
    private Integer losses;

    @Column(name = "updated_time")
    private java.time.LocalDateTime updatedTime;
}