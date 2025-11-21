package cn.islys.gms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "items")
public class Item {
    // Getter和Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "名称", nullable = false)
    private String name;

    @Column(name = "伤害", nullable = false)
    private Float damage;

    @Column(name = "价值", nullable = false)
    private Integer value;

    @Column(name = "耐久")
    private String durability;

    @Column(name = "类型", nullable = false)
    private String type;

    @Column(name = "使用子弹")
    private String usedBullet;

    public Item() {

    }

    public Item(String name, Float damage, Integer value, String durability, String type, String usedBullet) {
        this.name = name;
        this.damage = damage;
        this.value = value;
        this.durability = durability;
        this.type = type;
        this.usedBullet = usedBullet;
    }
}
