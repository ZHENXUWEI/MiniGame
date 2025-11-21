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

    /**
     * 判断是否为护甲
     */
    public boolean isArmor() {
        return "护甲".equals(this.type);
    }

    /**
     * 判断是否为远程武器
     */
    public boolean isRangedWeapon() {
        return !"子弹".equals(this.type) && !"护甲".equals(this.type) && !"近战武器".equals(this.type);
    }

    /**
     * 判断是否为近战武器
     */
    public boolean isMeleeWeapon() {
        return "近战武器".equals(this.type);
    }

    /**
     * 获取护甲防御值（从名称中解析或使用伤害字段）
     */
    public Integer getArmorDefense() {
        if (!isArmor()) return 0;

        // 如果伤害字段存储的是防御值，直接使用
        if (this.damage != null && this.damage > 0) {
            return this.damage.intValue();
        }

        // 否则从名称中解析防御值
        if (this.name.contains("防刺背心")) return 20;
        if (this.name.contains("防弹衣")) return 35;
        if (this.name.contains("重型")) return 50;
        if (this.name.contains("轻型")) return 15;

        return 10; // 默认防御值
    }
}
